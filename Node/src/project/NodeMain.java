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
import project.SpotCommons;

public class NodeMain extends MIDlet implements IADT7411ThresholdListener {
    
    private ILightSensor light = (ILightSensor)Resources.lookup(ILightSensor.class);
    private ITemperatureInput temp = (ITemperatureInput) Resources.lookup(ITemperatureInput.class);
    private IAccelerometer3D accel = (IAccelerometer3D) Resources.lookup(IAccelerometer3D.class);

    private RadiogramConnection tx = null;
    private Datagram dg;

    private final String MY_MAC = SpotCommons.getMyMAC(Spot.getInstance());

   protected void startApp() throws MIDletStateChangeException {
        int lightValue = 0;
        int tempValue = 0;
        int accelXValue = 0;
        int accelYValue = 0;
        int accelZValue = 0;
        try {
           while (true) {
               tx = (RadiogramConnection)Connector.open("radiogram://7f00.0101.0000.1002:112");
               dg = (Datagram) tx.newDatagram(tx.getMaximumLength());
               lightValue = light.getValue();
               tempValue = (int) temp.getCelsius();
               accelXValue = (int) accel.getAccelX();
               accelYValue = (int) accel.getAccelY();
               accelZValue = (int) accel.getAccelZ();
               try {
                   dg.reset();
                   dg.writeUTF("HEARTBEAT,"
                           + MY_MAC
                           + ","
                           + lightValue
                           + ","
                           + tempValue
                           + ","
                           + accelXValue
                           + ","
                           + accelYValue
                           + ","
                           + accelZValue);
                   tx.send(dg);
                System.out.println("Sent data: " + lightValue + tempValue + accelXValue + accelYValue + accelZValue);
               }
               catch (IOException ex) {
                   System.out.println("Error receiving packet: " + ex);
               }
               finally {
                   tx.close();
               }
               Utils.sleep(10000);
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

    public void thresholdChanged(ADT7411Event evt) {
    }

    public void thresholdExceeded(ADT7411Event evt) {
    }

}
