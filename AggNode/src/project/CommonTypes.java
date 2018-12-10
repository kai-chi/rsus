/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package project;

/**
 *
 * @author userrsus
 */
public interface CommonTypes {

    /** Port to use to locate the host application. */
    public static final String BROADCAST_PORT = "42";
    /** Port to use for sending commands and replies between the SPOT and the host application. */
    public static final String CONNECTED_PORT = "43";
    // Command & reply codes for data packets
    /** Client command to locate a display server. */
    public static final byte LOCATE_DISPLAY_SERVER_REQ = 1;    // sent to display host (broadcast)
    /** Host command to indicate it is restarting. */
    public static final byte DISPLAY_SERVER_RESTART = 2;    // sent to any clients (broadcast)
    /** Host command to blink the remote SPOT's LEDs. */
    public static final byte BLINK_LEDS_REQ = 10;
    /** Host reply to indicate it is available. */
    public static final byte DISPLAY_SERVER_AVAIL_REPLY = 101;
}
