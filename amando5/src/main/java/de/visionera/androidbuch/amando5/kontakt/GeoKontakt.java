package de.visionera.androidbuch.amando5.kontakt;

import android.location.Location;
import de.visionera.androidbuch.amando5.common.GeoMarkierung;
import de.visionera.androidbuch.amando5.common.GpsData;
import de.visionera.androidbuch.amando5.receiver.SmsBroadcastReceiver;

/**
 * Ein Kontakt mit Geoinformationen.
 * 
 * @author David Müller, 2010 visionera GmbH
 */
public class GeoKontakt {

  /**
   * Allgemeiner Schlüssel zum Speichern von Mobilnummern
   * in Schlüssel-Wert-Paaren.
   */
  public static final String KEY_MOBILNUMMER = 
    "mobilnummer";
  
  /** Name des Besitzers der Mobilnummer. */
	public String name;
	
	/**
   * Die Mobilnummer des androidkompatiblen Geräts, unter
   * der der Kontakt erreichbar ist.
   */
  public String mobilnummer;
	
	/** id der DB Tabelle in der Amando Datenbank. */
	public long id;
	
	/**
   * lookup key referenziert den Kontakt im
   * Android-Adressbuch.
   */
  public String lookupKey;

  /** Letzte bekannte Position. */
  public GeoMarkierung letztePosition;
  

  /**
   * Zeigt an, ob der GeoKontakt bereits gespeichert wurde.
   * 
   * @return true, wenn Kontakt in Datenbank vorhanden.
   */
  public boolean istNeu() {
    return id == 0;
  }

  /**
   * Intialisiert einen neuen Geokontakt.
   */
  public void startWerteSetzen() {
    // Neuen Kontakt mit Anfangsdaten versehen
    final Location location = 
        new Location(SmsBroadcastReceiver.LOCATION_PROVIDER);
    location.setLongitude(0);
    location.setLatitude(0);
    location.setAltitude(0);
    location.setTime(System.currentTimeMillis());
    GpsData gpsData = new GpsData(location);
    
    final GeoMarkierung anfangsPosition =
        new GeoMarkierung("", gpsData);
    this.letztePosition = anfangsPosition;
  }
}
