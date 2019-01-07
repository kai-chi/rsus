package project;

import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.Condition;
import com.sun.spot.resources.transducers.IAccelerometer3D;
import com.sun.spot.resources.transducers.IConditionListener;
import com.sun.spot.resources.transducers.ILightSensor;
import com.sun.spot.resources.transducers.ITemperatureInput;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.LEDColor;
import com.sun.spot.resources.transducers.SensorEvent;
import com.sun.spot.util.Utils;
import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * SimpleNode abstract class represents basic functionality of every node.
 * @author userrsus
 */
public abstract class SimpleNode extends MIDlet {

    /**
     * MAC address of the device
     */
    public final String MY_MAC = SpotCommons.getMyMAC(Spot.getInstance());
    private final double TEMP_ALARM_THRESHOLD = 25.0;
    private final int ACC_ALARM_THRESHOLD = 2;
    /**
     * How often alarm can be reported in ms
     */
    public final long ALARM_FREQ = 5000;
    /**
     * How often heartbeat can be reported in ms
     */
    public final int HEARTBEAT_FREQ = 10000;
    private final int CHECK_ALARM_FREQ = 1000;
    private ITemperatureInput temp = (ITemperatureInput) Resources.lookup(ITemperatureInput.class);
    private IAccelerometer3D acc = (IAccelerometer3D) Resources.lookup(IAccelerometer3D.class);
    private ILightSensor light = (ILightSensor) Resources.lookup(ILightSensor.class);
    private ITriColorLEDArray leds = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    /**
     * Timestamp of the last sent heartbeat
     */
    public long lastSentHeartbeat = 0;
    /**
     * Timestamp of the last sent fire alarm
     */
    public long lastSentTempAlarm = 0;
    /**
     * Timestamp of the last sent vandalism alarm
     */
    public long lastSentAccAlarm = 0;
    /**
     * Current temperature value
     */
    public int tempValue = 0;
    /**
     * Current accelerometer X-axis value
     */
    public int accXValue = 0;
    /**
     * Current accelerometer Y-axis value
     */
    public int accYValue = 0;
    /**
     * Current accelerometer Z-axis value
     */
    public int accZValue = 0;
    /**
     * Current light sensor value
     */
    public int lightValue = 0;
    /**
     * RadiogramConnection responsible for tx communication
     */
    public RadiogramConnection tx = null;
    /**
     * Datagram for tx communication
     */
    public Datagram txDatagram;
    /**
     * RadiogramConnection responsible for rx communication
     */
    public RadiogramConnection rxConn = null;
    /**
     * Datagram for rx communication
     */
    public Datagram rxDatagram;

    /**
     * Sends heartbeat message to the node higher in the hierarchy -
     * Node sends to AggNode, AggNode sends to the server
     * @throws IOException
     */
    public abstract void sendHeartbeat() throws IOException;

    /**
     * Sends frame towards the server
     * @param frame String to be send to the server
     * @throws IOException
     */
    public abstract void sendFrameToServer(String frame) throws IOException;
    //temp callback
    private IConditionListener tempListener = new IConditionListener() {

        public void conditionMet(SensorEvent evt, Condition condition) {
            String frame = "ALARMTEMP," + MY_MAC + "," + MY_MAC;
            try {
                sendFrameToServer(frame);
            } catch (IOException e) {
                System.out.println("Error sending temp alarm");
            }
            lastSentTempAlarm = System.currentTimeMillis();
        }
    };
    private Condition fireCondition = new Condition(temp, tempListener, CHECK_ALARM_FREQ) {

        public boolean isMet(SensorEvent evt) throws IOException {
            if (((ITemperatureInput) sensor).getCelsius() >= TEMP_ALARM_THRESHOLD
                    && (lastSentTempAlarm < System.currentTimeMillis() - ALARM_FREQ)) {
                return true;
            } else {
                return false;
            }
        }
    };
    //acc callback
    private IConditionListener accListener = new IConditionListener() {

        public void conditionMet(SensorEvent evt, Condition condition) {
            String frame = "ALARMACC," + MY_MAC + "," + MY_MAC;
            try {
                sendFrameToServer(frame);
            } catch (IOException e) {
                System.out.println("Error sending acc alarm");
            }
            lastSentAccAlarm = System.currentTimeMillis();
        }
    };
    private Condition vandalismCondition = new Condition(acc, accListener, CHECK_ALARM_FREQ) {

        public boolean isMet(SensorEvent evt) throws IOException {
            double[] acc = ((IAccelerometer3D) sensor).getAccelValues();
            if ((acc[0] >= ACC_ALARM_THRESHOLD
                    || acc[1] >= ACC_ALARM_THRESHOLD
                    || acc[2] >= ACC_ALARM_THRESHOLD)
                    && lastSentAccAlarm < System.currentTimeMillis() - ALARM_FREQ) {
                return true;
            } else {
                return false;
            }
        }
    };

    /**
     * Starts monitoring the alarm thresholds
     */
    public void startAlarmConditions() {
        fireCondition.start();
        vandalismCondition.start();
    }

    /**
     * Reads current sensor values and stores in the variables
     * @throws IOException
     */
    public void readSensorValues() throws IOException {
        tempValue = (int) temp.getCelsius();
        accXValue = (int) acc.getAccelX();
        accYValue = (int) acc.getAccelY();
        accZValue = (int) acc.getAccelZ();
        lightValue = light.getValue();
    }

    /**
     * N/A
     */
    protected void pauseApp() {
    }

    /**
     * N/A
     * @param unconditional
     * @throws MIDletStateChangeException
     */
    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
    }

    /**
     * Blinks with LEDS in a given color:
     * 0 - GREEN
     * 1 - RED
     * 2 - YELLOW
     * @param color integer representing color to blink with
     */
    public void blinkLEDs(int color) {
        switch (color) {
            case 0: // registration successful
                leds.setColor(LEDColor.GREEN);
                break;
            case 1: // alarm
                leds.setColor(LEDColor.RED);
                break;
            case 2: // ping
                leds.setColor(LEDColor.YELLOW);
                break;
            default:
                return;
        }
        for (int i = 0; i < 3; i++) {
            leds.setOn();
            Utils.sleep(300);
            leds.setOff();
            Utils.sleep(200);
        }
    }

    /**
     * Parses data received
     * @param data String data to parse
     */
    public abstract void parseRxData(String data);

    /**
     * Receives data from RadiogramConnection and obtains the message as a String
     * @param rxConn
     * @param rxDatagram
     * @throws IOException
     */
    public void parseIncomingData(RadiogramConnection rxConn, Datagram rxDatagram) throws IOException {
        if (rxConn.packetsAvailable()) {
            rxConn.receive(rxDatagram);
            String rxData = rxDatagram.readUTF();
            System.out.println("Received data: " + rxData);
            parseRxData(rxData);
        }
    }

    /**
     * Gets the port for rx communication
     * @return
     */
    public abstract int getRxPort();

    /**
     * Opens rx connection
     * @throws IOException
     */
    public void openRxConnection() throws IOException {
        rxConn = (RadiogramConnection) Connector.open("radiogram://:" + getRxPort());
        rxDatagram = rxConn.newDatagram(rxConn.getMaximumLength());
    }
}
