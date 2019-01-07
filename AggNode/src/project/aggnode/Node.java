/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project.aggnode;

/**
 * Node is a helper class that holds all the information about the registered
 * node. It is used by the AggNode to keep track of the registered Nodes.
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

    /**
     *
     * @param MAC
     * @param lightValue
     * @param tempValue
     * @param accelXValue
     * @param accelYValue
     * @param accelZValue
     */
    public Node(String MAC, int lightValue, int tempValue, int accelXValue, int accelYValue, int accelZValue) {
        this.MAC = MAC;
        this.lightValue = lightValue;
        this.tempValue = tempValue;
        this.accelXValue = accelXValue;
        this.accelYValue = accelYValue;
        this.accelZValue = accelZValue;
        this.lastHeartbeat = System.currentTimeMillis();
    }

    /**
     *
     * @param MAC
     */
    public Node(String MAC) {
        this.MAC = MAC;
    }

    /**
     * Returns a boolean if the accelerometer alarm has been reported to the server
     * @return
     */
    public boolean isAccAlarmRaised() {
        return accAlarmRaised;
    }

    /**
     * Sets the status of the acc alarm - raised or not raised
     * @param accAlarmRaised
     */
    public void setAccAlarmRaised(boolean accAlarmRaised) {
        this.accAlarmRaised = accAlarmRaised;
    }

    /**
     * Returns a boolean if the temperature alarm has been reported to the server
     * @return
     */
    public boolean isTempAlarmRaised() {
        return tempAlarmRaised;
    }

    /**
     * Sets the status of the temperature alarm - raised or not raised
     * @param tempAlarmRaised
     */
    public void setTempAlarmRaised(boolean tempAlarmRaised) {
        this.tempAlarmRaised = tempAlarmRaised;
    }

    /**
     * Returns timestamp of the last sent acc alarm
     * @return
     */
    public long getLastAccAlarm() {
        return lastAccAlarm;
    }

    /**
     * Sets timestamp of the last sent acc alarm
     * @param lastAccAlarm last sent acc alarm
     */
    public void setLastAccAlarm(long lastAccAlarm) {
        this.lastAccAlarm = lastAccAlarm;
    }

    /**
     * Returns the last sent temp alarm
     * @return
     */
    public long getLastTempAlarm() {
        return lastTempAlarm;
    }

    /**
     * Sets timestamp of the last sent temperature alarm
     * @param lastTempAlarm last sent temperature alarm
     */
    public void setLastTempAlarm(long lastTempAlarm) {
        this.lastTempAlarm = lastTempAlarm;
    }

    /**
     * Returns the MAC address of the Node
     * @return
     */
    public String getMAC() {
        return MAC;
    }

    /**
     * Returns the timestamp of the last sent heartbeat message
     * @return
     */
    public long getLastHeartbeat() {
        return lastHeartbeat;
    }

    /**
     * Sets the timestamp of the last sent heartbeat message
     * @param lastHeartbeat last sent heartbeat timestamp
     */
    public void setLastHeartbeat(long lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
        this.lastHeartbeatSendStatus = false;
    }

    /**
     * Return a boolean if the last reported heartbeat has been sent to the server
     * @return
     */
    public boolean isLastHeartbeatSendStatus() {
        return lastHeartbeatSendStatus;
    }

    /**
     * Sets the status of the last sent heartbeat
     * @param lastHeartbeatSendStatus
     */
    public void setLastHeartbeatSendStatus(boolean lastHeartbeatSendStatus) {
        this.lastHeartbeatSendStatus = lastHeartbeatSendStatus;
    }

    /**
     * Returns the current light value
     * @return
     */
    public int getLightValue() {
        return lightValue;
    }

    /**
     * Sets the light value
     * @param lightValue
     */
    public void setLightValue(int lightValue) {
        this.lightValue = lightValue;
    }

    /**
     * Returns the last temperature value
     * @return
     */
    public double getTempValue() {
        return tempValue;
    }

    /**
     * Sets the temperature value
     * @param tempValue
     */
    public void setTempValue(int tempValue) {
        this.tempValue = tempValue;
    }
    /**
     * Returns the X-axis accelerometer value
     * @return
     */
    public double getAccelXValue() {
        return accelXValue;
    }

    /**
     * Sets the X-axis accelerometer value
     * @param accelXValue
     */
    public void setAccelXValue(int accelXValue) {
        this.accelXValue = accelXValue;
    }
     /**
      *Returns the Y-axis accelerometer value
      * @return
      */
     public double getAccelYValue() {
        return accelYValue;
    }

     /**
      * Sets the X-axis accelerometer value
      * @param accelYValue
      */
     public void setAccelYValue(int accelYValue) {
        this.accelYValue = accelYValue;
    }
     /**
      * Returns the Z-axis accelerometer value
      * @return
      */
     public double getAccelZValue() {
        return accelZValue;
    }

     /**
      * Sets the X-axis accelerometer value
      * @param accelZValue
      */
     public void setAccelZValue(int accelZValue) {
        this.accelZValue = accelZValue;
    }
}
