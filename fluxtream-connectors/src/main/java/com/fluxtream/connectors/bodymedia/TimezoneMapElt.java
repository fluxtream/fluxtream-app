package com.fluxtream.connectors.bodymedia;

import java.util.TimeZone;
import org.joda.time.DateTime;

/**
 * <p>
 * <code>TimezoneMapElt</code> does something...
 * </p>
 *
 * @author Anne Wright (arwright@cmu.edu)
 */
public class TimezoneMapElt {
  public final DateTime start;
  public final DateTime end;
  public final TimeZone tz;

  public TimezoneMapElt(DateTime start, DateTime end, TimeZone tz) {
      this.start = start;
      this.end = end;
      this.tz=tz;
  }
}