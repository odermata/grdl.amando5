package de.visionera.androidbuch.amando5.gui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import de.visionera.androidbuch.amando5.R;
import de.visionera.androidbuch.amando5.db.GeoKontaktTbl;
import de.visionera.androidbuch.amando5.kontakt.GeoKontaktSpeicher;
import de.visionera.androidbuch.amando5.services.GeoPositionsService;

/**
 * Stellt das Layout zum Senden der eigenen Position bereit.
 * Es können ein oder mehrere Empfaenger aus den
 * Geokontakten gewaehlt werden. An diese wird mit Hilfe des
 * @see{GeoPositionsService} eine SMS mit den eigenen
 * Positionsdaten gesendet.
 *
 * @author Arno Becker, David Müller, 2015 visionera GmbH
 */
public class PositionSenden extends Activity {

  /** Kuerzel fuers Logging. */
  public static final String TAG = PositionSenden.class
      .getSimpleName();

  /** Local Service fuer das Senden der eigenen Position. */
  private GeoPositionsService.GeoPositionsServiceBinder 
      mGeoPositionsBinder;
  
  /** 
   * Wird angezeigt während die eigene Position versendet 
   * wird, solange noch keine aktuelle (nicht älter als 
   * 3 Minuten) Position vorliegt.
   */
  private ProgressDialog mProgressDialog;
  
  /** 
   * Remote Service fuer das Senden der eigenen Position. 
   */
  // private IGeoPositionsServiceRemote mService;


  /** Schnittstelle zur persistenten Speicher. */
  private GeoKontaktSpeicher mKontaktSpeicher;

  /** 
   * Die DB-Id des ausgewählten Kontaktes. 
   */
  private long mGeoKontaktId;

  /**
   * Liste mit den Mobilnummern der Empfaenger, so wie sie
   * aus den Geokontakten kommen. Die Normalisierung der
   * Nummern erfolgt spaeter.
   */
  private List<String> mEmpfaengerMobilnummern = 
    new ArrayList<String>();

  /**
   * Stichwort, das beim Senden der Position mitgeschickt
   * wird.
   */
  private String mStichwort;

  /**
   * Legt fest ob die eigene Position per Server für den
   * Empfänger veröffentlicht und aktualisiert wird.
   */
  private boolean mPositionNachverfolgen = true;
  
  /**
   * Flag legt fest, ob der GeoPositionsService neu gestartet
   * werden soll. Wird verwendet, falls das GPS-Modul in den
   * Systemeinstellungen aktiviert ist, man sich aber in
   * geschlossenen Räumen befindet. Nach Deaktivierung des 
   * GPS-Moduls muss der Service neu gestartet werden.
   */
  private boolean mRestartGeoPositionsService = false;

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
      mGeoPositionsBinder =
          (GeoPositionsService.
              GeoPositionsServiceBinder) binder;
    }

    public void onServiceDisconnected(
        ComponentName className) {
      Log.d(TAG,
          "mGpsServiceConnection->onServiceConnected(): " +
          "Verbindung zum Service beendet");
    }
  };  
  
  /* TODO: GPS-Service kann auch als Remote-Service 
           implementiert werden. */
  /**
   * Baut eine Verbindung zum Remote GPS-Service auf. Der
   * Service laeuft in einem eigenen Prozess.
   */
  /*private ServiceConnection mGpsServiceRemoteConnection = 
      new ServiceConnection() {
    
    // Wird aufgerufen, sobald die Verbindung zum
    // Remote-Service steht.
    @Override
    public void onServiceConnected(ComponentName className,
        IBinder binder) {
      mService = IGeoPositionsServiceRemote.Stub
          .asInterface(binder);

      try {
        mService.registriereCallback(mServiceCallback);
        mService.getGpsDataAsynchron();
      } catch (RemoteException e) {
        // diese Exception wird dann geworfen, wenn der
        // Service beendet wurde,
        // bevor wir uns mit ihm verbinden.
        Log.e(TAG,
            "remoteServiceConnection->" +
            "onServiceConnected(): Fehler: " + 
            e.toString());
      }
    }

    @Override
    public void onServiceDisconnected(ComponentName 
        className) {        
      try {
        mService.entferneCallback(mServiceCallback);
      } catch (RemoteException e) {
        Log.e(TAG, "remoteServiceConnection->" +
            "onServiceDisconnected(): Fehler: " + 
            e.toString()); 
      } 
    }
  }; */
  
  /* TODO: GPS-Service kann auch als Remote-Service 
    implementiert werden. */
  /**
   * Callback-Implementierung für den Remote Service.
   */
  /*private final IServiceCallback mServiceCallback = 
      new IServiceCallback.Stub() {
    public void aktuellePosition(GpsDataParcelable gpsData)
        throws RemoteException {
      Log.d(TAG, "aktuellePosition(): GpsData: " + 
          gpsData.toString());
      
      Location location = 
          new Location(LocationManager.GPS_PROVIDER);
      location.setLongitude(gpsData.mLaengengrad);
      location.setLatitude(gpsData.mBreitengrad);
      location.setAltitude(gpsData.mHoehe);
      location.setTime(gpsData.mZeitstempel);
      
      Bundle bundle = new Bundle();
      bundle.putParcelable("location", location);
      Message message = new Message();
      message.obj = location;
      message.setData(bundle);
      uiThreadCallbackHandler.sendMessage(message);
    }
  };*/

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.position_senden);
    Log.d(TAG, "onCreate(): entered...");

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
        
    final Intent intent = new Intent(this,
        GeoKontakteAuflisten.class);    
    intent.putExtra(GeoKontakteAuflisten.SELECT_KONTAKT, 
        true);
    
    startActivityForResult(intent, 0);
  }

  @Override
  protected void onStart() {
    Log.d(TAG, "onStart(): entered...");
    
    if (mRestartGeoPositionsService) {
      mGeoPositionsBinder.restarteGeoProvider();
      mRestartGeoPositionsService = false;
    }
    
    zeigeDetails();

    super.onStart();
  }

  @Override
  protected void onPause() {
    if (mProgressDialog != null 
        && mProgressDialog.isShowing()) {
      mProgressDialog.dismiss();
    }
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    unbindService(mGeoPositionsServiceConnection);
    if (mKontaktSpeicher != null) {
      mKontaktSpeicher.schliessen();
    }
    super.onDestroy();
  }

  /**
   * Füllt die Felder der View mit den Details des
   * ausgewählten GeoKontakts.
   */
  private void zeigeDetails() {
    Log.d(TAG, "zeigeDetails(): Kontakt-Id = " + 
        mGeoKontaktId);
    if (mGeoKontaktId <= 0) {
      return;
    }
    mKontaktSpeicher = new GeoKontaktSpeicher(this);
    
    final Cursor kontaktCursor =
        mKontaktSpeicher
        .ladeGeoKontaktDetails(mGeoKontaktId);        
    if (!kontaktCursor.moveToFirst()) {
      Log.e(TAG, "Kontakt nicht gefunden. Id " +
          mGeoKontaktId);
      return;
    }
  
    Log.d(TAG, "zeigeDetails(): Kontakt geladen " +
        kontaktCursor.getString(1));

    final TextView fldName = (TextView) 
      findViewById(R.id.tx_name);
    fldName.setText(kontaktCursor.getString(kontaktCursor
        .getColumnIndex(GeoKontaktTbl.NAME)));

    final TextView fldMobilnummer = (TextView) 
      findViewById(R.id.tx_telefon);
    mEmpfaengerMobilnummern.add(kontaktCursor
        .getString(kontaktCursor
        .getColumnIndex(GeoKontaktTbl.MOBILNUMMER)));
    fldMobilnummer.setText(
        mEmpfaengerMobilnummern.get(0));

    final TextView fldStichwort = (TextView) 
      findViewById(R.id.tx_stichwort);
    fldStichwort.setText("Finde mich");
    
    final double breitengrad = kontaktCursor.getDouble(
      kontaktCursor
      .getColumnIndex(GeoKontaktTbl.BREITENGRAD));
    final double laengengrad = kontaktCursor.getDouble(
      kontaktCursor
      .getColumnIndex(GeoKontaktTbl.LAENGENGRAD));
    Log.i(TAG, "zeigeDetails(): Laenge: " + laengengrad 
        + ", Breite: " + breitengrad);
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
   * Wird vom Position melden Knopf aufgerufen.
   * 
   * @see res.layout.position_senden.xml
   * 
   * @param view
   * 
   * @version Android 1.5
   */
  /*private OnClickListener mButtonPositionSenden = 
    new OnClickListener() {
    public void onClick(View v) {
      onClickPositionSenden(v);
    }
  };*/
  
  @Override
  protected void onActivityResult(int requestCode,
      int resultCode, Intent data) {
    Log.d(TAG, "onActivityResult: " + requestCode + ", " +
        data);
    switch (resultCode) {
      case Activity.RESULT_OK:
        mGeoKontaktId = data.getExtras().getLong(
            GeoKontakteAuflisten.IN_PARAM_KONTAKT_ID);
        break;
      case Activity.RESULT_CANCELED:
        Log.d(TAG, "onActivityResult: Geokontakt " +
            "auswählen wurde abgebrochen");
        finish();
        break;
      default:
        Log.d(TAG,
            "onActivityResult: Unexpected resultCode " +
            resultCode);
        break;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  /**
   * Wird vom Position melden Knopf aufgerufen.
   * 
   * @see res.layout.position_senden.xml
   * 
   * @param view Schaltfläche 'Position senden'
   */
  public void onClickPositionSenden(final View view) {
    final EditText fldStichwort = (EditText) 
      findViewById(R.id.tx_stichwort);
    mStichwort = fldStichwort.getText().toString();
    Log.d(TAG, "onClickPositionSenden(): mStichwort = " 
        + mStichwort);
    
    final CheckBox checkBox = 
        (CheckBox)findViewById(R.id.positionNachverfolgen);
    mPositionNachverfolgen = false;
    if (checkBox.isChecked()) {
      mPositionNachverfolgen = true;
    } 
    Log.d(TAG, "onClickPositionSenden(): mPositionNachverfolgen = " 
        + mPositionNachverfolgen);
    
    if (mGeoPositionsBinder.getGpsData() == null) {
      Log.w(TAG, "onClickPositionSenden(): Abbruch. Eigene Geoposition ist nicht bekannt.");
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setMessage(R.string.dialog_text_netzwerk_provider);
      builder.setTitle(R.string.dialog_titel_warnung_geoposition);
      builder.setPositiveButton(R.string.sf_ok, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          final Intent intent = 
            new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          startActivity(intent);
          mRestartGeoPositionsService = true;
        }
      });
      AlertDialog dialog = builder.create(); 
      dialog.show();
    } else {    
      // Warte auf Ermittlung der aktuellen Position:
      mProgressDialog = ProgressDialog.show(
          this, 
          "Bitte warten...",
          "Ermittle aktuelle Geoposition",
          true,
          false);
      
      // Sende SMS an Freund, übertrage optional die aktuelle 
      // Position initial per HTTP an den Amando-Server:
      mGeoPositionsBinder.sendeGeoPosition(
          mEmpfaengerMobilnummern, mStichwort,
          mPositionNachverfolgen);
    }  
  }
  
}
