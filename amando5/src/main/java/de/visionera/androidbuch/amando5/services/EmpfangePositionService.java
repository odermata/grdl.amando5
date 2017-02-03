package de.visionera.androidbuch.amando5.services;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import de.visionera.androidbuch.amando5.NetzwerkKonfigurator;
import de.visionera.androidbuch.amando5.R;
import de.visionera.androidbuch.amando5.gui.KarteAnzeigen;
import de.visionera.androidbuch.amando5.kontakt.GeoKontakt;
import de.visionera.androidbuch.amando5.receiver.SmsBroadcastReceiver;

/**
 * Der Service dient zum Empfang der Geoposition eines anderen
 * Teilnehmers.
 * Nach dem Erhalt der SMS mit der Geoposition von dem anderen
 * Teilnehmer wird dieser Service gestartet. Es wird eine
 * Verbindung zum Amando-Server aufgebaut. Ein Listener wird
 * erzeugt und gestartet. Dieser Listerner empfängt über eine
 * permanente Socket-Verbindung die Positionsveränderungen
 * des anderen Teilnehmers. 
 * Schlüssel für den Empfang aktueller 
 * Positionsdaten ist die Mobilnummer des SMS-Senders. 
 * Die Verbindung zum Server wird von beiden Parteien dauerhaft 
 * gehalten. Bricht die Verbindung weg (Funkloch), wird sie
 * automatisch neu aufgebaut.
 * 
 * Dieser Service ist ein Beispiel für eine Implementierung der
 * Service-Klasse als Sticky-Service.
 * 
 * @author Arno Becker, 2015 visionera GmbH
 */
public class EmpfangePositionService extends Service {
  
  /** Tag für die LogCat. */
  private static final String TAG = 
      EmpfangePositionService.class.getSimpleName();
  
  /** Intent-Key zum Übertragen der Sender-Mobilnummer. */
  public static final String KEY_SENDER_MOBILNUMMER 
      = "sender_mobilnr";
  
  /** Intent-Key zum Übertragen der Empfänger-Mobilnummer. */
  public static final String KEY_EMPFAENGER_MOBILNUMMER 
      = "empfaenger_mobilnr";

  /** Id für den Scheduler Job dieses Service. */
  public static final int SCHEDULE_JOB_ID = 1;
  
  /** 
   * Id der Notification, wenn der Service im Vordergrund
   * gestartet wird.
   */
  private static final int NOTIFICATION_ID = 77;

  /** Socket zum Amando-Server. */
  private Socket mSocket;
  
  /** Reader für Socket-Daten vom Server. */
  private volatile BufferedReader mBufferedReader;
  
  /** 
   * Thread zum Lauschen auf neue Positionsdaten
   * bei offen-gehaltener Socket-Verbindung.
   */
  private EmpfangePositionsThread mPositionListenerThread;

  /**
   * Handler zur Aktualisierung von KarteAnzeigen-Activity,
   * falls neue Positionsdaten vorliegen.
   */
  private static Handler mKarteAnzeigenCallbackHandler;
    
  /**
   * Lauscht, ob sich der Besitzer von 'mobilNummer' 
   * bewegt.
   * Falls ja, wird dem @see{karteAnzeigenCallbackHandler}
   * eine Message mit der neuen Position des Clients
   * übegeben.
   * 
   * @param senderMobilNummer
   *          Mobilnummer des Senders der SMS
   * @param clientMobilNummer
   *          Mobilnummer dieses Geräts (Client)
   * @return true, wenn die Verbindung mit dem Server
   *         klappt. Sonst false
   */
  private void empfangeFreundPosition(
      final String senderMobilNummer,
      final String clientMobilNummer) {
    Log.d(TAG, "empfangeFreundPosition(): "
            + "Sender-Mobilnummer: "
            + senderMobilNummer);
    Log.d(TAG, "empfangeFreundPosition(): "
            + "Client-Mobilnummer: "
            + clientMobilNummer);
    
    mPositionListenerThread = 
        new EmpfangePositionsThread(senderMobilNummer, 
            clientMobilNummer);
    mPositionListenerThread.start();
  }
  
  /**
   * Übergibt dem Service einen Callback-Handler, um die
   * aktualisierte Position der Clients zu melden. Der
   * Handler wird in der Activity @see{KarteAnzeigen}
   * definiert und dem NetzwerkSerice übergeben. Er dient
   * dem Aktualisieren der Positionen der Clients (dem
   * Sender der SMS).
   * 
   * @param callback
   *          ein Handler-Objekt zur Aktualisierung der
   *          Oberfläche.
   */
  public static void setzeActivityCallbackHandler(
      final Handler callback) {
    mKarteAnzeigenCallbackHandler = callback;
  }
  
  /**
   * Entfernt den CallbackHandler, der mit dem 
   * NetzwerkService übergeben wurde. Die Methode wird in
   * Activity KarteAnzeigen.java gebraucht, wenn die 
   * Activity nicht mehr sichtbar ist.
   * 
   * @see{setAnzeigeCallbackHandler}
   * @see{KarteAnzeigen} 
   */
  public static void entferneActivityCallbackHandler() {
    mKarteAnzeigenCallbackHandler = null;
  }
  
  /** 
   * Sende die eigene aktuelle Position an den Server. 
   * 
   * @param position Aktuelle eigene Position.
   */
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.v(TAG, "onHandleIntent(): entered. " 
    		+ "Empfange Freund-Position vom Amando-Server");

    starteJob();
    
    Intent i = new Intent(this, KarteAnzeigen.class);
    final PendingIntent pendingIntent = 
        PendingIntent.getActivity(this, 0, i, 0);
    
    final Notification notification = 
        new NotificationCompat.Builder(this)
      .setContentTitle(this.getResources()
          .getText(R.string.not_empfange_pos_service_aktiv))
      // .setContentText("Subject")
      .setSmallIcon(R.drawable.logo_klein)
      .setContentIntent(pendingIntent)
      .build();
    
    startForeground(NOTIFICATION_ID, notification);
    
    final String senderMobilNr =
        intent.getStringExtra(KEY_SENDER_MOBILNUMMER);
    final String empfaengerMobilNr =
        intent.getStringExtra(KEY_EMPFAENGER_MOBILNUMMER);
    
    empfangeFreundPosition(senderMobilNr, 
        empfaengerMobilNr);        
    
    // Wie START_STICKY, aber der Intent mit den Mobilnummern wird
    // ebenfalls mit übergeben: 
    return START_REDELIVER_INTENT;
  }  
 
  @Override
  public void onDestroy() {
    Log.d(TAG, "onDestroy(): entered...");
    try {
      if (mSocket != null && mSocket.isConnected()) {
        mSocket.close();         
      }
    } catch (IOException e) {
      Log.e(TAG, "onDestroy(): Fehler beim Schliessen: " 
          + "des Sockets. " + e.toString());
    }

    beendeJob();

    super.onDestroy();
  }
  
  private class EmpfangePositionsThread extends Thread {
    
    private String senderMobilNummer;
    private String clientMobilNummer;
    
    public EmpfangePositionsThread(String senderMobilNummer,
        String clientMobilNummer) {
      this.senderMobilNummer = senderMobilNummer;
      this.clientMobilNummer = clientMobilNummer;
    }  
    
    @Override
    public void run() {
      if (netzwerkVerbindungHerstellen()) {
        if (registrierePositionsdatenListener(
            senderMobilNummer,
            clientMobilNummer)) {
          startePositionsdatenListener();          
        }
      }
      Log.d(TAG, "run(): leave...");
    }
    
    /**
     * Lauscht auf eigehende Nachrichten vom
     * Amando-Server. Dieser übermittelt neue 
     * Geopositionen des Bekannten an diesen Listener.
     */
    private void startePositionsdatenListener() {
      try {
        Log.d(TAG, "startePositionsdatenListener(): " +
            "aufgerufen...");

        String senderGeoposition;
        // blockiere, bis Verbindung zum Server steht:
        while (!mBufferedReader.ready()) { 
          try {
            Thread.sleep(200);
          } catch (InterruptedException e) { 
            e.printStackTrace();
          }
        }
        Log.d(TAG, "startePositionsdatenListener()-> ready...");

        while ((senderGeoposition = 
              mBufferedReader.readLine()) != null) {
          // Beispiel:
          // 01607644553#37.2345675#49.7634567#
          //   25.63656776#1265385731102
          Log.d(TAG, "startePositionsdatenListener()->Vom Server uebermittelt: "
              + senderGeoposition);

          // nur aktiv, wenn gerade die Activity
          // 'KarteAnzeigen' angezeigt wird:
          if (senderGeoposition != null &&
              !"".equals(senderGeoposition) &&
              mKarteAnzeigenCallbackHandler != null) {
            
            final String[] parameter = senderGeoposition
                .split("#");
            
            final Location ortsPosition = 
                new Location(SmsBroadcastReceiver.LOCATION_PROVIDER);
            ortsPosition.setLongitude(Double
                .parseDouble(parameter[1]));
            ortsPosition.setLatitude(Double.parseDouble(parameter[2]));
            ortsPosition.setAltitude(Double.parseDouble(parameter[3]));
            ortsPosition.setTime(Long.parseLong(parameter[4]));              

            final Bundle bundle = new Bundle();
            bundle.putParcelable(
                KarteAnzeigen.IN_PARAM_GEO_POSITION,
                ortsPosition);
            bundle.putString(GeoKontakt.KEY_MOBILNUMMER,
                parameter[0]);

            final Message msg = new Message();
            msg.setData(bundle);
            msg.what = KarteAnzeigen.TYP_FREUND_POSITION;

            mKarteAnzeigenCallbackHandler.
                sendMessage(msg);
          }
        } // ende while
        Log.d(TAG, "startePositionsdatenListener()->" +
            "run(): Listener beendet...");
        mPositionListenerThread = null;
      } catch (IOException ex) {
        Log.d(TAG, "startePositionsdatenListener->" +
            "run()->: " + ex.toString());
        mPositionListenerThread = null;
        return;
      }
    }  
    
    /**
     * Baut eine Socket-Verbindung zum Amando-Server auf.
     * @return true, falls die Verbindung erfolgreich
     *   aufgebaut wurde.
     */
    private boolean netzwerkVerbindungHerstellen() {
      Log.d(TAG, "netzwerkVerbindungHerstellen(): " +
          "Netzwerkverbindung herstellen...");
      try {
        // Neu: Socket mit Timeout, falls der Server nicht
        // verfuegbar ist:
        Log.d(TAG,
            "netzwerkVerbindungHerstellen(): Server-IP: "
                + NetzwerkKonfigurator.SERVER_IP);
        Log.d(TAG,
            "netzwerkVerbindungHerstellen(): Portnummer: "
                + NetzwerkKonfigurator.SOCKET_PORTNUM);
        
        mSocket = new Socket();
        mSocket.connect(new InetSocketAddress(
            NetzwerkKonfigurator.SERVER_IP,
            NetzwerkKonfigurator.SOCKET_PORTNUM), 15000);
        
        if (mSocket != null) {
          mBufferedReader = new BufferedReader(
              new InputStreamReader(mSocket.getInputStream()));
        } else {
          Log.e(TAG, "netzwerkVerbindungHerstellen(): Socket-Verbindung "
              + "konnte nicht hergestellt werden!");
          return false;
        }
      } catch (UnknownHostException e) {
        Log.e(TAG, "netzwerkVerbindungHerstellen(): ", e);
        mSocket = null;
        return false;
      } catch (IOException e) {
        Log.e(TAG, "netzwerkVerbindungHerstellen(): ", e);
        mSocket = null;
        return false;
      }        
      return true;
    }
      
    /**
     * Schickt einen Schlüssel, bestehend aus der eigenen
     * Mobilnummer und aus der Mobilnummer des SMS-Senders 
     * an den Server, um die zu diesem Zeitpunkt schon 
     * bestehende Socket-Verbindung fuer 
     * Positionsveränderungen eines
     * Android-Geräts mit einer bestimmten Mobilnummer zu
     * registrieren.
     * 
     * @param senderMobilNummer Mobilnummer des SMS-Senders
     * @param clientMobilNummer eigene Mobilnummer 
     * @return true, falls der Schlüssel erfolgreich an den
     *   Amando-Server übermittelt werden konnte.
     */
    private boolean registrierePositionsdatenListener(
        final String senderMobilNummer,
        final String clientMobilNummer) {
      Log.d(TAG,
          "registrierePositionsdatenListener(): entered...");
      try {
        // Format: senderNummer#clientNummer
        final String schluessel = senderMobilNummer + "#"
            + clientMobilNummer;
        final PrintWriter pw = new PrintWriter(mSocket
            .getOutputStream(), true);
        pw.println(schluessel);

        return true;
      } catch (IOException e) {
        Log.e(TAG, "registrierePositionsdatenListener():"
            + e.getMessage());
        return false;
      }
    }
  }

  @TargetApi(21)
  @SuppressLint("NewApi")
  private void starteJob() {
    ComponentName serviceKomponente =
            new ComponentName(this, EmpfangePositionService.class);

    JobInfo.Builder builder =
        new JobInfo.Builder(SCHEDULE_JOB_ID, serviceKomponente);

    // builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);
    builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);

    JobInfo empfangePositionJob = builder.build();

    JobScheduler jobScheduler =
        (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
    jobScheduler.schedule(empfangePositionJob);
  }

  @TargetApi(21)
  @SuppressLint("NewApi")
  private void beendeJob() {
    JobScheduler jobScheduler =
        (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
      jobScheduler.cancel(SCHEDULE_JOB_ID);
  }

  @Override
  public IBinder onBind(Intent intent) {
    // wird nicht benötigt. Dies ist ein Sticky-Service
    return null;
  }

  
}
