package project.aggnode;

import com.sun.spot.io.j2me.radiogram.Radiogram;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.Condition;
import com.sun.spot.resources.transducers.IConditionListener;
import com.sun.spot.resources.transducers.ILightSensor;
import com.sun.spot.resources.transducers.SensorEvent;
import com.sun.spot.resources.transducers.IAccelerometer3D;
import com.sun.spot.resources.transducers.ITemperatureInput;

import com.sun.spot.sensorboard.peripheral.ADT7411Event;
import com.sun.spot.sensorboard.peripheral.IADT7411ThresholdListener;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;

import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import project.CommonTypes;
import project.SpotCommons;

public class AggNodeMain extends MIDlet implements IADT7411ThresholdListener,
        IConditionListener, LocateServiceListener, CommonTypes, PacketHandler {

    private static final long SERVICE_CHECK_INTERVAL = 15000;
    private final int RX_NODE_PORT = 112;
    private final String SERVER_MAC = "7f00.0101.0000.1001";
    private final int SERVER_TX_PORT = 111;
    private final String MY_MAC = SpotCommons.getMyMAC(Spot.getInstance());
    private final long ALARM_FREQUENCY = 10000;
    private final int NODE_TX_PORT = 46;
    private final int TEMP_ALARM_THRESHOLD = 25;
    private final int ACC_ALARM_THRESHOLD = 2;
    private final long ALARM_FREQ = 5000;

    private RadiogramConnection txServerConn = null;
    private Datagram txServerDatagram;
    private long heartbeatInterval = 10 * 1000; // interval in milliseconds
    private long lastHeartbeatTimestamp = 0L;
    private ILightSensor light = (ILightSensor) Resources.lookup(ILightSensor.class);
    private ITemperatureInput temp = (ITemperatureInput) Resources.lookup(ITemperatureInput.class);
    private IAccelerometer3D accel = (IAccelerometer3D) Resources.lookup(IAccelerometer3D.class);
    private Node nodeList[] = new Node[3];
    private int nodeCount = 0;
    private LocateService locator;
    private RadiogramConnection hostConn;
    private PacketReceiver rcvr;
    private PacketTransmitter xmit;
    private boolean connected = false;
    private long lastSentTempAlarm = 0;
    private long lastSentAccAlarm = 0;

    private int tempValue = 0;
    private int accelXValue = 0;
    private int accelYValue = 0;
    private int accelZValue = 0;


    private void initialize() {
        locator = new LocateService(this, BROADCAST_PORT, SERVICE_CHECK_INTERVAL,
                LOCATE_DISPLAY_SERVER_REQ,
                DISPLAY_SERVER_AVAIL_REPLY,
                4);
    }

    private void run() {
        locator.start();
        try {
            RadiogramConnection rxConn = (RadiogramConnection) Connector.open("radiogram://:" + RX_NODE_PORT);
            Datagram rxDatagram = rxConn.newDatagram(rxConn.getMaximumLength());
            txServerConn = (RadiogramConnection) Connector.open("radiogram://" + SERVER_MAC + ":" + SERVER_TX_PORT);
            txServerDatagram = (Datagram) txServerConn.newDatagram(txServerConn.getMaximumLength());

            while (true) {
                parseIncomingData(rxConn, rxDatagram);
                readSensorValues();
                checkForTempAlarms();
                checkForAccAlarms();
                checkForSendingHeartbeat();
            }

        } catch (IOException e) {
            System.out.println("Error opening connection: " + e);
        }
    }

    protected void startApp() throws MIDletStateChangeException {
        initialize();
        run();
    }
    
    private void parseIncomingData(RadiogramConnection rxConn, Datagram rxDatagram) throws IOException {
        if (rxConn.packetsAvailable()) {
            rxConn.receive(rxDatagram);
            String rxData = rxDatagram.readUTF();
            System.out.println("Received data: " + rxData);
            parseRxData(rxData);
        }
    }

    private void checkForSendingHeartbeat() throws IOException {
        if (isReadyToSendHeartbeat()) {
            String frame = getHeartbeatFrame();
            sendToServer(frame);
            lastHeartbeatTimestamp = System.currentTimeMillis();
        }
    }

    private void checkForTempAlarms() {
        for (int i = 0; i < nodeCount; i++) {
            Node n = nodeList[i];
            if (n.isTempAlarmRaised() && (n.getLastTempAlarm() < System.currentTimeMillis() - ALARM_FREQUENCY)) {
                StringBuffer sb = new StringBuffer();
                sb.append("ALARMTEMP")
                        .append(",")
                        .append(MY_MAC)
                        .append(",")
                        .append(n.getMAC());
                sendToServer(sb.toString());
                n.setTempAlarmRaised(false);
                n.setLastTempAlarm(System.currentTimeMillis());
            }
        }
        checkMyTempAlarm();
    }

    private void checkForAccAlarms() {
        for (int i = 0; i < nodeCount; i++) {
            Node n = nodeList[i];
            if (n.isAccAlarmRaised() && (n.getLastAccAlarm() < System.currentTimeMillis() - ALARM_FREQUENCY)) {
                StringBuffer sb = new StringBuffer();
                sb.append("ALARMACC")
                        .append(",")
                        .append(MY_MAC)
                        .append(",")
                        .append(n.getMAC());
                sendToServer(sb.toString());
                n.setAccAlarmRaised(false);
                n.setLastAccAlarm(System.currentTimeMillis());
            }
        }
        checkMyAccAlarm();
    }

    private void checkMyAccAlarm() {
        if (lastSentAccAlarm < System.currentTimeMillis() - ALARM_FREQ
                && ((accelXValue >= ACC_ALARM_THRESHOLD)
                || (accelYValue >= ACC_ALARM_THRESHOLD)
                || (accelZValue >= ACC_ALARM_THRESHOLD))) {
            String frame = "ALARMACC," + MY_MAC + "," + MY_MAC;
            sendToServer(frame);
            lastSentAccAlarm = System.currentTimeMillis();
        }
    }

    private void checkMyTempAlarm() {
        if (lastSentTempAlarm < System.currentTimeMillis() - ALARM_FREQ
                && (tempValue >= TEMP_ALARM_THRESHOLD)) {
            String frame = "ALARMTEMP," + MY_MAC + "," + MY_MAC;
            sendToServer(frame);
            lastSentTempAlarm = System.currentTimeMillis();
        }
    }

    private void readSensorValues() throws IOException {
        tempValue = (int) temp.getCelsius();
        accelXValue = (int) accel.getAccelX();
        accelYValue = (int) accel.getAccelY();
        accelZValue = (int) accel.getAccelZ();
    }

    private boolean isReadyToSendHeartbeat() {
        long diff = System.currentTimeMillis() - lastHeartbeatTimestamp;
        boolean timerReady = diff > heartbeatInterval;
        boolean heartbeatSendStatus = true;
        for (int i = 0; i < nodeCount; i++) {
            heartbeatSendStatus = heartbeatSendStatus && nodeList[i].isLastHeartbeatSendStatus();
        }
        if (heartbeatSendStatus) {
            if (timerReady) {
                return true;
            }
        } else {
            if (diff > 1.2 * heartbeatInterval) {
                return true;
            }
        }
        return false;
    }

    private String getHeartbeatFrame() throws IOException {
        StringBuffer sb = new StringBuffer("HEARTBEAT." + MY_MAC + ",");
        for (int i = 0; i < nodeCount; i++) {
            sb.append(nodeList[i].getMAC())
                    .append(",")
                    .append(nodeList[i].getLightValue())
                    .append(",")
                    .append(nodeList[i].getTempValue())
                    .append(",")
                    .append(nodeList[i].getAccelXValue())
                    .append(",")
                    .append(nodeList[i].getAccelYValue())
                    .append(",")
                    .append(nodeList[i].getAccelZValue());
            nodeList[i].setLastHeartbeatSendStatus(true);
        }
        sb.append(MY_MAC)
                .append(",")
                .append(light.getValue())
                .append(",")
                .append((int) temp.getCelsius())
                .append(",")
                .append(accel.getAccelX())
                .append(",")
                .append(accel.getAccelY())
                .append(",")
                .append(accel.getAccelZ());
        return sb.toString();
    }

    private void sendToServer(String frame) {
        System.out.println("Send to server: " + frame);
        try {
            txServerDatagram.writeUTF(frame);
            txServerConn.send(txServerDatagram);
            txServerDatagram.reset();
        } catch (IOException ex) {
            System.out.println("Error sending frame to server " + ex);
        }
    }

    private void sendToNode(String mac, String frame) {
        RadiogramConnection tx = null;
        Datagram txDatagram;
        System.out.println("Sent to " + mac + " : " + frame);
        try {
            try {
                tx = (RadiogramConnection) Connector.open("radiogram://" + mac + ":" + NODE_TX_PORT);
                txDatagram = (Datagram) tx.newDatagram(tx.getMaximumLength());

                txDatagram.reset();
                txDatagram.writeUTF(frame);
                tx.send(txDatagram);

            } catch (IOException e) {
                System.out.println("Error sending frame to Node");
            } finally {
                tx.close();
            }
        }
        catch (IOException e) {
            System.out.println("Error sending to Node " + e);
        }

    }

    private void parseRxData(String data) {
        String[] fields = parseCSV(data);
        if (fields.length < 3) {
            return;
        }
        if (fields[0].equals("HEARTBEAT")) {
            Node n = getNodeByMAC(fields[1]);
            n.setLightValue(Integer.parseInt(fields[2]));
            n.setTempValue(Integer.parseInt(fields[3]));
            n.setAccelXValue(Integer.parseInt(fields[4]));
            n.setAccelYValue(Integer.parseInt(fields[5]));
            n.setAccelZValue(Integer.parseInt(fields[6]));
            n.setLastHeartbeat(System.currentTimeMillis());
        }
        else if(fields[0].equals("ALARMTEMP")
                || fields[0].equals("ALARMACC")) {
            StringBuffer sb = new StringBuffer(fields[0])
                    .append(",")
                    .append(MY_MAC)
                    .append(",")
                    .append(fields[1]);
            sendToServer(sb.toString());
            sendToNode(fields[1], "BLINK,1");
        }
    }

    private Node getNodeByMAC(String mac) {
        for (int i = 0; i < nodeCount; i++) {
            if (nodeList[i].getMAC().equals(mac)) {
                return nodeList[i];
            }
        }
        Node n = new Node(mac);
        nodeList[nodeCount++] = n;
        System.out.println("Registerd a new Node");
        sendToNode(mac, "BLINK,0");
        return n;
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

    public void conditionMet(SensorEvent evt, Condition condition) {
    }

    public void serviceLocated(long serverAddress) {
        try {
            if (hostConn != null) {
                hostConn.close();
            }
            hostConn = (RadiogramConnection) Connector.open("radiogram://"
                    + IEEEAddress.toDottedHex(serverAddress) + ":" + CONNECTED_PORT);
            hostConn.setTimeout(-1);    // no timeout
        } catch (IOException ex) {
            System.out.println("Failed to open connection to host: " + ex);
            closeConnection();
            return;
        }
        connected = true;

        xmit = new PacketTransmitter(hostConn);     // set up thread to transmit replies to host
        xmit.setServiceName("Telemetry Xmitter");
        xmit.start();

        rcvr = new PacketReceiver(hostConn);        // set up thread to receive & dispatch commands
        rcvr.setServiceName("Telemetry Command Server");

        rcvr.registerHandler(this, BLINK_LEDS_REQ);
        rcvr.start();
    }

    public void handlePacket(byte type, Radiogram pkt) {
        try {
            switch (type) {
                case BLINK_LEDS_REQ:
                    System.out.println("Blink: " + pkt.readUTF());
            }
        } catch (Exception ex) {
            closeConnection();
        }
    }

    public void closeConnection() {
        if (!connected) {
            return;
        }
        connected = false;
        xmit.stop();
        rcvr.stop();
        Utils.sleep(100);       // give things time to shut down
        try {
            if (hostConn != null) {
                hostConn.close();
                hostConn = null;
            }
        } catch (IOException ex) {
            System.out.println("Failed to close connection to host: " + ex);
        }
        locator.start();
    }
}
