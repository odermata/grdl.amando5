package de.visionera.androidbuch.amando5.gui;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.lang.ref.WeakReference;
import java.util.Locale;

import de.visionera.androidbuch.amando5.R;
import de.visionera.androidbuch.amando5.common.GpsData;
import de.visionera.androidbuch.amando5.kontakt.GeoKontakt;
import de.visionera.androidbuch.amando5.kontakt.GeoKontaktSpeicher;
import de.visionera.androidbuch.amando5.kontakt.TelefonnummernHelfer;
import de.visionera.androidbuch.amando5.receiver.SmsBroadcastReceiver;
import de.visionera.androidbuch.amando5.services.EmpfangePositionService;
import de.visionera.androidbuch.amando5.services.GeoPositionsService;
import de.visionera.androidbuch.amando5.services.GeoPositionsService.GeoPositionsServiceBinder;

/**
 * Zeigt Google Maps in einer MapActivity an.
 * <p>
 * Im Emulator zum Beispiel die Koordinaten mit <br>
 * 'geo fix 7.1 51.3' <br>
 * setzen um etwas zu sehen.
 */
public class KarteAnzeigen extends Activity 
   /* implements OnMarkerClickListener, OnCameraChangeListener */ {

  /** Kuerzel fuers Logging. */
  private static final String TAG = KarteAnzeigen.class
      .getSimpleName();

  /**
   * Schlüssel des zu bearbeitenden Kontakts, der per Intent
   * über geben wird.
   */
  static final String IN_PARAM_KONTAKT_ID = "KONTAKT_ID";
  
  /** 
   * Schlüssel für Intent-Parameter: KarteAnzeigen im 
   * Simulationsmodus starten oder nicht.
   */
  static final String IN_PARAM_SIMULATIONSMODUS = 
    "SimulationsModus";
  
  /** 
   * Schlüssel für Callback-Parameter aus dem 
   * GeoPositionsService: Geoposition
   */
  public static final String IN_PARAM_GEO_POSITION = 
    "location";
  
  /** 
   * Schlüssel für Callback-Parameter aus dem 
   * GeoPositionsService: Typ eigene Position
   */
  public static final int TYP_EIGENE_POSITION = 1;
  
  /** 
   * Schlüssel für Callback-Parameter aus dem 
   * GeoPositionsService: Typ Freund-Position
   */
  public static final int TYP_FREUND_POSITION = 2;
  
  /** Zoom-Level für die Karte beim Start der Activity. */
  private static final float DEFAULT_ZOOM_LEVEL = 17.5f;

  /**
   * GeoKontakt-Id des Kontakts, der in der Karte angezeigt 
   * wird.
   */
  private long mGeoKontaktId;
  
  /**
   * true, wenn die Position des Bekannten ständig in der 
   * Karte aktualisiert werden soll.
   */
  private static boolean mPositionNachverfolgen;
  
  /** true, wenn Amando im Simulationsmodus laufen soll. */
  private boolean mSimulationsModus;
  
  /** GeoKontakt, der in der Karte angezeigt wird. */
  private static GeoKontakt mFreundKontakt;

  /** Meine eigene zuletzt bekannte Position. */
  private static Location mMeinePosition;
  
  /** View, die die Google Map enthält. */
  private MapView mMapView;
  
  /** 
   * Kann null sein, wenn die Google Play App nicht installiert 
   * ist. Dies ist z.B. beim Emulator der Fall.
   */
  private GoogleMap mMap;
  
  /**
   * Intent-Filter für abgerissene und wiederhergestellte
   * Netzwerkverbindungen.
   */
  private final IntentFilter mIntentFilter = 
    new IntentFilter(
      "android.net.conn.CONNECTIVITY_CHANGE");
  
  /**
   * Empfaengt Intent, wenn sich der Status des Netzwerks
   * aendert connect/disconnect.
   */
  private ConnectionBroadcastReceiver mBroadcastReceiver;
  
  /**
   * Dieser BroadcastReceiver registriert
   * Netzwerk-Verbindungsabbrueche und wiederhergestellte
   * Netzwerkverbindungen.
   * 
   * @author becker
   */
  private class ConnectionBroadcastReceiver extends
      BroadcastReceiver {    
    
    @Override
    public void onReceive(Context ctxt, Intent intent) {      
      Log.d(TAG, "ConnectionBroadcastReceiver::onReceive(): entered...");
      final boolean isNotConnected =
          intent.getBooleanExtra(
          ConnectivityManager.EXTRA_NO_CONNECTIVITY,
          false);
      if (isNotConnected) {
        Log.d(TAG, "onReceive(): Netzwerkverbindung " +
            "verloren. Service beenden.");
        stopService(new Intent(ctxt, 
            EmpfangePositionService.class));
      } else {
        Log.d(TAG, "onReceive(): Verbindung besteht " +
            "wieder. Starte den Service neu...");
        if (mPositionNachverfolgen) {
          startePositionsService();
        }
      }      
    }
  };
  
  /**
   * Baut eine Verbindung zum GPS-Service auf. Der Service
   * laeuft im gleichen Prozess wie diese Activity. Daher
   * wird er automatisch beendet, wenn der Prozess der
   * Activity beendet wird.
   */
  private ServiceConnection mGeoPositionsServiceConnection =
      new ServiceConnection() {
    // Wird aufgerufen, sobald die Verbindung zum Service
    // steht.
    public void onServiceConnected(ComponentName className,
        IBinder binder) {
      Log.d(TAG,
          "mGpsServiceConnection->onServiceConnected(): " +
          "Verbindung zum Service aufgebaut");
      ((GeoPositionsServiceBinder) binder)
          .setzeActivityCallbackHandler(mKarteAnzeigenCallbackHandler);
    }

    public void onServiceDisconnected(
        ComponentName className) { }
  };  

  /**
   * Handler zur Verarbeitung von Callbacks aus dem
   * GeoPositionsService mit Hilfe von Message-Objekten. Der
   * GeoPositionsService empfängt die Positionsänderungen des
   * Clients, der die SMS an diese Anwendung geschickt hat,
   * bzw. eigene Positionsveränderungen.
   * Wird diese Activity angezeigt, so wird dem
   * GeoPositionsService dieser Callback-Handler übergeben.
   * Empfängt der GeoPositionsService eine Positionsänderung,
   * stellt er ein Message-Objekt in die Message-Queue des
   * Handlers. Dadurch wird die hier überschriebene Methode
   * 'handleMessage()' aufgerufen, in der die
   * Positionänderung des Clients in der Karte aktualisiert
   * wird.
   */
  private static Handler mKarteAnzeigenCallbackHandler; 
  
  /** Marker für die eigene Position in der Karte. */
  private Marker mMeinMarker;
  
  /** Marker für die eigene Position in der Karte. */
  private Marker mFreundMarker;
  
  /** Verbindungslinie zwischen eigener Position und Freund. */
  private Polyline mVerbindungslinie;
  
  public void handleMessage(Message msg) {
    Log.d(TAG, "Handler->handleMessage(): Handler: " + this);
    Log.d(TAG, "Handler->handleMessage(): mMap: " + mMap);
    
    final Bundle bundle = msg.getData();      
    if (bundle != null) {
      final Location location = (Location) bundle
          .get(KarteAnzeigen.IN_PARAM_GEO_POSITION);
      final LatLng latLng = new LatLng(location.getLatitude(), 
          location.getLongitude());
    
      final int typ = msg.what;
      Log.d(TAG, "Handler->handleMessage(): "
          + "Bundle empfangen. What = " + typ);   
      
      final MarkerOptions markerOption = new MarkerOptions();
      markerOption.position(latLng);
      
      if (typ == TYP_EIGENE_POSITION) {
        Log.d(TAG, "Handler->handleMessage(): meine Position = " 
            + location.toString());
        mMeinePosition = location;
        if (mMap != null) {          
          if (mMeinMarker != null) {
            mMeinMarker.remove();
          }  
          markerOption.title(getString(R.string.msg_position_ich));
          mMeinMarker = mMap.addMarker(markerOption);
          mMeinMarker.showInfoWindow();
          
          if (!mPositionNachverfolgen) {
             mMap.animateCamera(CameraUpdateFactory
                .newLatLng(latLng));                  
          }
        }  
      } else if (typ == TYP_FREUND_POSITION && isFreundKontaktVorhanden()) {
        Log.d(TAG, "Handler->handleMessage(): Freund-Position = " 
            + location.toString());
        Log.d(TAG, "Handler->handleMessage(): Karte auf Freund-Position ausrichten");
      
        GpsData gpsData = mFreundKontakt.letztePosition.gpsData;
        Log.d(TAG, "Handler->handleMessage(): Position Freund: " + gpsData.toString());
        LatLng altePosition = new LatLng(gpsData.getBreitengrad(), 
            gpsData.getLaengengrad());
        
        mFreundKontakt.letztePosition.gpsData = 
            new GpsData(location);
        mFreundKontakt.mobilnummer = 
          bundle.getString(GeoKontakt.KEY_MOBILNUMMER); 
        if (mMap != null) { 
          Log.d(TAG, "Handler->handleMessage(): Karte neu positionieren");
          Log.d(TAG, "Handler->handleMessage(): location = " + location.toString());
          
          mMap.animateCamera(CameraUpdateFactory
              .newLatLng(latLng));
                      
          if (mFreundMarker != null) {
            mFreundMarker.remove();
          }  
          
          markerOption.title(getString(R.string.msg_position_freund));
          markerOption.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
          
          // Blaue Linie zur eigenen Position ziehen:
          if (mMeinePosition != null) {    
            Log.d(TAG, "Handler->handleMessage(): Linie zu eigener Position ziehen");
            if (mVerbindungslinie != null) {
              mVerbindungslinie.remove();
            }
            mVerbindungslinie = mMap.addPolyline(new PolylineOptions()
                .add(latLng, new LatLng(mMeinePosition.getLatitude(), 
                    mMeinePosition.getLongitude()))
                .width(5)
                .color(Color.BLUE));
            
            float distanz = mMeinePosition.distanceTo(location);
            String snippet = String.format(Locale.getDefault(), "%.2f", distanz);
            markerOption.snippet("Entfernung: " + snippet + " Meter");
          }  
          mFreundMarker = mMap.addMarker(markerOption); 
          mFreundMarker.showInfoWindow();
          
          // Strecke, die der Freund bisher zurückgelegt hat.            
//          mMap.addPolyline(new PolylineOptions()
//          .add(latLng, altePosition)
//          .width(3)
//          .color(Color.YELLOW));
        }  
      }  
    }  
  }  
      
  /**
   * Liest die im Intent übergebenen Daten aus und 
   * speichert sie in Instanzvariablen ab.
   * 
   * Am wichtigsten ist dabei die id des Geokontaktes, 
   * damit im nächsten Schritt die Geokontaktdaten
   * geladen werden können.
   * 
   * @param extras Bundle mit Geokontakt-Id, 
   *   Simulationsmodus und dem Position-Nachverfolgen-Flag.
   */
  private void verarbeiteIntentBundle(final Bundle extras) {
    if (extras != null) {
      if (extras.containsKey(IN_PARAM_KONTAKT_ID)) {
        mGeoKontaktId = extras.getLong(IN_PARAM_KONTAKT_ID);
        Log.d(TAG, "verarbeiteIntentBundle(): Aufruf mit "
            + "Kontakt-Id " + mGeoKontaktId);
      }
      
      if (extras.containsKey(SmsBroadcastReceiver.
          KEY_NACHVERFOLGEN)) {
        mPositionNachverfolgen = 
          extras.getBoolean(SmsBroadcastReceiver.
              KEY_NACHVERFOLGEN);
        Log.d(TAG, "verarbeiteIntentBundle(): "
            + "mPositionNachverfolgen: " + mPositionNachverfolgen);
      }
      
      if (extras.containsKey(IN_PARAM_SIMULATIONSMODUS)) {
        mSimulationsModus = 
          extras.getBoolean(IN_PARAM_SIMULATIONSMODUS);
        Log.d(TAG, "verarbeiteIntentBundle(): "
            + "mSimulationsModus: " + mSimulationsModus);
      }  
    }
  }
  
  private boolean isGooglePlayServiceAvailable() {
    int errorCode = GooglePlayServicesUtil
            .isGooglePlayServicesAvailable(this);
    if (errorCode != ConnectionResult.SUCCESS) {
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                errorCode, this, -1);
        if (errorDialog != null) {
            errorDialog.show();
            return false;
        }
    }
    return true;
  }

  /**
   * Stellt die MapView und deren Controller nach unseren
   * Vorlieben ein.
   * 
   * <li>Zoomstufe</li>
   * <li>Zoomkontrollen Anzeige</li>
   * <li>Typ der angezeigten Karte</li>
   * 
   */
  private void initMapView() {
    Log.d(TAG, "initMapView(): aufgerufen...");

    boolean usePlayService = isGooglePlayServiceAvailable();
    if (usePlayService) {  
        MapsInitializer.initialize(this);

      if (mMap == null) {
        mMap = mMapView.getMap();
        if (mMap != null) {
          Log.d(TAG, "initMapView(): MapView initialisieren");
          mMap.getUiSettings().setZoomControlsEnabled(true);
          mMap.getUiSettings().setCompassEnabled(true);
          mMap.setMyLocationEnabled(true);      
          //mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
          mMap.setIndoorEnabled(true); 
          mMap.setTrafficEnabled(true);  
          
          Log.d(TAG, "initMapView(): MaxZoomLevel: " 
              + mMap.getMaxZoomLevel());
          
          //mMap.setOnMarkerClickListener(this);      
          //mMap.setOnCameraChangeListener(this);
          
          // Default-Zoomlevel:          
          mMap.animateCamera(CameraUpdateFactory
              .zoomTo(DEFAULT_ZOOM_LEVEL)); 
        }
      }  
    } else {
      finish();
    }
  }
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate(): entered...");
  
    setContentView(R.layout.karte_anzeigen);
       
    verarbeiteIntentBundle(getIntent().getExtras());
    
    mKarteAnzeigenCallbackHandler = 
        new KarteAnzeigenCallbackHandler(this);
    
    mMapView = (MapView) findViewById(R.id.mv_karte_anzeigen);
    mMapView.onCreate(savedInstanceState);
    
    initMapView();
    
    // Alternative 1: Local Service:
    // verbinde mit Service zum Ermitteln der GPS-Daten:
    final Intent geoIntent = new Intent(this,
        GeoPositionsService.class);
    bindService(geoIntent, mGeoPositionsServiceConnection,
        Context.BIND_AUTO_CREATE);
    
    // Alternative 2: Remote-Service:
    // verbinde mit Remote Service zum Ermitteln 
    // der GPS-Daten:
//    Intent geoIntent = new Intent(this, 
//        GeoPositionsServiceRemoteImpl.class);
//    bindService(geoIntent, mGpsServiceRemoteConnection, 
//        Context.BIND_AUTO_CREATE);
  }
  
  @Override
  protected void onDestroy() {
    Log.d(TAG, "onDestroy() aufgerufen...");
    
    mMapView.onDestroy();    
    mKarteAnzeigenCallbackHandler.removeCallbacksAndMessages(null);
    
    unbindService(mGeoPositionsServiceConnection);
    stopService(new Intent(this, GeoPositionsService.class));
    
    stopService(new Intent(this, 
        EmpfangePositionService.class));
    
    super.onDestroy();
  }

  @Override
  protected void onStart() {
    super.onStart();
    Log.d(TAG, "onStart() aufgerufen...");
    ladeGeoKontakt();    
  }
  
  @Override
  protected void onResume() {
    Log.d(TAG, "onResume() aufgerufen...");
    
    if (mMapView != null) { 
      // null, wenn Google Play Store APK nicht installiert ist
      mMapView.onResume();
    }   
   
    mBroadcastReceiver = new ConnectionBroadcastReceiver();
    registerReceiver(mBroadcastReceiver, mIntentFilter);
    
    // Übergebe Callback-Handler an GeoPositionsService:
    EmpfangePositionService.setzeActivityCallbackHandler(
        mKarteAnzeigenCallbackHandler);
    
    // Simuliere eine eigene Positionsmeldung über den 
    // LocationManager:
    if (mSimulationsModus) {
      Log.d(TAG, "onResume(): App im Simulationsmodus");
      initalisiereEmulatorModus();  
    }  
    
    // wenn die Geokontakt-Id existiert, wurde diese Activity
    // aufgerufen, nachdem eine SMS mit der Geoposition des
    // Freundes empfangen wurde. Karte positionieren, indem
    // die Freund-Position an den Handler übergeben wird.
    if (mGeoKontaktId > 0) {
      final Location location = 
          mFreundKontakt.letztePosition.gpsData.location;
      final Bundle bundle = new Bundle();
      bundle.putParcelable(
          KarteAnzeigen.IN_PARAM_GEO_POSITION,
          location); 
      final Message msg = new Message();
      msg.setData(bundle);
      msg.what = KarteAnzeigen.TYP_FREUND_POSITION;
      
      Log.d(TAG, "onResume(): Sende Message um Karte zu positionieren.");
      mKarteAnzeigenCallbackHandler.sendMessage(msg);
    }
    
    super.onResume();
  }
  
  @Override
  protected void onPause() {
    Log.d(TAG, "onPause() aufgerufen...");
    mMapView.onPause();
    unregisterReceiver(mBroadcastReceiver);       
    super.onPause();
  }
  
  @Override
  public void onLowMemory() {
      super.onLowMemory();
      mMapView.onLowMemory();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      mMapView.onSaveInstanceState(outState);
  }
    
//  @Override
//  public void onCameraChange(CameraPosition cp) {
//    Log.d(TAG, "onCameraChange(): Zoom-Level: " + cp.zoom);    
//  }

//  @Override
//  public boolean onMarkerClick(final Marker marker) {
//      if (marker.equals(mMarker)) {
//          Log.d(TAG, "onMarkerClick(): Marker der eigenen Position angeklickt");
//          Toast.makeText(this, "Marker angeklickt", Toast.LENGTH_LONG).show();
//          return false; // Info "Hier bin ich" soll angezeigt werden
//      }
//      return true;
//  }
  
  /**
   * Lädt den auf der Karte anzuzeigenden GeoKontakt.
   * Wenn kein Kontakt vorhanden, bleibt geoKontaktId auf 0.
   */
  private void ladeGeoKontakt() {
    final GeoKontaktSpeicher kontaktSpeicher = 
      new GeoKontaktSpeicher(this);   
    
    Log.d(TAG, "ladeGeoKontakt() aufgerufen...");
    if (mGeoKontaktId != 0) {
      mFreundKontakt =
        kontaktSpeicher.ladeGeoKontakt(mGeoKontaktId);    
      if (mFreundKontakt != null) {
        Log.d(TAG, "ladeGeoKontakt(): Kontakt geladen: " +
            mFreundKontakt.name);           
        Log.d(TAG, "ladeGeoKontakt(): Gps-Daten des " +
            "Kontakts: " +
            mFreundKontakt.letztePosition.toString());
      }  
    } 
    // FIXME: mp - feature ist nicht sinnvoll. wenn kein kontakt da, dann nix anzeigen.
//    else {
//      //einfach den ersten (== neuesten) Kontakt holen
//      Log.w(TAG, "ladeGeoKontakt(): Keine Kontakt-Id.");
//      final Cursor c = kontaktSpeicher.
//        ladeGeoKontaktListe(null);
//      c.moveToFirst(); 
//      mFreundKontakt = kontaktSpeicher.ladeGeoKontakt(
//          c.getLong(c.getColumnIndex(GeoKontaktTbl.ID)));
//    }
    kontaktSpeicher.schliessen();
  }
  
  /**
   * Ermittelt die eigene Mobilnummer aus den 
   * Systemeinstellungen.
   * 
   * @return Eigene Mobilnummer als String
   */
  private String ermittleEigeneMobilnummer() {
    Log.d(TAG, "ermittleEigeneMobilnummer(): entered...");
    final String eigeneMobilnummer = 
      TelefonnummernHelfer.ermittleEigeneMobilnummer(this);
    return TelefonnummernHelfer.bereinigeTelefonnummer(
        eigeneMobilnummer);
  }
  
  /**
   * Startet den EmpfangePositionService. 
   */
  private void startePositionsService() {
    Log.d(TAG, "startePositionsService(): entered...");
    final Intent intent = new Intent(this,
        EmpfangePositionService.class);
    if( isFreundKontaktVorhanden() ) {
      Log.d(TAG, "startePositionsService(): Freundkontakt-Mobilnummer: " 
          + mFreundKontakt.mobilnummer);
      intent.putExtra(EmpfangePositionService
          .KEY_SENDER_MOBILNUMMER, 
          mFreundKontakt.mobilnummer);      
    }
    intent.putExtra(EmpfangePositionService
        .KEY_EMPFAENGER_MOBILNUMMER, 
        ermittleEigeneMobilnummer());
    
    startService(intent);    
  }

  /**
   * Falls die Karte im Simulationsmodus angezeigt wird,
   * erzeuge die Position des Freunds als Mock-Wert.
   */
  private void initalisiereEmulatorModus() {
    Log.d(TAG, "initalisiereEmulatorModus(): entered...");
    // Falls die Anwendung im Emulator läuft, soll die 
    // eigene Position simuliert werden, da der Emulator 
    // ohne Hilfe erst mal gar keine Position liefert:
    final String eigeneMobilnummer = 
      ermittleEigeneMobilnummer();
    // Emulator:
    if ("15555218135".equals(eigeneMobilnummer)) { 
      final double laengengrad = 
        mFreundKontakt.letztePosition.gpsData.getLaengengrad();
      final double breitengrad = 
        mFreundKontakt.letztePosition.gpsData.getBreitengrad();
      final double hoehe = 64.3;
      final long zeitstempel = System.currentTimeMillis();
        
      // setze die eigene Ortsposition für den Test:
      final LocationManager locationManager = 
        (LocationManager) getSystemService(
            Context.LOCATION_SERVICE);          
      final String mockProvider = 
        LocationManager.GPS_PROVIDER;
      
      if (locationManager.getProvider(mockProvider) != 
          null) {
        locationManager.addTestProvider(mockProvider, false,
            false, false, false, false, false, false, 0, 5);
        locationManager.setTestProviderEnabled(mockProvider,
            true);
        
        final Location location = 
            new Location(LocationManager.GPS_PROVIDER);
          location.setLongitude(laengengrad); 
          location.setLatitude(breitengrad);    
          location.setAltitude(hoehe);
          location.setTime(zeitstempel);
          
          locationManager.setTestProviderLocation(
            mockProvider, location);
      } 
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.karte_anzeigen, menu);

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.opt_position_senden: 
        final Intent i = new Intent(this,
            PositionSenden.class);
        startActivity(i);
        return true;
      
      case R.id.opt_navigation_starten: 
        return navigationStarten();      
  
      case R.id.opt_geokontakt_anrufen: 
        geoKontaktAnrufen();
        return true;
      
      case R.id.opt_hilfe: 
        final Intent j = new Intent(this, 
            HilfeAnzeigen.class);
        startActivity(j);
        return true;
  
      default: 
        Log.w(TAG, "unbekannte Option gewaehlt: " + item); 
        return super.onOptionsItemSelected(item);
    }
  }

  protected boolean isFreundKontaktVorhanden() {
    return mGeoKontaktId > 0 && mFreundKontakt != null;
  }
  
  /**
   * Startet die Anzeige von Routeninformationen,
   * bzw. die Navigation über Google Navigation.
   */
  protected boolean navigationStarten()  {
    Log.d(TAG, "navigationStarten(): start...");
    if (mMeinePosition == null
        || mFreundKontakt.letztePosition.gpsData == null) {
      return false;
    }
    
    final String geoKontaktPosition = mFreundKontakt.
      letztePosition.gpsData.getBreitengrad() + "," + 
      mFreundKontakt.letztePosition.gpsData.getLaengengrad();
        
    final String meinePosition = 
        mMeinePosition.getLatitude() + "," 
        + mMeinePosition.getLongitude();
  
    Log.d(TAG, "navigationStarten(): Positionen: Freund = " + geoKontaktPosition 
        + " Ich = " + meinePosition);
    
    // so kann Google Maps gestartet werden
//    final Intent navigation = new Intent(Intent.ACTION_VIEW,
//        Uri.parse("http://maps.google.com/maps?saddr=" +
//            meinePosition + "&daddr=" 
//            + geoKontaktPosition));
//    startActivity(navigation);
  
    // so kann Google Navigation gestartet werden
    final Intent i = new Intent(Intent.ACTION_VIEW, 
        Uri.parse("google.navigation:q=" + 
            geoKontaktPosition));
    startActivity(i);
    return true;
  }
  
  /**
   * Startet einen Anruf des auf der Karte angezeigten 
   * GeoKontakts.
   */
  protected void geoKontaktAnrufen() {
    Log.d(TAG, "geoKontaktAnrufen(): rufe an: " + mFreundKontakt.mobilnummer);
    final Intent intent = new Intent(Intent.ACTION_DIAL, 
        Uri.parse("tel:" + mFreundKontakt.mobilnummer));
    startActivity(intent);
  }
  
  /**
   * Handler zur Verarbeitung von Callbacks aus dem
   * NetzwerkService mit Hilfe von Message-Objekten. Der
   * NetzwerkService empfängt die Positionsänderungen der
   * Clients, der die SMS an diese Anwendung geschickt hat.
   * Wird diese Activity angezeigt, so wird dem
   * NetzwerkService dieser Callback-Handler übergeben.
   * Empfängt der NetzwerkService eine Positionsänderung,
   * stellt er ein Message-Objekt in die Message-Queue des
   * Handlers. Dadurch wird die hier überschriebene Methode
   * 'handleMessage()' aufgerufen, in der die
   * Positionänderung des Clients in der Karte aktualisiert
   * wird.
   */
  static class KarteAnzeigenCallbackHandler extends Handler {
    private WeakReference<KarteAnzeigen> mActivity; 

    KarteAnzeigenCallbackHandler(KarteAnzeigen activity) {
      mActivity = new WeakReference<KarteAnzeigen>(activity);
    }
    
    @Override
    public void handleMessage(Message msg) {
      KarteAnzeigen activity = mActivity.get();
       if (activity != null) {
         activity.handleMessage(msg);
       }
    }
  }
}
