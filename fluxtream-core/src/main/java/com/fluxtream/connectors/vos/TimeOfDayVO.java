package com.fluxtream.connectors.vos;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class TimeOfDayVO {

    public int minutes;
    public int hours;

    public TimeOfDayVO(int minutes) {
        this.minutes = minutes%60;
        this.hours = minutes/60;
    }

}
