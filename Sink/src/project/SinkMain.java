package project;

import com.sun.spot.io.j2me.radiogram.RadiogramConnection;


import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

public class SinkMain extends MIDlet {

    protected void startApp() throws MIDletStateChangeException {
        try {
           RadiogramConnection rx = (RadiogramConnection) Connector.open("radiogram://:111");
           Datagram dg = rx.newDatagram(rx.getMaximumLength());
           while (true) {
               try {
                rx.receive(dg);
                String data = dg.readUTF();
                if (data.length() != 0) {
                 System.out.println("Received data: " + data);
                }
               }
               catch (IOException ex) {
                   System.out.println("Error receiving packet: " + ex);
               }


           }
        }
        catch (Exception e) {
            System.out.println("Error opening connection: " + e);
        }
    }
    
    protected void pauseApp() {
        // This will never be called by the Squawk VM
    }
    
    protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
        // Only called if startApp throws any exception other than MIDletStateChangeException
    }
}
