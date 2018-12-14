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
import com.sun.spot.util.Utils;

public class NodeMain extends MIDlet implements IADT7411ThresholdListener {

    private final int TEMP_ALARM_THRESHOLD = 25;
    private final int ACC_ALARM_THRESHOLD = 2;
    private final long HEARTBEAT_FREQ = 10000;
    private final long ALARM_FREQ = 5000;

    private ILightSensor light = (ILightSensor) Resources.lookup(ILightSensor.class);
    private ITemperatureInput temp = (ITemperatureInput) Resources.lookup(ITemperatureInput.class);
    private IAccelerometer3D accel = (IAccelerometer3D) Resources.lookup(IAccelerometer3D.class);
    private RadiogramConnection tx = null;
    private Datagram dg;
    private final String MY_MAC = SpotCommons.getMyMAC(Spot.getInstance());

    private long lastSentHeartbeat = 0;
    private long lastSentTempAlarm = 0;
    private long lastSentAccAlarm = 0;

    int tempValue = 0;
    int accelXValue = 0;
    int accelYValue = 0;
    int accelZValue = 0;

    protected void startApp() throws MIDletStateChangeException {
        try {
            while (true) {
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
            String frame = "ALARMACCEL," + MY_MAC;
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
            dg = (Datagram) tx.newDatagram(tx.getMaximumLength());

            dg.reset();
            dg.writeUTF(frame);
            tx.send(dg);
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
}
