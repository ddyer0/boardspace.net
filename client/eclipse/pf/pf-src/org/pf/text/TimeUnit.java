// ===========================================================================
// CONTENT  : ENUM TimeUnit
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 13/09/2013
// HISTORY  :
//  13/09/2013  mdu  CREATED
//
// Copyright (c) 2013, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text;

/**
 * This enum provides several time units with an associated factor useful
 * to calculate the milliseconds from a time value given in a different time unit.
 */
public enum TimeUnit 
{
  // =========================================================================
  // ENUM VALUES
  // =========================================================================
  MILLISECONDS("milliseconds", "ms", 1L),
  SECONDS("seconds", "s", 1000L),
  MINUTES("minutes", "m", 60000L),
  HOURS("hours", "h", 3600000L);
  
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private String longName;
  private String shortName;
  private long msFactor;
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  private TimeUnit(String longName, String shortName, long msFactor)
  {
    this.longName = longName;
    this.shortName = shortName;
    this.msFactor = msFactor;
  } // TimeUnit() 
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the string long name of the unit. 
   */
  public String getLongName()
  {
    return this.longName;
  } // getLongName() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the string short name of the unit. 
   */
  public String getShortName()
  {
    return this.shortName;
  } // getShortName() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the multiplication factor for this unit to 
   * convert a corresponding value to milliseconds. 
   */
  public long getMsFactor() 
  {
    return this.msFactor;
  } // getMsFactor() 
  
  // -------------------------------------------------------------------------
}
