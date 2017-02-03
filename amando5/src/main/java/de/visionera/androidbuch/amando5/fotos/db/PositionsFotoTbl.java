/**
 * 
 */
package de.visionera.androidbuch.amando5.fotos.db;

/**
 * Schnittstelle zur Tabelle FOTOS. <br>
 * Die Klasse liefert
 * <ul>
 * <li>SQL-Code zur Erzeugung der Tabelle
 * <li>SQL-Code für alle für Amando erforderlichen
 * Statements
 * </ul>
 */
public final class PositionsFotoTbl implements
    PositionsFotoColumns {
  /**
   * Name der Datenbanktabelle.
   */
  public static final String TABLE_NAME = "fotos";

  /**
   * SQL Anweisung zur Schemadefinition.
   */
  public static final String SQL_CREATE =
      "CREATE TABLE fotos (" +
      ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
      TITEL + " TEXT NOT NULL," +
      STICHWORT_POS + " TEXT NOT NULL," +
      DATEINAME + " TEXT NOT NULL," +
      ZEITSTEMPEL + " INTEGER NOT NULL" +
      ");";

  /** 
   * Standard-Sortierreihenfolge für die Tabelle.
   * <br>
   * Sortiert wird nach Positions-Stichwort absteigend.
   */
  public static final String DEFAULT_SORT_ORDER = 
    STICHWORT_POS + 
    " ASC";
  
  /**
   * SQL Anweisung zur L&ouml;schung der Tabelle.
   */
  public static final String SQL_DROP =
      "DROP TABLE IF EXISTS " +
      TABLE_NAME;

  /**
   * SQL f&uuml;r Speicherung eines Positionsfotos. Es
   * werden alle Attribute als Pflichtfelder benötigt.
   */
  public static final String STMT_INSERT_FULL =
      "INSERT INTO fotos " +
      "(" + ID + "," + TITEL + "," + DATEINAME + "," +
      ZEITSTEMPEL + ") " +
      "VALUES (?,?,?,?)";

  /**
   * SQL-Anweisung zur L&ouml;schung aller Fotos.
   */
  public static final String STMT_DELETE_ALL =
      "DELETE fotos ";

  /**
   * SQL-Anweisung zur L&ouml;schung eines Fotos anhand
   * seines Schl&uuml;sselwerts.
   */
  public static final String STMT_DELETE_BY_ID =
      "DELETE fotos " +
      "WHERE " + ID + " = ?";

  /**
   * SQL-Bedingung für Anfrage nach Schlüsselwert.
   */
  public static final String WHERE_ID_EQUALS =
      ID + "= ?";

  /**
   * Klasse enthält nur Konstanten. Daher keine
   * Objekterzeugung vorgesehen.
   */
  private PositionsFotoTbl() {
  }
}
