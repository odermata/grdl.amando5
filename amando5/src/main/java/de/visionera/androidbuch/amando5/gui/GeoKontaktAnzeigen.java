package de.visionera.androidbuch.amando5.gui;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import de.visionera.androidbuch.amando5.R;
import de.visionera.androidbuch.amando5.db.GeoKontaktTbl;
import de.visionera.androidbuch.amando5.kontakt.GeoKontaktSpeicher;

/**
 * Zeigt die Einzelheiten eines Geokontakts an.
 * 
 * @author David Müller, 2010 visionera GmbH
 */
public class GeoKontaktAnzeigen extends Activity {

  /** Kuerzel fuers Logging. */
  private static final String TAG = GeoKontaktAnzeigen.class
      .getSimpleName();

  static final String IN_PARAM_KONTAKT_ID = "KONTAKT_ID";

  /** Schnittstelle zur persistenten Speicher. */
  private GeoKontaktSpeicher mKontaktSpeicher;

  /** Die DB Id des ausgewählten Kontaktes. */
  private long mGeoKontaktId;

  @Override
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    // getWindow().setBackgroundDrawableResource(R.drawable.hintergrund);

    setContentView(R.layout.geokontakt_anzeigen);
    final Bundle extras = getIntent().getExtras();
    if (extras != null &&
        extras.containsKey(IN_PARAM_KONTAKT_ID)) {
      mGeoKontaktId = extras.getLong(IN_PARAM_KONTAKT_ID);
      Log.d(TAG, "Aufruf mit Kontakt id " + mGeoKontaktId);
    } else {
      Log.w(TAG, "Keine GeoKontakt Id übergeben");
    }
    mKontaktSpeicher = new GeoKontaktSpeicher(this);
  }

  @Override
  protected void onStart() {
//    mKontaktSpeicher.oeffnen();
    
    zeigeDetails();

    super.onStart();
  }

  
  
  /* (non-Javadoc)
   * @see android.app.Activity#onStop()
   */
  @Override
  protected void onDestroy() {
    mKontaktSpeicher.schliessen();
    
    super.onDestroy();
  }

  /**
   * Befüllt die Views der Activity mit den Daten des
   * GeoKontakts aus der Datenbank.
   */
  private void zeigeDetails() {
    if (mGeoKontaktId == 0) {
      return;
    }
    // "schnelle" anfrage, daher kein async ueber
    // Loader noetig.
    final Cursor kontaktCursor =
        mKontaktSpeicher
        .ladeGeoKontaktDetails(mGeoKontaktId);
    if (!kontaktCursor.moveToFirst()) {
      Log.i(TAG, "Kontakt nicht gefunden. Id " +
          mGeoKontaktId);
      return;
    }
    Log.d(TAG, "Kontakt geladen " +
        kontaktCursor.getString(1));

    final TextView fldName = (TextView)
        findViewById(R.id.tx_name);
    fldName.setText(kontaktCursor.getString(kontaktCursor
        .getColumnIndex(GeoKontaktTbl.NAME)));

    final TextView fldTelefon = (TextView)
        findViewById(R.id.tx_telefon);
    fldTelefon.setText(kontaktCursor
        .getString(kontaktCursor
        .getColumnIndex(GeoKontaktTbl.MOBILNUMMER)));

    final TextView fldStichwort = (TextView)
      findViewById(R.id.tx_stichwort);
    fldStichwort.setText(kontaktCursor
        .getString(kontaktCursor
        .getColumnIndex(GeoKontaktTbl.STICHWORT_POS)));

    final long zeitstempel = kontaktCursor.getLong(
        kontaktCursor.getColumnIndex(
            GeoKontaktTbl.ZEITSTEMPEL));
    final TextView fldDatum = (TextView)
      findViewById(R.id.tx_datum);
    if (zeitstempel > 0) {
      fldDatum.setText(DateFormat.getDateTimeInstance(
        DateFormat.LONG, DateFormat.MEDIUM).
          format(new Date(zeitstempel))); 
    } else {
      fldDatum.setText("unbekannt");
    }
    final double breitengrad = kontaktCursor.getDouble(
      kontaktCursor
      .getColumnIndex(GeoKontaktTbl.BREITENGRAD));
    final double laengengrad = kontaktCursor.getDouble(
      kontaktCursor
      .getColumnIndex(GeoKontaktTbl.LAENGENGRAD));
    Log.i(TAG, "Laenge: " + laengengrad + ", Breite: " +
        breitengrad);
    final TextView fldPosition = (TextView) findViewById(
        R.id.tx_letzte_position);
    if (breitengrad > 0 && laengengrad > 0) {
      fldPosition.setText(MessageFormat.format(
          "{0}'' Länge, {1}'' Breite", laengengrad, 
          breitengrad));
    } else {
      fldPosition.setText("");
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.geokontakt_anzeigen,
        menu);

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.opt_geokontakt_bearbeiten:
        final Intent i = new Intent(this,
            GeoKontaktBearbeiten.class);
        i.putExtra(
            GeoKontaktBearbeiten.IN_PARAM_KONTAKT_ID,
            mGeoKontaktId);
        startActivity(i);
        return true;   
        
      case R.id.opt_geokontakt_loeschen:
        mKontaktSpeicher.loescheGeoKontakt(mGeoKontaktId);
        finish();
        return true;
        
      case R.id.opt_hilfe:
        final Intent j = new Intent(this,
            HilfeAnzeigen.class);
        startActivity(j);
        return true;

      default:
        Log.w(TAG, "unbekannte Option gewaehlt: " + item);
    }
    return super.onOptionsItemSelected(item);
  }

}
