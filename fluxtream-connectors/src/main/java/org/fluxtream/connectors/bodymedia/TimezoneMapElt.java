package org.fluxtream.connectors.bodymedia;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;


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
  public final DateTimeZone tz;

  public TimezoneMapElt(DateTime start, DateTime end, DateTimeZone tz) {
      this.start = start;
      this.end = end;
      this.tz=tz;
  }
}