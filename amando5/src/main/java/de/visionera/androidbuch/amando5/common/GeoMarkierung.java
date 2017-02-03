package de.visionera.androidbuch.amando5.common;


/**
 * Klasse zum Speichern einer Geoposition eines Bekannten.
 * Wird zur Anzeige in der Karte gebraucht, daher enthält 
 * sie das Stichwort.
 * 
 * @author Arno Becker, 2010 visionera GmbH
 * 
 */
public class GeoMarkierung {
  
  /** Schlüssel für den Zugriff auf das Stichwort. */
  public static final String KEY_STICHWORT = "stichwort";
  
	/** Stichwort zu dieser Position. */
	public String stichwort;	
	
	/** Letzte bekannte Position des Bekannten. */
	public GpsData gpsData;
	
	/** 
	 * Konstruktor.
	 * 
	 * @param stichwort Stichwort, z.B. 'Kneipe'.
	 * @param gpsData Letzte bekannte Position.
	 */
	public GeoMarkierung(final String stichwort, 
	    final GpsData gpsData) {
		this.stichwort = stichwort;		
		this.gpsData = gpsData;
	}

  @Override
  public String toString() {
    return "GeoMarkierung [gpsData=" + gpsData.toString()
        + ", stichwort=" + stichwort + "]";
  }
}
