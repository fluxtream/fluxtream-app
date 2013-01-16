package com.fluxtream.mvc.models;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class DurationModel {

    public int totalSeconds;

    public int seconds;
    public int minutes;
    public int hours;

    public DurationModel(int secs) {
        totalSeconds = secs;

        hours = secs / (60 * 60);

        int divisor_for_minutes = secs % (60 * 60);
        minutes = divisor_for_minutes / 60;

        int divisor_for_seconds = divisor_for_minutes % 60;
        seconds = (int)Math.ceil(divisor_for_seconds);
    }

}
