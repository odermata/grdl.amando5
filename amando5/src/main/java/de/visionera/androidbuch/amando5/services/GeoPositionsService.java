package de.visionera.androidbuch.amando5.services;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import java.util.List;

import de.visionera.androidbuch.amando5.common.AmandoSmsUtil;
import de.visionera.androidbuch.amando5.common.GeoMarkierung;
import de.visionera.androidbuch.amando5.common.GpsData;
import de.visionera.androidbuch.amando5.gui.KarteAnzeigen;
import de.visionera.androidbuch.amando5.kontakt.GeoPosition;
import de.visionera.androidbuch.amando5.kontakt.TelefonnummernHelfer;
//import android.location.LocationListener;

/**
 * Dieser Service hat drei fachliche Aufgaben:
 *   - Ermittlung der eigenen Position mittels Network-
 *     Provider oder GPS. 
 *   - Übertragen der eigenen Geoposition an den Amando-
 *     Server
 *   - Senden der SMS mit eigenen Geoposition an einen
 *     Freund
 *       
 * Dieser Service ist ein Beispiel für die Implementierung 
 * eines Service als Komponente für Business-Logik mit Hilfe
 * eines Binders. 
 * 
 * Bitte beachten: dieser Service hat keine langlaufenden 
 * Methoden und implementiert nicht die onStartCommand-Methode.
 * Für Datenübertragung ist der SendePositionService zuständig.
 * 
 * @author Arno Becker, 2015 visionera GmbH
 */
public class GeoPositionsService extends Service 
    implements LocationListener, ConnectionCallbacks, 
    OnConnectionFailedListener {
  
  /** Tag für die LogCat. */
  private static final String TAG = 
      GeoPositionsService.class.getSimpleName();

  /**
   * Minimaler Zeitraum zwischen zwei Abfragen der aktuellen
   * Ortsposition vom LocationManager.
   */
  private static final int MIN_ZEIT = 5000; // 5 Sekunden
  
  /**
   * Minimale Ortsveraenderung, ab der der Locationmanager
   * eine neue Ortsposition abfragt.
   */
  private static final int MIN_DISTANZ = 10; // 10 Meter
  
  private static final boolean USE_LOCATION_API_V2 = true;
  
  /** Update-Intervall des LocationClients in Millisekunden. */
  private static final long UPDATE_INTERVAL = 15000; // 5 Sekunden
         
  /** Minimales Intervall für Location Updates in Millisekunden. */
  private static final long SCHNELLSTES_INTERVAL = 5000; // 1 Sekunde

  /** Letzte bekannte eigene Geoposition. */
  private GpsData mGpsData;

  /**
   * Bei Positionsänderung die Position an den Amando-Server
   * schicken.
   */
  private static boolean mPositionNachverfolgen;

  /** Mobilnummern der SMS-Empfänger. */
  private List<String> mEmpfaengerMobilnummern;

  /**
   * Das Stichwort des Senders, welches per SMS an den
   * Empfaenger geschickt wird.
   */
  private String mStichwort;

  /** 
   * Speichert die einmal ermittelte eigene 
   * Telefonnummer. 
   */
  private String mEigeneMobilnummer;

  /**
   * Das Flag wird immer dann auf true gesetzt, wenn die
   * 'Position senden'- Schaltfläche aktiviert wurde. Dann 
   * wird einmalig die aktuelle Geoposition per SMS an den 
   * Freund geschickt. Wenn die Option "Eigene Position
   * aktualisieren" gewählt wurde, wird die eigene Position
   * fortwährend vom Location-Listener an den Amando-Server
   * geschickt.
   */
  //private boolean mSendePositionFlag;
  
  /** Binder für Zugriff auf diesen Service. */
  private final IBinder mGpsBinder = 
    new GeoPositionsServiceBinder();
  
  /**
   * Callback-Handler für Geopositions-Rückmeldungen an
   * KarteAnzeigen-Activity
   */
  private Handler mKarteAnzeigenCallbackHandler;
  
  private LocationClient mLocationClient;
  
  private LocationRequest mLocationRequest;
  
  /**
   * Klassen, die den Binder fuer den Zugriff von Clients
   * auf diesen Service definiert. Da dieser Service immer
   * im gleichen Prozess wie der Aufrufer laeuft, ist kein
   * IPC notwendig.
   */
  public class GeoPositionsServiceBinder extends Binder {
    /** 
     * Liefert die letzte bekannte eigene Position. 
     * 
     * @return Letzte bekannte eigene Position.
     */
    public GpsData getGpsData() {
      return mGpsData;
    }
  
    /**
     * Sendet die zuletzt bekannte eigene Position per SMS an
     * einen Empfänger mit der Mobilnummer 'empfaengerNr'.
     * Falls 'positionAktualisieren' true ist, wird die
     * Position zusätzlich an den Amando-Server geschickt. Der
     * Empfänger der SMS (der Client) ist nun berechtigt, die
     * Positionsänderungen des SMS-Senders nachzuverfolgen.
     * Daher werden Positionsänderungen jeweils an den
     * Amando-Server geschickt. Der
     * @see{GeoPositionsServiceImpl} implementiert einen
     * LocationListener. Dieser schickt bei Ortsveränderungen
     * die aktuelle Position an den Amando-Server, der sie an
     * den Client weiterleitet (allerdings nur, wenn
     * 'positionAktualisieren' true ist).
     * 
     * @param empfaenger
     *          Mobilnummern der Empfänger
     * @param stichwort
     *          ein Stichwort, was an die Empfänger geschickt
     *          wird
     * @param positionAktualisieren
     *          Empfänger darf Positionsänderungen des
     *          SMS-Senders nachverfolgen.
     * @return true, wenn dieser Service eine Geoposition kennt.         
     */
    public void sendeGeoPosition(
        final List<String> empfaenger,
        final String stichwort, 
        final boolean positionAktualisieren) {
      Log.d(TAG, "sendeGeoPosition(): Stichwort: " + 
          stichwort);
      
      mEmpfaengerMobilnummern = empfaenger;
      mStichwort = stichwort;
      mPositionNachverfolgen = positionAktualisieren;      
      mEigeneMobilnummer = ermittleEigeneMobilnummer();
      
      sendeGeoPositionImpl(false);
      
      Log.d(TAG, "sendeGeoPosition(): leave...");
    }
  
    /**
     * Übergibt dem Service einen Callback-Handler, um die
     * aktualisierte Position der Clients zu melden. Der
     * Handler wird in der Activity @see{KarteAnzeigen}
     * definiert und dient zum Aktualisieren der Positionen
     * der Clients. Dieser Callback wird direkt an den
     * @see{EmpfangePositionService} weitergeleitet, da 
     * dieser staendig die aktuellen Positionen der Clients
     * mit Hilfe eines Listeners registriert.
     * 
     * @param callback
     *        ein Handler-Objekt zur Aktualisierung der
     *        Oberfläche.
     */
    public void setzeActivityCallbackHandler(
        final Handler callback) {
      mKarteAnzeigenCallbackHandler = callback;
      // Weitergabe an den EmpfangePositionService. Dieser 
      // hat einen Listener, der neue Positionen von Clients 
      // empfaengt und aktualisiert selber die Oberflaeche:
      EmpfangePositionService.setzeActivityCallbackHandler(callback);      
    }
    
    /**
     * Startet den Location Manager neu und verwendet den derzeit
     * geeignetsten Provider.
     */
    public void restarteGeoProvider() {
        starteGeoProvider();
    }
  }

  @Override
  public void onCreate() {
    Log.d(TAG, "onCreate(): PID: " + Process.myPid());
    Log.d(TAG, "onCreate(): TID: " + Process.myTid());
    Log.d(TAG, "onCreate(): UID: " + Process.myUid());
    
    if (USE_LOCATION_API_V2) {
      boolean usePlayService = isGooglePlayServiceAvailable();
      if (usePlayService) {
        Log.i(TAG, "onCreate(): verwende die neue Location API mit dem Fuse-Provider");
        starteGeoProvider(); 
      } 
    } else {
      starteGeoProvider();   
    }  
    
    if (mLocationClient != null) {
      mLocationClient.connect();
    }
  }
  
  @Override
  public void onDestroy() {
    if (mLocationClient != null) {
      mLocationClient.disconnect();
    }
    super.onDestroy();
  }

  @Override
  public IBinder onBind(final Intent intent) {
    Log.d(TAG, "onBind(): entered...");
    return mGpsBinder;
  }

  @Override
  public boolean onUnbind(Intent intent) {
    Log.d(TAG, "onUnbind(): entered...");
    mKarteAnzeigenCallbackHandler = null;
    return super.onUnbind(intent);
  }
  
  private void starteGeoProvider() {
    Log.d(TAG, "starteGeoProvider(): entered...");
    if (USE_LOCATION_API_V2) {
      mLocationClient = new LocationClient(this, this, this);
      
      mLocationRequest = LocationRequest.create();
      mLocationRequest.setPriority(
                LocationRequest.PRIORITY_HIGH_ACCURACY);
      mLocationRequest.setInterval(UPDATE_INTERVAL);
      // Set the fastest update interval to 1 second
      mLocationRequest.setFastestInterval(SCHNELLSTES_INTERVAL);
    } else {
//      final LocationManager locationManager = (LocationManager) 
//          getSystemService(Context.LOCATION_SERVICE);
//      
//      // Bestmöglichen Provider starten:
//      final Criteria locationCriteria = new Criteria();
//      locationCriteria.setAccuracy(Criteria.NO_REQUIREMENT);
//      String provider = 
//          locationManager.getBestProvider(locationCriteria, true);    
//      if (LocationManager.PASSIVE_PROVIDER.equalsIgnoreCase(provider)) {
//        provider = LocationManager.NETWORK_PROVIDER;
//      }
//      locationManager.requestLocationUpdates(provider, 
//          MIN_ZEIT,
//          MIN_DISTANZ,
//          this); 
//      Log.d(TAG, "starteGeoProvider(): bester möglicher Provider: " + provider);
    }  
  }

  /**
   * Verschickt die eigene Position per SMS (falls noch
   * nicht geschehen) an den Empfänger und anschließend an
   * den Amando-Server. Die eigene Position darf nicht älter
   * als 3 Minuten sein, sonst wird sie nicht gesendet.
   * 
   * @param sofort true, wenn die zuletzt bekannte Position
   *   sofort gesendet werden soll. false, wenn gewartet
   *   werden soll, bis eine neue aktuelle Position 
   *   ermittelt werden konnte.
   */
  private void sendeGeoPositionImpl(final boolean sofort) {
    Log.d(TAG, "sendeGeoPositionImpl(): sofort = " 
        + sofort);
    Log.d(TAG, "sendeGeoPositionImpl(): mPositionNachverfolgen = " 
        + mPositionNachverfolgen);
    // sende Position sofort (wenn die Methode aus
    // onLocationChanged heraus aufgerufen wurde) 
    // oder wenn die Ortsposition nicht älter als 
    // 3 Minuten ist:
    final long vorDreiMinuten = 
      System.currentTimeMillis() - 180000L;
    if (sofort || 
        mGpsData.getZeitstempel() < vorDreiMinuten) {
      Log.d(TAG, 
          "_sendeGeoPosition(): " +
          "Sende SMS. Empfaengernummer: " + 
          mEmpfaengerMobilnummern);
      // Falls noch keine SMS an die Empfänger geschickt
      // wurde, schicke nun einmalig eine SMS mit der
      // aktuellen Ortsposition:
      if (mEmpfaengerMobilnummern != null) {
        sendeGeoPositionPerSms();
        mEmpfaengerMobilnummern = null;
      }
      
      // letzte bekannte eigene Position an den
      // Amando-Server schicken, falls
      // die Clients (die Empfänger der SMS) die eigenen
      // Positionsänderungen
      // nachverfolgen dürfen:
      if (mPositionNachverfolgen) {
        sendeGeoPositionAnServer();
      }
    } else {
      // Tritt auf, wenn GPS-Modul eingeschaltet ist, man sich aber in geschlossenen
      // Räumen befindet. mGpsData ist dann hier null! Auf Network Provider wechseln!
      Log.w(TAG, "sendeGeoPositionImpl(): sofort: " + sofort);
      Log.w(TAG, "sendeGeoPositionImpl(): mGpsData: " 
          + mGpsData.toString());
    }
  }

  /** 
   * Schickt eine SMS an den Empfänger einer 
   * Geoposition. 
   */
  private void sendeGeoPositionPerSms() {
    Log.d(TAG,
        "sendeGeoPositionPerSms(): Schicke eine SMS an " +
        "die Empfänger der Geoposition");

    final GeoMarkierung geoMarkierung = new GeoMarkierung(
        mStichwort, mGpsData);

    if ("15555218135".equals(mEigeneMobilnummer)) {
      // Emulator:
      Log.d(TAG,
          "sendeGeoPositionPerSms(): Android-Geraet " +
          "ist: Emulator");
      AmandoSmsUtil.sendeSms(mEmpfaengerMobilnummern,
          geoMarkierung,
          mPositionNachverfolgen,
          AmandoSmsUtil.TYP_TEXT_SMS);
    } else { // echtes Gerät:
      Log.d(TAG,
          "sendeGeoPositionPerSms(): " +
          "Android-Geraet ist: Endgeraet");
      AmandoSmsUtil.sendeSms(mEmpfaengerMobilnummern,
          geoMarkierung,
          mPositionNachverfolgen,
          AmandoSmsUtil.TYP_DATEN_SMS);
    }
  }

  /** Schickt die aktuelle Position an den Amando-Server. */
  private void sendeGeoPositionAnServer() {
    Log.d(TAG,
        "sendeGeoPositionAnServer(): schicke die " +
        "aktuelle Position an den Amando-Server");
    final GeoPosition position = new GeoPosition(mGpsData,
        mEigeneMobilnummer);
    
    SendePositionService.setPosition(position);
    Intent intent = new Intent(this, SendePositionService.class);
    startService(intent);
  }

  /**
   * 
   * @return Die eigene Mobilnummer.
   */
  private String ermittleEigeneMobilnummer() {
    final String eigeneMobilnummer =
        TelefonnummernHelfer
        .ermittleEigeneMobilnummer(this);
    return TelefonnummernHelfer
        .bereinigeTelefonnummer(eigeneMobilnummer);
  }
  
  /**
   * @return true, wenn Position nachverfolgen aktiviert 
   * ist, sonst false.
   */
  public static boolean istPositionNachverfolgenAktiviert() {
    return mPositionNachverfolgen;
  }
  
  /**
   * Stoppt das Übermitteln der eigenen Position an den
   * Amando-Server. So kann der Freund die eigene 
   * Position nicht mehr nachverfolgen.
   */
  public static void stoppeNachverfolgung() {
    mPositionNachverfolgen = false;
  }
  
  @Override
  public void onLocationChanged(Location location) {
    Log.d(TAG, "MyLocationListener->" +
        "onLocationChanged(): entered...");
    if (location != null) {
      Log.d(TAG,
          "MyLocationListener->onLocationChanged(): " +
          "Längengrad: " + location.getLongitude());
      Log.d(TAG,
          "MyLocationListener->onLocationChanged(): " +
          "Breitengrad: " + location.getLatitude());
      
      mGpsData = new GpsData(location);

      if (mPositionNachverfolgen) {
        sendeGeoPositionImpl(true);
      }  

      // nur wenn gerade die Activity KarteAnzeige 
      // angezeigt wird:
      if (mKarteAnzeigenCallbackHandler != null) {
        final Bundle bundle = new Bundle();
        bundle.putParcelable(
            KarteAnzeigen.IN_PARAM_GEO_POSITION,
            location);  

        final Message msg = new Message();
        msg.setData(bundle);
        msg.what = KarteAnzeigen.TYP_EIGENE_POSITION;
        
        mKarteAnzeigenCallbackHandler.
            sendMessage(msg);
      } 
    }
  }

//  @Override
//  public void onProviderDisabled(String provider) {
//    Log.w(TAG, "onProviderDisabled(): " + provider);
//  }
//
//  @Override
//  public void onProviderEnabled(String provider) {
//    Log.i(TAG, "onProviderEnabled(): " + provider);
//  }
//
//  @Override
//  public void onStatusChanged(String provider, int status,
//      Bundle extras) {
//    Log.i(TAG, "onStatusChanged(): " + provider);
//  }

  @Override
  public void onConnectionFailed(ConnectionResult arg0) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void onConnected(Bundle arg0) {
    Location location = mLocationClient.getLastLocation();
    mGpsData = new GpsData(location);
    
    mLocationClient.requestLocationUpdates(mLocationRequest, this);  
  }

  @Override
  public void onDisconnected() {
    // TODO Auto-generated method stub
    
  }
  
  private boolean isGooglePlayServiceAvailable() {
    int errorCode = GooglePlayServicesUtil
            .isGooglePlayServicesAvailable(this);
    if (errorCode != ConnectionResult.SUCCESS) {
        return false;
    }
    return true;
  }

}
