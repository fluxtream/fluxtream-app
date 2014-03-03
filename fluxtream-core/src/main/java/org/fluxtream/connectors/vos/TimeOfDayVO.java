package org.fluxtream.connectors.vos;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class TimeOfDayVO {

    public String minutes;
    public String hours;
    public String ampm = "am";

    public TimeOfDayVO(int minutes, boolean ampm) {
        this.minutes = String.valueOf(minutes%60);
        if (this.minutes.length()==1) this.minutes = "0" + this.minutes;
        if (ampm) {
            int hourOfDay = minutes/60;
            if (hourOfDay >= 12){
                this.ampm = "pm";
                if (hourOfDay>=13) {
                    hourOfDay -=12;
                }
            }
            else if (hourOfDay == 0){
                hourOfDay = 12;
            }
            this.hours = String.valueOf(hourOfDay);
        } else {
            this.hours = String.valueOf(minutes/60);
        }
    }

    public static void main(final String[] args) {
        TimeOfDayVO o = new TimeOfDayVO(950, true);
        System.out.println(o);
    }

}
