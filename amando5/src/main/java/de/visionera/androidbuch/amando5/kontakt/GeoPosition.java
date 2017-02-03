package de.visionera.androidbuch.amando5.kontakt;

import de.visionera.androidbuch.amando5.common.GpsData;


/**
 * Ein aktueller Ortspunkt. Wird verwendet, um die kontinuierliche Verfolgung
 * einer Person zu ermöglichen. 
 * Jede Ortsveränderung wird über den Server an den Empfänger geschickt. Der
 * Server dient als Vermittler. Über die Mobilnummer erfolgt die korrekte
 * Zuweisung und Übertragung der Daten an den richtigen Empfänger. Sender der
 * Ortsposition und Empfänger sind beide ständig mit dem Server verbunden.
 * 
 * @author Arno Becker, 2010 visionera GmbH
 * 
 */
public class GeoPosition {
  
  public GpsData mGpsData;
  public String mMobilnummer;
  
  public GeoPosition(final GpsData gpsData, 
      final String mobilnummer) {
    mGpsData = gpsData;
    mMobilnummer = mobilnummer;
  }  
}
