package de.visionera.androidbuch.amando5.gui;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import de.visionera.androidbuch.amando5.R;
import de.visionera.androidbuch.amando5.common.GeoMarkierung;
import de.visionera.androidbuch.amando5.kontakt.GeoKontakt;
import de.visionera.androidbuch.amando5.kontakt.GeoKontaktSpeicher;

/**
 * Erlaubt die Bearbeitung eines Geokontakts.
 * 
 * @author David Müller, 2010 visionera GmbH
 */
public class GeoKontaktBearbeiten extends Activity {

  /** Kuerzel fuers Logging. */
  private static final String TAG = 
    GeoKontaktBearbeiten.class.getSimpleName();

  /**
   * Schlüssel des zu bearbeitenden Kontakts, der per Intent
   * über geben wird.
   */
  static final String IN_PARAM_KONTAKT_ID = "KONTAKT_ID";

  /** Schnittstelle zur persistenten Speicher. */
  private GeoKontaktSpeicher mKontaktSpeicher;

  /** Die DB Id des ausgewählten Kontaktes. */
  private long mGeoKontaktId;
  /** Der zu bearbeitende Kontakt. */
  private GeoKontakt mGeoKontakt;

  @Override
  protected void onCreate(final Bundle icicle) {
    super.onCreate(icicle);
    // getWindow().setBackgroundDrawableResource(R.drawable.hintergrund);

    setContentView(R.layout.geokontakt_bearbeiten);
    
    final Bundle extras = getIntent().getExtras();
    if (extras != null &&
        extras.containsKey(IN_PARAM_KONTAKT_ID)) {
      mGeoKontaktId = extras.getLong(IN_PARAM_KONTAKT_ID);
      Log.d(TAG, "Aufruf mit Kontakt id " + mGeoKontaktId);
    }

    mKontaktSpeicher = new GeoKontaktSpeicher(this);
  }

  @Override
  protected void onStart() {

    if (mGeoKontaktId != 0) {
      ladeKontakt();
    } else {
      // neuen Kontakt erzeugen
      mGeoKontakt = new GeoKontakt();
      mGeoKontakt.startWerteSetzen();
    }

    zeigeDetails();

    super.onStart();
  }

  /* (non-Javadoc)
   * @see android.app.Activity#onDestroy()
   */
  @Override
  protected void onDestroy() {
    mKontaktSpeicher.schliessen();
    
    super.onDestroy();
  }

  /**
   * Stellt sicher, dass Kontaktinformationen bei
   * Inaktivierung der Activity nicht verloren gehen. <br>
   * Gespeichert werden: <li>kontaktId (wenn vorhanden) <li>
   * die Werte aller Textfelder
   * 
   * @see android.app.Activity#onPause()
   */
  @Override
  protected void onPause() {
    super.onPause();
    final SharedPreferences einstellungen = 
      getPreferences(MODE_PRIVATE);
    final Editor editor = einstellungen.edit();
    if (mGeoKontaktId != 0) {
      editor.putLong(IN_PARAM_KONTAKT_ID, mGeoKontaktId);
    }
    editor.putString("" + R.id.tx_name_edit, "" + 
        ((EditText) findViewById(R.id.tx_name_edit))
        .getText());
    editor.putString("" + R.id.tx_telefon_edit, "" +
        ((EditText) findViewById(R.id.tx_telefon_edit))
        .getText());
    editor.commit();
  }

  /** 
   * Lädt einen Kontakt mit der zuvor für diese Klasse
   * gesetzen Id.
   */
  private void ladeKontakt() {
    mGeoKontakt = mKontaktSpeicher
        .ladeGeoKontakt(mGeoKontaktId);
    Log.d(TAG, "Kontakt geladen " +
        (mGeoKontakt != null ? mGeoKontakt.name
            : mGeoKontakt));
  }

  /**
   * Aktualisiert die Activity mit den zuvor aus der 
   * Datenbank geladenen Werten eines GeoKontakts.
   */
  private void zeigeDetails() {
    final EditText fldName = (EditText)
        findViewById(R.id.tx_name_edit);
    fldName.setText(mGeoKontakt.name);

    final EditText fldTelefon = (EditText)
        findViewById(R.id.tx_telefon_edit);
    fldTelefon.setText(mGeoKontakt.mobilnummer);

    final TextView fldStichwort = (TextView)
        findViewById(R.id.tx_stichwort);
    final GeoMarkierung marke = mGeoKontakt.letztePosition;
    fldStichwort.setText(marke.stichwort);

    final long zeitstempel = marke.gpsData.getZeitstempel();
    final TextView fldDatum = (TextView)
        findViewById(R.id.tx_datum);
    if (zeitstempel > 0) {
      fldDatum.setText(DateFormat.getDateTimeInstance(
          DateFormat.LONG, DateFormat.MEDIUM).
          format(new Date(zeitstempel)));
    } else {
      fldDatum.setText("unbekannt");
    }
    final double breitengrad = marke.gpsData.getBreitengrad();
    final double laengengrad = marke.gpsData.getLaengengrad();
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

  /**
   * Liest die änderbaren Felder der GUI aus und speichert
   * den GeoKontakt in der Datenbank.
   */
  private void speichereKontakt() {

    final EditText fldName = (EditText) findViewById(
        R.id.tx_name_edit);
    mGeoKontakt.name = fldName.getText().toString();

    final EditText fldTelefon = (EditText) findViewById(
        R.id.tx_telefon_edit);
    mGeoKontakt.mobilnummer = fldTelefon.getText().
        toString();

    mKontaktSpeicher.speichereGeoKontakt(mGeoKontakt);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.geokontakt_bearbeiten,
        menu);

    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.opt_speichern:

        speichereKontakt();
        finish();
        return true;

      case R.id.opt_hilfe:
        final Intent i = new Intent(this,
            HilfeAnzeigen.class);
        startActivity(i);
        return true;

      default:
        Log.w(TAG, "unbekannte Option gewaehlt: " +
            item);
    }
    return super.onOptionsItemSelected(item);
  }

  // für unit tests...
  /**
   * Hole den aktuellen GeoKontakt.
   * @return aktueller GeoKontakt
   */
  public GeoKontakt getGeoKontakt() {
    return mGeoKontakt;
  }

  // für unit tests...
  /** 
   * Setzt den GeoKontakt-Speicher.
   * @param speicher GeoKontakt-Speicher
   */
  public void setGeoKontaktSpeicher(
      final GeoKontaktSpeicher speicher) {
    mKontaktSpeicher = speicher;
  }

  // für unit tests...
  /** 
   * Liefert die aktuelle GeoKontakt-Id. 
   * @return aktuelle GeoKontakt-Id
   */
  public long getKontaktId() {
    return mGeoKontaktId;
  }

  /**
   * Setzt die Kontakt-Id des anzuzeigenden GeoKontakts.
   * @param kontaktId Kontakt-Id des anzuzeigenden 
   *   GeoKontakts
   */
  public void setKontaktId(final long kontaktId) {
    mGeoKontaktId = kontaktId;
  }
}
