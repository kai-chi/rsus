package project;

import commons.SpotCommons;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.util.Utils;

import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.midlet.MIDletStateChangeException;
import commons.SimpleNode;

public class NodeMain extends SimpleNode {

    protected void startApp() throws MIDletStateChangeException {
        try {
            openRxConnection();
            startAlarmConditions();
            while (true) {
                parseIncomingData(rxConn, rxDatagram);
                sendHeartbeat();
                Utils.sleep(200);
            }
        } catch (Exception e) {
            System.out.println("Connection error: " + e);
        }
    }
    
    public void parseRxData(String data) {
        String[] fields = SpotCommons.parseCSV(data);
        if (fields.length == 0) {
            return;
        }
        if (fields[0].equals("BLINK")) {
            blinkLEDs(Integer.parseInt(fields[1]));
        }
    }

    public void sendHeartbeat() throws IOException {
        if (lastSentHeartbeat < System.currentTimeMillis() - HEARTBEAT_FREQ) {
            StringBuffer sb = new StringBuffer("HEARTBEAT,");
            readSensorValues();
            String frame = sb.append(MY_MAC)
                    .append(",")
                    .append(lightValue)
                    .append(",")
                    .append(tempValue)
                    .append(",")
                    .append(accXValue)
                    .append(",")
                    .append(accYValue)
                    .append(",")
                    .append(accZValue)
                    .toString();
            sendFrameToServer(frame);
            lastSentHeartbeat = System.currentTimeMillis();
        }
    }

    public void sendFrameToServer(String frame) throws IOException {
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

    public int getRxPort() {
        return 46;
    }
}
