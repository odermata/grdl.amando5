/**
 * 
 */
package de.visionera.androidbuch.amando5.fotos.db;

/**
 * Spalten der Tabelle FOTOS. <br>
 * 
 * @author pant
 */
public interface PositionsFotoColumns {
  /** Primärschlüssel. */
  String ID = "_id";

  /**
   * Titel := Eindeutiger Titel des Bildes.
   * <br>
   * Pflichtfeld
   * <br>
   * TEXT
   */
  String TITEL = "titel";
  
  /** 
   * Stichwort der Geomarkierung der Position. 
   * Für jedes Stichwort können mehrere Fotos gespeichert
   * werden.
   * <br>
   * Das Stichwort <em>kann</em>, muss aber nicht mit 
   * einem Positionsstichwort eines Geokontakts
   * übereinstimmen.
   * <br>
   * Pflichtfeld
   * <br>
   * TEXT
   */
  String STICHWORT_POS = "stichwortpos";
  

  /**
   * Name der Bilddatei. Wird i.A. vom System vergeben.
   * <br>
   * Pflichtfeld
   * <br>
   * TEXT
   */
  String DATEINAME = "_data";

  /** 
   * Zeitpunkt der letzten Positionsmeldung. 
   * <br>
   * Pflichtfeld
   * <br>
   * INTEGER
   */
  String ZEITSTEMPEL = "zeitstempel";

}
