package com.fluxtream.services.impl.converters;

import java.util.Date;
import com.fluxtream.domain.AbstractFacet;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class ZeoSleepGraphConverter  implements Converter {

    @Override
    public String convert(AbstractFacet facet) {
        return null;
    }

    @Override
    public String getBodytrackChannelName() {
        return "Sleep_Graph";
    }

    /**
     * Formats a byte array as a String.
     *
     * @param b a byte array
     * @return a String representing the values in the byte array
     */
    //private String formatHypnogram(String b[], Date startTime, boolean isLast) {
    //
    //    StringBuffer s = new StringBuffer();
    //
    //    // strip off trailing zeroes
    //    int lastNonzeroIndex = 0;
    //    for (int i = 0; i < b.length; i++) {
    //        if (!b[i].equals("0")) lastNonzeroIndex = i;
    //    }
    //
    //    for (int i = 0; i < lastNonzeroIndex; i++) {
    //        switch (SleepStage.convert(b[i])) {
    //            case UNUSED:
    //                // skip the unused sleep stage label to match website CSV output
    //                break;
    //
    //            case DEEP_2:
    //                // show DEEP_2 as DEEP to match website CSV output
    //                b[i] = (byte) SleepStage.DEEP.ordinal();
    //                break;
    //
    //            default:
    //                // Remap sleep values
    //                int val = (b[i] > 0) ? 5-b[i] : 0;
    //
    //                s.append("      [" + Long.toString(startTime.getTime()/1000 + i*ZeoData.HYP_BASE_STEP) + "," + (char) (val + '0') + "]");
    //
    //                if (isLast && i == lastNonzeroIndex-1)
    //                    s.append("\n");
    //                else
    //                    s.append(",\n");
    //
    //                break;
    //        }
    //    }
    //    return s.toString();
    //}

}
