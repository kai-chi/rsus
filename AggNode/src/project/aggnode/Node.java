/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project.aggnode;

/**
 *
 * @author userrsus
 */
public class Node {

    private String MAC;
    private int lightValue;
    private int tempValue;
    private int accelXValue;
    private int accelYValue;
    private int accelZValue;
    private long lastHeartbeat;
    private boolean tempAlarmRaised;
    private long lastTempAlarm;
    private boolean accAlarmRaised;
    private long lastAccAlarm;
    private boolean lastHeartbeatSendStatus = false;

    public Node(String MAC, int lightValue, int tempValue, int accelXValue, int accelYValue, int accelZValue) {
        this.MAC = MAC;
        this.lightValue = lightValue;
        this.tempValue = tempValue;
        this.accelXValue = accelXValue;
        this.accelYValue = accelYValue;
        this.accelZValue = accelZValue;
        this.lastHeartbeat = System.currentTimeMillis();
    }

    public Node(String MAC) {
        this.MAC = MAC;
    }

    public boolean isAccAlarmRaised() {
        return accAlarmRaised;
    }

    public void setAccAlarmRaised(boolean accAlarmRaised) {
        this.accAlarmRaised = accAlarmRaised;
    }

    public boolean isTempAlarmRaised() {
        return tempAlarmRaised;
    }

    public void setTempAlarmRaised(boolean tempAlarmRaised) {
        this.tempAlarmRaised = tempAlarmRaised;
    }

    public long getLastAccAlarm() {
        return lastAccAlarm;
    }

    public void setLastAccAlarm(long lastAccAlarm) {
        this.lastAccAlarm = lastAccAlarm;
    }

    public long getLastTempAlarm() {
        return lastTempAlarm;
    }

    public void setLastTempAlarm(long lastTempAlarm) {
        this.lastTempAlarm = lastTempAlarm;
    }

    public String getMAC() {
        return MAC;
    }

    public long getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(long lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
        this.lastHeartbeatSendStatus = false;
    }

    public boolean isLastHeartbeatSendStatus() {
        return lastHeartbeatSendStatus;
    }

    public void setLastHeartbeatSendStatus(boolean lastHeartbeatSendStatus) {
        this.lastHeartbeatSendStatus = lastHeartbeatSendStatus;
    }

    public int getLightValue() {
        return lightValue;
    }

    public void setLightValue(int lightValue) {
        this.lightValue = lightValue;
    }

    public double getTempValue() {
        return tempValue;
    }

     public void setTempValue(int tempValue) {
        this.tempValue = tempValue;
    }
      public double getAccelXValue() {
        return accelXValue;
    }

     public void setAccelXValue(int accelXValue) {
        this.accelXValue = accelXValue;
    }
           public double getAccelYValue() {
        return accelYValue;
    }

     public void setAccelYValue(int accelYValue) {
        this.accelYValue = accelYValue;
    }
           public double getAccelZValue() {
        return accelZValue;
    }

     public void setAccelZValue(int accelZValue) {
        this.accelZValue = accelZValue;
    }
}
