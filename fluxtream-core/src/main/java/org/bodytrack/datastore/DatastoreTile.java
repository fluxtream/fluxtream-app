package org.bodytrack.datastore;

/**
 * <p>
 * <code>DatastoreTile</code> does something...
 * </p>
 *  This is translated from bodytrack/website/app/models/tile.rb
 * @author Anne Wright
 */
public class DatastoreTile {
    public final static double TILE_BREAK = (-1e308);
    public final static double TILE_BREAK_THRESH = (-1e307);
    public final static long TILE_BIN_NUM=512;

    // Returns the minimum tile level requred to fully represent the
    // closest spaced data points you want to preserve from min_spacing in
    // seconds.  For example, 1.2 sec min_spacing would return level 0
    // which is a bin spacing of 1 second; 0.005 sec would return level -8
    public static int min_delta_to_level(final double min_spacing) {
        return ((int)(Math.floor(Math.log(min_spacing) / Math.log(2.0))));
    }

    // Returns the time in seconds of a bin in a tile at a given level.
    // This is 2^level seconds, so level 0 bins are 1 second, level 4 are
    // 16 secs, etc.
    public static double level_to_bin_secs(final int level) {
        return (Math.pow(2, (double)level));
    }

      // Returns the duration in seconds of a tile at a given level.  For
  // level 0 this is TILE_BIN_NUM seconds
  public static double level_to_duration(final int level) {
    return(level_to_bin_secs(level)*TILE_BIN_NUM);
  }

      // Returns the offset of the tile at a given level containing the given
  // unixtime.  This is the number of intervals for the given level that
  // have elapsed since the unixtime epoch (midnight on Jan 1, 1970) for
  // the tile containing unixtime
  public static long unixtime_at_level_to_offset(final double unixtime, final int level) {
    return((long)Math.floor(unixtime/level_to_duration(level)));
  }

  // Returns the unixtime of the start of a tile at a given offset and
  // level.  This is the duration at that level * the offset.
  public static double offset_at_level_to_unixtime(final long offset,final int level) {
    return(level_to_duration(level)*offset);
  }

  // Returns the minimum tile level requred to fully represent the
  // closest spaced data points you want to preserve from min_spacing in
  // seconds.  For example, 1.2 sec min_spacing would return level 0
  // which is a bin spacing of 1 second; 0.005 sec would return level -8
  public static int delta_to_tile_level(final double min_spacing) {
    return((int)Math.floor(Math.log(min_spacing)/Math.log(2)));
  }

  // Returns the minimum tile level at which the duration of the tile
  // exceed the given duration in seconds.  Note that depending on
  // alignment this may not cover a particular timespan with that
  // delta; two tiles may be required.
  public static int duration_to_tile_level(final double duration) {
    return((delta_to_tile_level(duration/TILE_BIN_NUM))+1);
  }
}
