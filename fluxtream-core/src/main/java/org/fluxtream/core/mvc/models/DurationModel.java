package org.fluxtream.core.mvc.models;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class DurationModel {

    public int totalSeconds;

    public Integer seconds;
    public Integer minutes;
    public Integer hours;

    public DurationModel(int secs) {
        totalSeconds = secs;

        int hrs = secs / (60 * 60);

        int divisor_for_minutes = secs % (60 * 60);
        int min = divisor_for_minutes / 60;

        int divisor_for_seconds = divisor_for_minutes % 60;
        int s = (int)Math.ceil(divisor_for_seconds);

        if (hrs>0) hours = hrs;
        if (min>0) minutes = min;
        if (s>0) seconds = s;
    }

    public String toString() {
        return hours + ":" + pad(minutes) + ":" + pad(seconds);
    }

    static String pad (int i) {
        return i<10?"0"+i : String.valueOf(i);
    }


}
