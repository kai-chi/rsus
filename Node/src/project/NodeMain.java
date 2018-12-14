package project;

import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.ILightSensor;
import com.sun.spot.resources.transducers.IAccelerometer3D;
import com.sun.spot.resources.transducers.ITemperatureInput;

import com.sun.spot.sensorboard.peripheral.ADT7411Event;
import com.sun.spot.sensorboard.peripheral.IADT7411ThresholdListener;

import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.LEDColor;
import com.sun.spot.util.Utils;

public class NodeMain extends MIDlet implements IADT7411ThresholdListener {

    private final int TEMP_ALARM_THRESHOLD = 25;
    private final int ACC_ALARM_THRESHOLD = 2;
    private final long HEARTBEAT_FREQ = 10000;
    private final long ALARM_FREQ = 5000;
    private final String MY_MAC = SpotCommons.getMyMAC(Spot.getInstance());
    private final int RX_PORT = 46;

    private ILightSensor light = (ILightSensor) Resources.lookup(ILightSensor.class);
    private ITemperatureInput temp = (ITemperatureInput) Resources.lookup(ITemperatureInput.class);
    private IAccelerometer3D accel = (IAccelerometer3D) Resources.lookup(IAccelerometer3D.class);
    private ITriColorLEDArray leds = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);

    private RadiogramConnection tx = null;
    private Datagram txDatagram;
    private RadiogramConnection rxConn = null;
    private Datagram rxDatagram;

    private long lastSentHeartbeat = 0;
    private long lastSentTempAlarm = 0;
    private long lastSentAccAlarm = 0;

    private int tempValue = 0;
    private int accelXValue = 0;
    private int accelYValue = 0;
    private int accelZValue = 0;

    protected void startApp() throws MIDletStateChangeException {
        try {
            openRxConnection();
            while (true) {
                checkForRxData();
                readSensorValues();
                checkAccAlarm();
                checkTempAlarm();
                sendHeartbeat();
                
                Utils.sleep(200);
            }
        } catch (Exception e) {
            System.out.println("Connection error: " + e);
        }
    }
    
    private void checkForRxData() throws IOException {
        if (rxConn.packetsAvailable()) {
            rxConn.receive(rxDatagram);
            String rxData = rxDatagram.readUTF();
            System.out.println("Received data: " + rxData);
            parseRxData(rxData);
        }
    }
    
    private void parseRxData(String data) {
        String[] fields = parseCSV(data);
        if (fields.length == 0) {
            return;
        }
        if (fields[0].equals("BLINK")) {
            blinkLEDs(Integer.parseInt(fields[1]));
        }
    }
    
    private String[] parseCSV(String line) {
        int elements = 0;
        String[] fields = new String[10];
        String s = line;
        while (s.length() > 0) {
            int nextCommaIndex = s.indexOf(",");
            if (nextCommaIndex == -1) {
                fields[elements] = s;
                break;
            }
            String field = s.substring(0, nextCommaIndex);
            fields[elements++] = field;
            s = s.substring(nextCommaIndex + 1);
        }
        return fields;
    }

    private void openRxConnection() throws IOException {
        rxConn = (RadiogramConnection) Connector.open("radiogram://:" + RX_PORT);
        rxDatagram = rxConn.newDatagram(rxConn.getMaximumLength());
    }

    private void sendHeartbeat() throws IOException {
        if (lastSentHeartbeat < System.currentTimeMillis() - HEARTBEAT_FREQ) {
            StringBuffer sb = new StringBuffer("HEARTBEAT,");
            String frame = sb.append(MY_MAC)
                    .append(",")
                    .append(light.getValue())
                    .append(",")
                    .append(tempValue)
                    .append(",")
                    .append(accelXValue)
                    .append(",")
                    .append(accelYValue)
                    .append(",")
                    .append(accelZValue)
                    .toString();
            sendFrameToAggNode(frame);
            lastSentHeartbeat = System.currentTimeMillis();
        }
    }
    
    private void checkAccAlarm() throws IOException {
        if (lastSentAccAlarm < System.currentTimeMillis() - ALARM_FREQ
                && ((accelXValue >= ACC_ALARM_THRESHOLD)
                || (accelYValue >= ACC_ALARM_THRESHOLD)
                || (accelZValue >= ACC_ALARM_THRESHOLD))) {
            String frame = "ALARMACC," + MY_MAC;
            sendFrameToAggNode(frame);
            lastSentAccAlarm = System.currentTimeMillis();
        }
    }
    
    private void checkTempAlarm() throws IOException {
        if (lastSentTempAlarm < System.currentTimeMillis() - ALARM_FREQ
                && (tempValue >= TEMP_ALARM_THRESHOLD)) {
            String frame = "ALARMTEMP," + MY_MAC;
            sendFrameToAggNode(frame);
            lastSentTempAlarm = System.currentTimeMillis();
        }
    }

    private void readSensorValues() throws IOException{
        tempValue = (int) temp.getCelsius();
        accelXValue = (int) accel.getAccelX();
        accelYValue = (int) accel.getAccelY();
        accelZValue = (int) accel.getAccelZ();
    }

    private void sendFrameToAggNode(String frame) throws IOException {
        try {
            tx = (RadiogramConnection) Connector.open("radiogram://7f00.0101.0000.1002:112");
            txDatagram = (Datagram) tx.newDatagram(tx.getMaximumLength());

            txDatagram.reset();
            txDatagram.writeUTF(frame);
            tx.send(txDatagram);
            System.out.println("Sent to AggNode: " + frame);
        } catch (IOException e) {
            System.out.println("Error sending frame to AggNode");
        } finally {
            tx.close();
        }

    }

    protected void pauseApp() {
        // This will never be called by the Squawk VM
    }

    protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
        // Only called if startApp throws any exception other than MIDletStateChangeException
    }

    public void thresholdChanged(ADT7411Event evt) {
    }

    public void thresholdExceeded(ADT7411Event evt) {
    }

    private void blinkLEDs(int color) {
        for (int i = 0; i < 3; i++) {
            switch (color) {
                case 0: // registration successful
                    leds.setColor(LEDColor.GREEN);
                    break;
                case 1: // alarm
                    leds.setColor(LEDColor.RED);
                    break;
                case 2: // ping
                    leds.setColor(LEDColor.WHITE);
                default:
                    return;
            }
            leds.setOn();
            Utils.sleep(300);
            leds.setOff();
            Utils.sleep(200);
        }
    }
}
