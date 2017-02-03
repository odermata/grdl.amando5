/**
 * 
 */
package de.visionera.androidbuch.amando5.db;

/**
 * Schnittstelle zur Tabelle GEOKONTAKTE. <br>
 * Die Klasse liefert
 * <ul>
 * <li>SQL-Code zur Erzeugung der Tabelle
 * <li>SQL-Code für alle für Amando erforderlichen
 * Statements
 * </ul>
 * 
 * @author pant
 */
public final class GeoKontaktTbl implements
    GeoKontaktColumns {
  /**
   * Name der Datenbanktabelle.
   */
  public static final String TABLE_NAME = "geokontakte";

  /**
   * SQL Anweisung zur Schemadefinition.
   */
  public static final String SQL_CREATE =
      "CREATE TABLE geokontakte (" +
      "_id   INTEGER PRIMARY KEY AUTOINCREMENT," +
      "name TEXT NOT NULL," +
      "lookup_key TEXT," +
      "mobilnummer TEXT," +
      "stichwortpos TEXT," +
      "laengengrad REAL, " +
      "breitengrad REAL, " +
      "hoehe REAL, " +
      "zeitstempel INTEGER " +
      ");";
  
  /** 
   * Standard-Sortierreihenfolge für die Tabelle.
   * <br>
   * Sortiert wird nach Zeitstempel absteigend.
   */
  public static final String DEFAULT_SORT_ORDER = 
    ZEITSTEMPEL + 
    " DESC";

  /**
   * SQL Anweisung zur L&ouml;schung der Tabelle.
   */
  public static final String SQL_DROP =
      "DROP TABLE IF EXISTS " +
      TABLE_NAME;

  /**
   * SQL Anweisung f&uuml;r Erzeugung eines
   * Minimal-Geokontakts aus Name.
   */
  public static final String STMT_MIN_INSERT =
      "INSERT INTO geokontakte " +
      "(name) " +
      "VALUES (?)";

  /**
   * SQL Anweisung f&uuml;r Erzeugung eines Geokontakts
   * aus den Stammdaten Name, Mobilnummer.
   */
  public static final String STMT_KONTAKT_INSERT =
      "INSERT INTO geokontakte " +
      "(name,mobilnummer) " +
      "VALUES (?,?)";

  /**
   * SQL-Anweisung zur L&ouml;schung aller Geokontakte.
   */
  public static final String STMT_KONTAKT_DELETE =
    "DELETE geokontakte ";
  
  /**
   * SQL-Anweisung zur L&ouml;schung eines Geokontakts
   * anhand seines Schl&uuml;sselwerts.
   */
  public static final String STMT_KONTAKT_DELETE_BY_ID =
    "DELETE geokontakte " +
    "WHERE _id = ?";

  /** Liste aller bekannten Attribute. */
  public static final String[] ALL_COLUMNS = new String[] {
      ID,
      NAME,
      LOOKUP_KEY,
      MOBILNUMMER,
      STICHWORT_POS,
      LAENGENGRAD,
      BREITENGRAD,
      HOEHE,
      ZEITSTEMPEL
      };

  /**
   * WHERE-Bedingung f&uuml;r ID-Anfrage.
   */
  public static final String WHERE_ID_EQUALS =
      ID + "=?";

  /**
   * Klasse enthält nur Konstanten.
   * Daher keine Objekterzeugung vorgesehen.
   */
  private GeoKontaktTbl() {
  }
}
