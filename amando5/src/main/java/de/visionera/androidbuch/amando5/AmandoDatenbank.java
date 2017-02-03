/**
 * 
 */
package de.visionera.androidbuch.amando5;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import de.visionera.androidbuch.amando5.db.GeoKontaktTbl;
import de.visionera.androidbuch.amando5.db.SpieldatenGenerator;
import de.visionera.androidbuch.amando5.fotos.db.PositionsFotoTbl;


/**
 * Die Klasse dient als logische Verbindung zur Datenbank
 * der Anwendung. <br>
 * Sie ist für die Erstellung und Wartung des
 * Datenbankschemas sowie die Initialbefüllung der
 * Tabellen verantwortlich.
 */
public class AmandoDatenbank extends SQLiteOpenHelper {
  
  /** Markierung für Logging. */
  private static final String TAG = "AmandoDatenbank";

  /** Name der Datenbankdatei. */
  private static final String DATENBANK_NAME = "amando.db";

  /**
   * Version des Schemas.
   * <ul>
   * <li>1 : Initiales Schema
   * <li>2 : VORNAME raus. LOOKUP_KEY neu
   * <li>3 : neue Tabelle FOTOS
   * <li>4 : neue Spalte FOTOS.stichwort_pos
   * </ul>
   */
  private static final int DATENBANK_VERSION = 4;

  private static AmandoDatenbank sINSTANCE;

  private static Object sLOCK = "";
  
  
  /**
   * Die Datenbank kann nur nach Kenntnis "ihrer" Anwendung
   * verwaltet werden. Daher muss der Context der Anwendung
   * übergeben werden.
   * 
   * @param context
   *          Context der aufrufenden Anwendung.
   * @return Das <i>eine</i> Exemplar der Amando-Datenbank,
   *    das in der Anwendung verwendet werden darf.
   */
  public static AmandoDatenbank getInstance(Context context) {
    if( sINSTANCE == null ) {
      synchronized(sLOCK) {
        if( sINSTANCE == null ) {
          sINSTANCE = new AmandoDatenbank(context.getApplicationContext());
        }
      }
    }
    return sINSTANCE;
  }
  

  /**
   * Die Datenbank kann nur nach Kenntnis "ihrer" Anwendung
   * verwaltet werden. Daher muss der Context der Anwendung
   * übergeben werden.<br>
   * Der Constructor darf nur von getInstance aufgerufen werden, 
   * um eine Mehrfach-Instanziierung zu verhindern.
   * 
   * @param context
   *          Context der aufrufenden Anwendung.
   */
  private AmandoDatenbank(Context context) {
    super(context, DATENBANK_NAME, null, 
        DATENBANK_VERSION);    
  }

  /**
   * Wird aufgerufen, wenn das Datenbankschema neu
   * angelegt werden soll.
   * <br>
   * Es wird die Tabelle <code>GeoKontaktTbl</code>
   * angelegt.
   * <br>
   * Anschließend wird die Initialbefüllung der Datenbank
   * durchgeführt.
   * 
   * @param db Aktuelle Datenbank-Verbindung
   */
  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(GeoKontaktTbl.SQL_CREATE);
    erzeugeDummyEintraege(db, 10);
    erzeugeSimulant(db);
    db.execSQL(PositionsFotoTbl.SQL_CREATE);
  }

  /**
   * Wird aufgerufen, wenn sich die Version des Schemas
   * geändert hat.
   * <br>
   * In diesem Fall wird die Datenbank gelöscht und mit
   * neuem Schema wieder aufgebaut.
   * 
   * 
   * @param db Aktuelle Datenbank-Verbindung
   * @param oldVersion bisherige Schemaversion
   * @param newVersion neue Schemaversion
   */
  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion,
      int newVersion) {
    Log.w(TAG, "Upgrading database from version "
        + oldVersion + " to " + newVersion
        + ", which will destroy all old data");
    db.execSQL(GeoKontaktTbl.SQL_DROP);
    db.execSQL(PositionsFotoTbl.SQL_DROP);
    onCreate(db);
  }
    
  /**
   * Hier werden die Daten eingelesen. In diesem Fall werden
   * die Datensätze zufällig erzeugt. 
   * <br><strong>Die Methode sollte bald überflüssig werden
   * </strong>
   * 
   * @param db Aktuelle Datenbank-Verbindung.
   * @param anzahlExemplare Anzahl Testdatensätze.
   */
  private void erzeugeDummyEintraege(SQLiteDatabase db,
      int anzahlExemplare) {    
    Log.d(TAG, "erzeugeDummyEintraege(): start");

    // loesche vorhandene daten
    db.delete(GeoKontaktTbl.TABLE_NAME, null, null);

    // init testdata
    final long t0 = System.currentTimeMillis();

    final SQLiteStatement stmtInsertKontakt = 
      db.compileStatement(
          GeoKontaktTbl.STMT_KONTAKT_INSERT);
    db.beginTransaction();

    try {
      for (int r = 0; r < anzahlExemplare; r++) {
        stmtInsertKontakt.bindString(1, SpieldatenGenerator
            .erzeugeName(r));
        stmtInsertKontakt.bindString(2, SpieldatenGenerator
            .erzeugeMobilnummer());
        stmtInsertKontakt.executeInsert();
      }
      db.setTransactionSuccessful();
      // CHECKSTYLE:OFF - methode deprecated
    } catch (Throwable ex) {
      // CHECKSTYLE:ON
      Log.e(TAG,
          "Fehler beim Einfügen eines Testdatensatzes. "
              + ex);
    } finally {
      db.endTransaction();
    }
    Log.w(TAG, "Importdauer Testdaten [ms] "
        + (System.currentTimeMillis() - t0));
  }

  /**
   * Erzeugt einen GeoKontakt in der DB. Der Kontakt dient
   * dazu, eine eingehende SMS zu simulieren, damit man die
   * Anwendung auf einem Gerät testen kann. Dazu ist ein
   * spezieller Testanwender nötig.
   * 
   * @param db
   */
  private void erzeugeSimulant(SQLiteDatabase db) {
    final SQLiteStatement stmtInsertKontakt = db
        .compileStatement(GeoKontaktTbl.STMT_KONTAKT_INSERT);
    db.beginTransaction();

    try {      
        stmtInsertKontakt.bindString(1, 
            SpieldatenGenerator.SIMULANT_NAME);
        stmtInsertKontakt.bindString(2, 
            SpieldatenGenerator.SIMULANT_MOBILNR);
        stmtInsertKontakt.executeInsert();
      
      db.setTransactionSuccessful();
    } catch (Throwable ex) {
      Log.e(TAG,
          "Fehler beim Einfügen eines Testdatensatzes. "
              + ex);
    } finally {
      db.endTransaction();
    }    
  }
  
}
