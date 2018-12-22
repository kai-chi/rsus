/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package commons;

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
