package project.aggnode;

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

import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import project.SpotCommons;

public class AggNodeMain extends MIDlet implements IADT7411ThresholdListener,
        IConditionListener {

    private final int RX_NODE_PORT = 112;
    private final String SERVER_MAC = "7f00.0101.0000.1001";
    private final int SERVER_TX_PORT = 111;
    private final String MY_MAC = SpotCommons.getMyMAC(Spot.getInstance());

    private RadiogramConnection txServerConn = null;
    private Datagram txServerDatagram;

    private long heartbeatInterval = 10 * 1000; // interval in milliseconds
    private long lastHeartbeatTimestamp = 0L;
    private ILightSensor light = (ILightSensor)Resources.lookup(ILightSensor.class);
    private ITemperatureInput temp = (ITemperatureInput) Resources.lookup(ITemperatureInput.class);
    private IAccelerometer3D accel = (IAccelerometer3D) Resources.lookup(IAccelerometer3D.class);

    private Node nodeList[] = new Node[3];
    private int nodeCount = 0;

    protected void startApp() throws MIDletStateChangeException {

        try {
            RadiogramConnection rxConn = (RadiogramConnection) Connector.open("radiogram://:" + RX_NODE_PORT);
            Datagram rxDatagram = rxConn.newDatagram(rxConn.getMaximumLength());
            txServerConn = (RadiogramConnection)Connector.open("radiogram://" + SERVER_MAC + ":" + SERVER_TX_PORT);
            txServerDatagram = (Datagram) txServerConn.newDatagram(txServerConn.getMaximumLength());

            while (true) {
                //check if something is in the buffer
                if (rxConn.packetsAvailable()) {
                    rxConn.receive(rxDatagram);
                    String rxData = rxDatagram.readUTF();
                    System.out.println("Received data: " + rxData);
                    parseRxData(rxData);
                }

                //check if it's time to send a heartbeat
                if (isReadyToSendHeartbeat()) {
                    System.out.println("Ready to send heartbeat");
                    String frame = getHeartbeatFrame();
                    sendToServer(frame);
                    lastHeartbeatTimestamp = System.currentTimeMillis();
                }
            }

        } catch (IOException e) {
            System.out.println("Error opening connection: " + e);
        }
    }
    
    private boolean isReadyToSendHeartbeat() {
        long diff = System.currentTimeMillis() - lastHeartbeatTimestamp;
        boolean timerReady =  diff > heartbeatInterval;
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

    private String getHeartbeatFrame() {
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
        try {
         sb.append(MY_MAC)
           .append(",")
           .append(light.getValue())
           .append(",")
           .append(temp.getCelsius())
           .append(",")
           .append(accel.getAccelX())
           .append(",")
           .append(accel.getAccelY())
           .append(",")
           .append(accel.getAccelZ());
        } catch (Exception e) {
            System.out.println("Error getting light Value, temp Value, accel Value: " + e);
        }
        return sb.toString();
    }

    private void sendToServer(String frame) {
        try {
            txServerDatagram.writeUTF(frame);
            txServerConn.send(txServerDatagram);
            txServerDatagram.reset();
        } catch (IOException ex) {
            System.out.println("Error sending frame to server " + ex);
        }
    }

    private void parseRxData(String data) {
        String[] fields = parseCSV(data);
        if (fields.length < 6) {
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
    }

    private Node getNodeByMAC(String MAC) {
        for (int i = 0; i < nodeCount; i++) {
            if (nodeList[i].getMAC().equals(MAC)) {
                return nodeList[i];
            }
        }
        Node n = new Node(MAC);
        nodeList[nodeCount++] = n;
        System.out.println("Registerd a new Node");
        return n;
    }

    private String[] parseCSV (String line) {
        int elements = 0;
        String[] fields = new String[10];
        String s = line;
        while(s.length() > 0) {
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
}
