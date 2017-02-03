/**
 * 
 */
package de.visionera.androidbuch.amando5.db;

/**
 * Spalten der Tabelle GEOKONTAKTE. <br>
 * 
 * @author pant
 */
public interface GeoKontaktColumns {
  /** Primärschlüssel. */
   String ID = "_id";
  /** 
   * Pflichtfeld. Name := Vorname Nachname
   * <br>
   * Pflichtfeld
   * <br>
   * TEXT
   */
   String NAME = "name";
  /** 
   * LOOKUP_KEY des dazugehörenden Eintrags 
   * aus dem Android-Telefonbuch.
   * <br>
   * TEXT
   */
   String LOOKUP_KEY = "lookup_key";
  /** 
   * Mobilnummer im Format der Android-Kontakte. 
   * <br>
   * TEXT
   */
   String MOBILNUMMER = "mobilnummer";
  /** 
   * Stichwort der Geomarkierung der letzten Position. 
   * <br>
   * TEXT
   */
   String STICHWORT_POS = "stichwortpos";
  /** 
   * L&#xe4;ngengrad der letzten Position. 
   * <br>
   * REAL
   */
   String LAENGENGRAD = "laengengrad";
  /** 
   * Breitengrad der letzten Position. 
   * <br>
   * REAL
   */
   String BREITENGRAD = "breitengrad";
  /** 
   * H&#xf6;he der letzten Position. 
   * <br>
   * REAL
   */
   String HOEHE = "hoehe";
  /** 
   * Zeitpunkt der letzten Positionsmeldung. 
   * <br>
   * INTEGER
   */
   String ZEITSTEMPEL = "zeitstempel";

}
