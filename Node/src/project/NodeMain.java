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

    private ILightSensor light = (ILightSensor) Resources.lookup(ILightSensor.class);
    private ITemperatureInput temp = (ITemperatureInput) Resources.lookup(ITemperatureInput.class);
    private IAccelerometer3D accel = (IAccelerometer3D) Resources.lookup(IAccelerometer3D.class);
    private RadiogramConnection tx = null;
    private Datagram dg;
    private final String MY_MAC = SpotCommons.getMyMAC(Spot.getInstance());

    protected void startApp() throws MIDletStateChangeException {
        int Alarm_Temp = 25;  // Add pour Comparaison
        int Alarm_Accel = 2; // Add pour Comparaison
        int lightValue = 0;
        int tempValue = 0;
        int accelXValue = 0;
        int accelYValue = 0;
        int accelZValue = 0;
        try {
            while (true) {
                tx = (RadiogramConnection) Connector.open("radiogram://7f00.0101.0000.1002:112");
                dg = (Datagram) tx.newDatagram(tx.getMaximumLength());
                lightValue = light.getValue();
                tempValue = (int) temp.getCelsius();
                accelXValue = (int) accel.getAccelX();
                accelYValue = (int) accel.getAccelY();
                accelZValue = (int) accel.getAccelZ();

                try {
                    dg.reset();
                    if ((accelXValue >= Alarm_Accel) || (accelYValue >= Alarm_Accel) || (accelZValue >= Alarm_Accel)) {
                        try {
                            dg.reset();
                            dg.writeUTF("ALARMACCEL,"
                                    + MY_MAC
                                    + ","
                                    + accelXValue
                                    + ","
                                    + accelYValue
                                    + ","
                                    + accelZValue);
                            tx.send(dg);
                            System.out.println("Sent ALARMACCEL");
                        } catch (IOException ex) {
                            System.out.println("Error receiving packet: " + ex);
                        }
                    }
                    if ((tempValue >= Alarm_Temp)) {
                        try {
                            dg.reset();
                            dg.writeUTF("ALARMTEMP,"
                                    + MY_MAC
                                    + ","
                                    + tempValue);
                            tx.send(dg);
                            System.out.println("Sent ALARMTEMP");
                        } catch (IOException ex) {
                            System.out.println("Error receiving packet: " + ex);
                        }
                    }
                    dg.reset();
                    StringBuffer sb = new StringBuffer("HEARTBEAT,");
                    String frame = sb.append(MY_MAC)
                            .append(",")
                            .append(lightValue)
                            .append(",")
                            .append(tempValue)
                            .append(",")
                            .append(accelXValue)
                            .append(",")
                            .append(accelYValue)
                            .append(",")
                            .append(accelZValue)
                            .toString();
                    dg.writeUTF(frame);
                    tx.send(dg);
                    System.out.println("Sent data: " + frame);
                } /* catch (IOException ex) {
                System.out.println("Error receiving packet: " + ex);
                }
                 */ finally {
                    tx.close();
                }
                Utils.sleep(5000);
            }
        } catch (Exception e) {
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
