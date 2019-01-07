/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package project;

import com.sun.spot.peripheral.ISpot;
import com.sun.spot.util.IEEEAddress;

/**
 * SpotCommons is a library of commonly used methods across all components
 * @author userrsus
 */
public class SpotCommons {

    /**
     *
     * @param spot
     * @return
     */
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

    /**
     * Parses a CSV message into an array of String
     * @param line CSV message
     * @return array of String with the values
     */
    public static String[] parseCSV(String line) {
        int elements = 0;
        String[] fields = new String[10];
        String s = line;
        while (s.length() > 0) {
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
}
