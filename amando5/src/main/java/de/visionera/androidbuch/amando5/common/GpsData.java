package de.visionera.androidbuch.amando5.common;

import android.location.Location;


/**
 * @author Arno Becker, 2015 visionera GmbH
 */
public class GpsData {
  
  /** Schlüssel für die Location. */
  public static final String KEY_LOCATION = 
    "location";
  
  /** Schlüssel für den Längengrad. Wird als POST-
   * Parameterschlüssel bei der Datenübertragung 
   * benötigt. */
  public static final String KEY_LAENGENGRAD = 
    "laengengrad";
  
  /** Schlüssel für den Breitengrad. Wird als POST-
   * Parameterschlüssel bei der Datenübertragung 
   * benötigt. */
  public static final String KEY_BREITENGRAD = 
    "breitengrad";
  
  /** Schlüssel für die Höhe. Wird als POST-
   * Parameterschlüssel bei der Datenübertragung 
   * benötigt. */
  public static final String KEY_HOEHE = "hoehe";
  
  /** Schlüssel für den Zeitstempel. Wird als POST-
   * Parameterschlüssel bei der Datenübertragung 
   * benötigt. */
  public static final String KEY_ZEITSTEMPEL = 
    "zeitstempel";
  
  public Location location;
  
  /**
   * Konstruktor zur Erzeugung eines GpsData-Objekts.
   * 
   * @param location Längengrad, Breitengrad, Höhe üNN,
   * Zeitpunkt
   */
  public GpsData(Location location) {
    this.location = location;    
  }
  
  public double getLaengengrad() {
    return location.getLongitude();
  }
  
  public double getBreitengrad() {
    return location.getLatitude();
  }
  
  public double getHoehe() {
    return location.getAltitude();
  }
  
  public long getZeitstempel() {
    return location.getTime();
  }

  @Override
  public String toString() {
    return "GpsData [Breitengrad=" + location.getLatitude()
        + ", Laengengrad=" + location.getLongitude()
        + ", Hoehe=" + location.getAltitude()  
        + ", Zeitstempel=" + location.getTime()
        + "]";
  }

}
