package project.aggnode;

import com.sun.spot.io.j2me.radiogram.Radiogram;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;

import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;

import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.midlet.MIDletStateChangeException;
import project.CommonTypes;
import project.SimpleNode;
import project.SpotCommons;

public class AggNodeMain extends SimpleNode implements
        LocateServiceListener, CommonTypes, PacketHandler {

    private static final long SERVICE_CHECK_INTERVAL = 15000;
    private final String SERVER_MAC = "7f00.0101.0000.1001";
    private final int SERVER_TX_PORT = 111;
    private final int NODE_TX_PORT = 46;

    private RadiogramConnection txServerConn = null;
    private Datagram txServerDatagram;
    private Node nodeList[] = new Node[3];
    private int nodeCount = 0;
    private LocateService locator;
    private RadiogramConnection hostConn;
    private PacketReceiver rcvr;
    private PacketTransmitter xmit;
    private boolean connected = false;


    private void initialize() {
        locator = new LocateService(this, BROADCAST_PORT, SERVICE_CHECK_INTERVAL,
                LOCATE_DISPLAY_SERVER_REQ,
                DISPLAY_SERVER_AVAIL_REPLY,
                4);
    }

    private void run() {
        locator.start();
        startAlarmConditions();
        try {
            openRxConnection();
            txServerConn = (RadiogramConnection) Connector.open("radiogram://" + SERVER_MAC + ":" + SERVER_TX_PORT);
            txServerDatagram = (Datagram) txServerConn.newDatagram(txServerConn.getMaximumLength());

            while (true) {
                parseIncomingData(rxConn, rxDatagram);
                readSensorValues();
                checkForTempAlarms();
                checkForAccAlarms();
                sendHeartbeat();
            }

        } catch (IOException e) {
            System.out.println("Error opening connection: " + e);
        }
    }

    protected void startApp() throws MIDletStateChangeException {
        initialize();
        run();
    }

    private void checkForTempAlarms() {
        for (int i = 0; i < nodeCount; i++) {
            Node n = nodeList[i];
            if (n.isTempAlarmRaised() && (n.getLastTempAlarm() < System.currentTimeMillis() - ALARM_FREQ)) {
                StringBuffer sb = new StringBuffer();
                sb.append("ALARMTEMP")
                        .append(",")
                        .append(MY_MAC)
                        .append(",")
                        .append(n.getMAC());
                sendFrameToServer(sb.toString());
                n.setTempAlarmRaised(false);
                n.setLastTempAlarm(System.currentTimeMillis());
            }
        }
    }

    private void checkForAccAlarms() {
        for (int i = 0; i < nodeCount; i++) {
            Node n = nodeList[i];
            if (n.isAccAlarmRaised() && (n.getLastAccAlarm() < System.currentTimeMillis() - ALARM_FREQ)) {
                StringBuffer sb = new StringBuffer();
                sb.append("ALARMACC")
                        .append(",")
                        .append(MY_MAC)
                        .append(",")
                        .append(n.getMAC());
                sendFrameToServer(sb.toString());
                n.setAccAlarmRaised(false);
                n.setLastAccAlarm(System.currentTimeMillis());
            }
        }
    }

    private void sendToNode(String mac, String frame) {
        System.out.println("Send to " + mac + " : " + frame);
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

    public void parseRxData(String data) {
        String[] fields = SpotCommons.parseCSV(data);
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
            sendFrameToServer(sb.toString());
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

        rcvr.registerHandler(this, BLINK_LEDS_REQ_NODE_1);
        rcvr.registerHandler(this, BLINK_LEDS_REQ_NODE_2);
        rcvr.registerHandler(this, BLINK_LEDS_REQ_NODE_3);
        rcvr.registerHandler(this, BLINK_LEDS_REQ_NODE_4);
        rcvr.start();
    }

    public void handlePacket(byte type, Radiogram pkt) {
        try {
            System.out.println("Received message from the server " + type);
            switch (type) {
                case BLINK_LEDS_REQ_NODE_1:
                    blinkLEDs(2);
                    break;
                case BLINK_LEDS_REQ_NODE_2:
                    sendToNode(nodeList[0].getMAC(), "BLINK,2");
                    break;
                case BLINK_LEDS_REQ_NODE_3:
                    sendToNode(nodeList[1].getMAC(), "BLINK,2");
                    break;
                case BLINK_LEDS_REQ_NODE_4:
                    sendToNode(nodeList[2].getMAC(), "BLINK,2");
                    break;
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

    public void sendHeartbeat() throws IOException {
        if (isReadyToSendHeartbeat()) {
            String frame = getHeartbeatFrame();
            sendFrameToServer(frame);
            lastSentHeartbeat = System.currentTimeMillis();
        }
    }

    private boolean isReadyToSendHeartbeat() {
        long diff = System.currentTimeMillis() - lastSentHeartbeat;
        boolean timerReady = diff > HEARTBEAT_FREQ;
        boolean heartbeatSendStatus = true;
        for (int i = 0; i < nodeCount; i++) {
            heartbeatSendStatus = heartbeatSendStatus && nodeList[i].isLastHeartbeatSendStatus();
        }
        if (heartbeatSendStatus) {
            if (timerReady) {
                return true;
            }
        } else {
            if (diff > 1.2 * HEARTBEAT_FREQ) {
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
                .append(lightValue)
                .append(",")
                .append(tempValue)
                .append(",")
                .append(accXValue)
                .append(",")
                .append(accYValue)
                .append(",")
                .append(accZValue);
        return sb.toString();
    }

    public void sendFrameToServer(String frame) {
        System.out.println("Send to server: " + frame);
        try {
            txServerDatagram.writeUTF(frame);
            txServerConn.send(txServerDatagram);
            txServerDatagram.reset();
        } catch (IOException ex) {
            System.out.println("Error sending frame to server " + ex);
        }
    }

    public int getRxPort() {
        return 112;
    }
}
