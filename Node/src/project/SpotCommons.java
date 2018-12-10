/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project;

import com.sun.spot.peripheral.ISpot;
import com.sun.spot.util.IEEEAddress;

/**
 *
 * @author userrsus
 */
public class SpotCommons {

    public static String getMyMAC(ISpot spot) {
        long address = spot.getRadioPolicyManager().getIEEEAddress();
//        if ((address & 1) == 0) {
//            address -= 1;           // even addresses pair with smaller odd addresses
//        } else {
//            address += 1;           // odd addresses pair with larger even addresses
//        }
        String s = IEEEAddress.toDottedHex(address);
        System.out.println("getMyMAC: " + s);
        return s;
    }

}
