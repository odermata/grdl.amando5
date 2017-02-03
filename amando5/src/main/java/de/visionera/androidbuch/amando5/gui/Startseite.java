package de.visionera.androidbuch.amando5.gui;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import de.visionera.androidbuch.amando5.R;
import de.visionera.androidbuch.amando5.common.GeoMarkierung;
import de.visionera.androidbuch.amando5.common.GpsData;
import de.visionera.androidbuch.amando5.db.SpieldatenGenerator;
import de.visionera.androidbuch.amando5.kontakt.GeoKontakt;
import de.visionera.androidbuch.amando5.kontakt.GeoKontaktSpeicher;
import de.visionera.androidbuch.amando5.receiver.SmsBroadcastReceiver;
import de.visionera.androidbuch.amando5.services.GeoPositionsService;

/**
 * Stellt das Layout der Startseite bereit. Testen der
 * Anwendung im Emulator: Der Emulator kann sich nicht
 * selber eine SMS schicken. Ebenso hat er nach dem Start
 * keine GeoPosition. Wenn man "Position senden" testen
 * möchte, wird jedoch erst eine SMS verschickt, wenn das
 * GPS-Modul des Emulators eine aktuelle Position liefert.
 * Daher ist zum Testen der Anwendung folgendes Eingreifen
 * von außen nötig: Position senden: Wenn man eine Position
 * an einen GeoKontakt sendet, passiert erst mal nichts. Da
 * die eigene Position gesendet wird, muss der Emulator
 * diese erst erhalten. Daher: - Konsole starten - mit
 * 'telnet localhost 5554' Verbindung zum Emulator
 * herstellen - z.B. 'geo fix 7.11 50.6' eingeben
 * SMS-Empfang simulieren Um den Empfang einer SMS mit
 * Geodaten zu simulieren, öffnet man die DDMS und gibt z.B.
 * folgenden SMS-Text ins Freifeld ein (ohne
 * Zeilenumbruch!): amandoSmsKey#Tolle
 * Kneipe#7.1152637#50.7066272 #0.0#1264464001000#1 Als
 * 'Incoming number' kann man eine beliebige Mobil- nummer
 * eingeben.
 * 
 * @author David Müller, Arno Becker, 2015 visionera GmbH
 */
public class Startseite extends Activity {

  /** Tag für die LogCat. */
  public static final String TAG = Startseite.class
      .getSimpleName();
  
  /**
   * Schlüssel gibt an, von wo die Hilfeseite aufgerufen
   * wurde.
   */
  public static final String CONTEXTMENUE_HILFE = 
    "startseiteContextMenue";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate(): startseite Anzeigen ");
    
    setContentView(R.layout.startseite);  
    setTitle(R.string.startseite_titel);
    
    registerForContextMenu(findViewById(
        R.id.sf_starte_geokontakte));
    
    Log.d(TAG, "onCreate(): PID: " + Process.myPid());
    Log.d(TAG, "onCreate(): TID: " + Process.myTid());
    Log.d(TAG, "onCreate(): UID: " + Process.myUid());

    // starte Service zum Ermitteln der eigenen Position:
    startService(new Intent(this,
        GeoPositionsService.class));    
    
    // Alternativ: Remote Service starten:
    //this.startService(new Intent(this,
    //    GeoPositionsServiceRemoteImpl.class));    

    // Normaler Start-intent oder per Notification?
    intentAuswerten(getIntent());
    
    final Intent i = getIntent();
    int notificationNr = i.getIntExtra(
        SmsBroadcastReceiver.KEY_NOTIFICATION_NR, 0);

    final NotificationManager nm = 
      (NotificationManager) getSystemService(
          Context.NOTIFICATION_SERVICE);
    nm.cancel(notificationNr);  
  }

  @Override
  protected void onResume() {
    final boolean positionNachverfolgen = 
        GeoPositionsService.istPositionNachverfolgenAktiviert();
    Button button = 
        (Button)findViewById(R.id.sf_position_senden);
    if (positionNachverfolgen) {
        button.setText(R.string.sf_position_senden_beenden);
        button.setTextColor(getResources()
            .getColor(android.R.color.holo_blue_bright));
    }

    super.onResume();
  }

  /**
   * Wertet den Intent aus, mit dem die Activity gestartet
   * wurde. 1. Fall: Anwendung wurde gestartet: Es passiert
   * nichts 2. Fall: SmsReceiver hat Ortsposition-SMS
   * erhalten und eine Notification gesendet. Anwender
   * klickte die Notification an und die Activity wurde über
   * den darin enthaltenen Intent gestartet. Dann enthält
   * der Intent die Client-Position, das Stichwort und die
   * Mobilnummer. Der passende Geokontakt wird aus der
   * Datenbank geladen. Existiert keiner, so wird ein neuer
   * erzeugt und die KarteAnzeigen-Activity mit der
   * GeoKontakt-Id aufgerufen.
   * 
   * @param intent
   *          Kann die Clientdaten enthalten (s.o.)
   */
  private void intentAuswerten(final Intent intent) {
    Log.d(TAG, "intentAuswerten(): entered...");
    
    // Schlüssel zum Löschen der richtigen Notification:
    final int notificationNr =
        intent.getIntExtra(SmsBroadcastReceiver
        .KEY_NOTIFICATION_NR, 0);

    // Notification löschen:
    if (notificationNr != 0) {
      final NotificationManager nm =
          (NotificationManager) getSystemService(Context
          .NOTIFICATION_SERVICE);
      nm.cancel(notificationNr);
    }

    final Bundle extras = intent.getBundleExtra(
        SmsBroadcastReceiver.KEY_CLIENT_INFO);
    if (extras != null) {
      Log.d(TAG, "intentAuswerten(): Daten-SMS empfangen." +
      		" Startseite wurde per Intent aus " +
      		"SmsBroadcastReceiver heraus aufgerufen");
      final Location location = 
          (Location)extras.getParcelable(GpsData.KEY_LOCATION); 
      GpsData gpsData = new GpsData(location);

      final String mobilnummer =
          extras.getString(GeoKontakt.KEY_MOBILNUMMER);
      
      final GeoMarkierung geoMarkierung = 
        new GeoMarkierung(
            extras.getString(GeoMarkierung.KEY_STICHWORT),
            gpsData);    

      GeoKontaktSpeicher kontaktSpeicher =
          new GeoKontaktSpeicher(this);
      final Cursor cKontakte =
        kontaktSpeicher
          .ladeGeoKontaktDetails(mobilnummer);
      long geoKontaktId = 0;
      GeoKontakt geoKontakt = null;
      if (cKontakte.moveToFirst()) { // gefunden!
        Log.d(TAG, "intentAuswerten(): vorhandenen" +
        		" Kontakt gefunden! Aktualisieren...");
        // lade die GeoKontakt-Daten über den Cursor
        geoKontakt = 
          kontaktSpeicher.ladeGeoKontakt(cKontakte);
        geoKontakt.letztePosition = geoMarkierung;
        // aktualisieren die Position in der DB
        geoKontaktId =
          kontaktSpeicher.speichereGeoKontakt(geoKontakt);
      } else {
        Log.d(TAG, "intentAuswerten(): Kontakt noch " +
            "nicht vorhanden. Anlegen...");
        geoKontakt = new GeoKontakt();
        geoKontakt.name = mobilnummer;
        geoKontakt.mobilnummer = mobilnummer;
        geoKontakt.letztePosition = geoMarkierung;
        // Neuen Kontakt anlegen
        geoKontaktId =
          kontaktSpeicher.speichereGeoKontakt(geoKontakt);
      }
      kontaktSpeicher.schliessen();

      final boolean positionNachverfolgen =
          extras.getBoolean(
              SmsBroadcastReceiver.KEY_NACHVERFOLGEN, false);

      Log.d(TAG, "intentAuswerten(): KarteAnzeigen aufrufen");
      Log.d(TAG, "intentAuswerten(): geoKontaktId: " + geoKontaktId);
      Log.d(TAG, "intentAuswerten(): positionNachverfolgen: " + positionNachverfolgen);
      final Intent activityIntent = new Intent(this,
          KarteAnzeigen.class);
      activityIntent.putExtra(
          KarteAnzeigen.IN_PARAM_KONTAKT_ID, geoKontaktId);
      activityIntent.putExtra(
          SmsBroadcastReceiver.KEY_NACHVERFOLGEN,
          positionNachverfolgen);
      startActivity(activityIntent);
    }
  }
  
  @Override
  protected void onSaveInstanceState(Bundle outState) {
    Log.d(TAG, "onSaveInstanceState(): aufgerufen...");
    super.onSaveInstanceState(outState);
  }

  /**
   * Wird bei Klick auf Schaltflaeche 'Position senden' 
   * aufgerufen.
   *
   * @param sfNormal Schaltfläche
   */
  public void onClickPositionSenden(final View sfNormal) {
    Log.d(TAG, "onClickPositionSenden(): aufgerufen...");
    final boolean positionNachverfolgen = 
        GeoPositionsService.istPositionNachverfolgenAktiviert();
    Button button = 
        (Button)findViewById(R.id.sf_position_senden);
    if (positionNachverfolgen) {
      button.setText(R.string.sf_position_senden);
      button.setTextColor(getResources()
          .getColor(android.R.color.black));      
      GeoPositionsService.stoppeNachverfolgung();
    } else {
      final Intent i = 
          new Intent(this, PositionSenden.class);
      startActivity(i);
    }  
  }

  /**
   * Wird bei Klick auf Schaltflaeche 'Geokontakte' 
   * aufgerufen.
   * 
   * @param sfNormal Schaltfläche
   */
  public void onClickGeokontakteVerwalten(
      final View sfNormal) {
    // Mock: statische Werte
    // final Intent i = new Intent(this, 
    //     GeoKontakteAuflistenMock.class);
    
    // Klassisch: Speicherintensiv
    final Intent i = new Intent(this, 
        GeoKontakteAuflisten.class);
    
    startActivity(i);       
  }

  /**
   * Wird bei Klick auf Schaltflaeche 'Karte
   * anzeigen' aufgerufen.
   * 
   * @param sfNormal Schaltfläche
   */
  public void onClickKarteAnzeigen(final View sfNormal) {
    final Intent intent = new Intent(this, KarteAnzeigen.class);
    startActivity(intent);
  }

  /**
   * Wird bei Klick auf Schaltflaeche 'Simulation
   * starten' aufgerufen.
   *
   * @param sfNormal Schaltfläche
   */
  public void onClickSimulationStarten(
      final View sfNormal) {
    Log.d(TAG, "onClickSimulationStarten(): entered...");
    final Location location = 
        new Location(SmsBroadcastReceiver.LOCATION_PROVIDER);
    location.setLongitude(7.1152637);
    location.setLatitude(50.7066272);
    location.setAltitude(66.1);
    location.setTime(System.currentTimeMillis());
    final GpsData gpsData = new GpsData(location);

    final GeoMarkierung geoMarkierung = new GeoMarkierung(
        "SIMULATION", gpsData);
    
    final String mobilnummer = 
      SpieldatenGenerator.SIMULANT_MOBILNR;

    GeoKontaktSpeicher kontaktSpeicher =
      new GeoKontaktSpeicher(this);
    
    final Cursor cursor =
      kontaktSpeicher.ladeGeoKontaktDetails(mobilnummer);
    long geoKontaktId = 0;
    GeoKontakt geoKontakt = null;
    if (cursor.moveToFirst()) { // gefunden!
      // lade die GeoKontakt-Daten über den Cursor
      geoKontakt = kontaktSpeicher.ladeGeoKontakt(cursor);
      Log.d(TAG,
          "onClickSimulationStarten(): " +
              "Mobilnummer des Simulanten: " +
              geoKontakt.mobilnummer);
      Log.d(TAG,
          "onClickSimulationStarten(): " +
          "Name des Simulanten: " +
          geoKontakt.name);
      geoKontakt.letztePosition = geoMarkierung;
      // aktualisieren die Position in der DB
      geoKontaktId =
        kontaktSpeicher.speichereGeoKontakt(geoKontakt);
    } else {
      Log.e(TAG,
          "Simulations-Geokontakt wurde zu Programmstart" +
          " nicht angelegt!");
      return;
    }
    kontaktSpeicher.schliessen();

    final boolean positionNachverfolgen = true;
    final boolean simulationsModus = true;

    final Intent activityIntent = new Intent(this,
        KarteAnzeigen.class);
    activityIntent.putExtra(
        KarteAnzeigen.IN_PARAM_KONTAKT_ID,
        geoKontaktId);
    activityIntent.putExtra(
        SmsBroadcastReceiver.KEY_NACHVERFOLGEN,
        positionNachverfolgen);
    activityIntent.putExtra(
        KarteAnzeigen.IN_PARAM_SIMULATIONSMODUS,
        simulationsModus);
    startActivity(activityIntent);
  }
    
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.hauptmenue, menu);
    return super.onCreateOptionsMenu(menu);
  }
  
  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    final MenuItem optHilfe = 
        (MenuItem)menu.findItem(R.id.opt_hilfe);
    // so könnte man einzelne Menüeinträge einer Activity
    // abschalten:
    // optHilfe.setEnabled(false);
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.opt_einstellungenBearbeiten:        
        startActivity(new Intent(this,
            EinstellungenBearbeiten.class));
        return true;    
      case R.id.opt_hilfe: 
        startActivity(new Intent(this,
            HilfeAnzeigen.class));
        return true;    
      case R.id.opt_amandoBeenden: 
        finish();
        return true;      
      default:
        return super.onOptionsItemSelected(item);        
    }    
  }
    
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    if (v.getId() == R.id.sf_starte_geokontakte) {
      getMenuInflater().inflate(
          R.menu.startseite_contextmenue, menu);
    }
    super.onCreateContextMenu(menu, v, menuInfo);
  }
 
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    if (item.getItemId() ==
        R.id.opt_geokontakt_verwalten_hilfe) {  
      final Intent intent = new Intent(this,
          HilfeAnzeigen.class);
      startActivity(intent);
      return true;
    }
    return super.onContextItemSelected(item);   
  }

  @Override
  protected void onDestroy() {
    Log.d(TAG,
        "onDestroy(): entered...");
    // Local Service stoppen:
    stopService(new Intent(this,
        GeoPositionsService.class));   
        
    // Alternativ: Remote service stoppen:
    //stopService(new Intent(this,
    //   GeoPositionsServiceRemoteImpl.class));   
    
    super.onDestroy();
  }
}