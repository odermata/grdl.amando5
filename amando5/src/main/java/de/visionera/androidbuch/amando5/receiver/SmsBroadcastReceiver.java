package de.visionera.androidbuch.amando5.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsMessage;
import android.util.Log;
import de.visionera.androidbuch.amando5.R;
import de.visionera.androidbuch.amando5.common.GeoMarkierung;
import de.visionera.androidbuch.amando5.common.GpsData;
import de.visionera.androidbuch.amando5.gui.Startseite;
import de.visionera.androidbuch.amando5.kontakt.GeoKontakt;

/**
 * @author Arno Becker
 */
public class SmsBroadcastReceiver 
    extends BroadcastReceiver {

  /** Tag zum Loggen in der LogCat. */
  private static final String TAG =
      SmsBroadcastReceiver.class.getSimpleName();
  
  /**
   * Schlüssel zum Erkennen einer
   * Amando-Benachrichtigungs-SMS.
   */
  public static final String AMANDO_SMS_KEY = 
    "amandoSmsKey";
  
  /** 
   * Dummy-Provider um ein Location-Objekt anlegen
   *  zu können. */
  public static final String LOCATION_PROVIDER = 
      "SMS_PROVIDER";

  /** Schlüssel für Flag: Position nachverfolgen. */
  public static final String KEY_NACHVERFOLGEN = 
    "nachverfolgen";

  /**
   * Schlüssel für das Budle mit allen Infos über den
   * Client, der die SMS geschickt hat: Ortsposition,
   * Stichwort, Mobilnummer etc.
   */
  public static final String KEY_CLIENT_INFO = "clientInfo";
  
  /** Nummer der zu erzeugenden Notification. */
  public static final String KEY_NOTIFICATION_NR = 
      "notificationNr";

  /**
   * Id der Notification, die über eingegangene
   * Positionsdaten von einem Client informiert.
   */
  public static final int NOTIFICATION_ID = 12345;

  /** Intent für Daten-SMS. */
  static final String DATEN_SMS_ACTION =
      "android.intent.action.DATA_SMS_RECEIVED";

  /**
   * Port, auf dem die Benachrichtigungs-SMS verschickt
   * wird.
   */
  public static final short DATA_SMS_PORT = 15873;

  /**
   * Methode wird beim Empfang einer SMS aufgerufen.
   * 
   * @param context
   *          Kontext der Anwendung
   * @param intent
   *          Intent, der die SMS enthält
   */
  @Override
  public final void onReceive(final Context context,
      final Intent intent) {
    Log.d(TAG, "onReceive(): Intent = " + 
        intent.getAction());
    Log.d(TAG, "onReceive(): Amando-SMS empfangen.");

    final Bundle bundle = intent.getExtras();
    if (bundle != null) {
      
      final String uricontent = intent.getDataString();
      Log.d(TAG, "verarbeiteDatenSms(): uricontent = " + 
          uricontent);     
      
      // Daten-SMS:
      if (intent.getAction().equals(DATEN_SMS_ACTION)) {
        Log.d(TAG,
            "onReceive(): Amando Daten-SMS empfangen");
        verarbeiteDatenSms(context, intent);
      } else { // Text-SMS:
        Log.d(TAG,
            "onReceive(): Amando Text-SMS empfangen");
        verarbeiteTextSms(context, intent);
      }
    } // ende if
    Log.d(TAG, "onReceive(): Methode beendet...");    
  }
    
  /**
   * Holt die Text-SMS (können mehrere sein) aus dem Intent
   * und isoliert die Absendernummer und den SMS-Text jeder
   * empfangenen SMS.
   * 
   * @param context
   *          Kontext der Anwendung
   * @param intent
   *          Intent, der die SMS enthält
   */
  private void verarbeiteDatenSms(final Context context,
      final Intent intent) {
    // Check: ist dies wirklich eine Amando Daten-SMS?
    final String uricontent = intent.getDataString();
    Log.d(TAG, "verarbeiteDatenSms(): uricontent = " + 
        uricontent);
    final String[] str = uricontent.split(":");
    final String strPort = str[str.length - 1];
    final int port = Integer.parseInt(strPort);

    if (port == DATA_SMS_PORT) {
      final Bundle bundle = intent.getExtras();
      if (bundle != null) {
        // PDU: Protocol Description Unit
        final Object[] pdUnitsAsObjects = (Object[]) bundle
            .get("pdus");
        // Ab Android 1.6: android.telephony.SmsMessage
        // Bis Android 1.5: android.telephony.gsm.SmsMessage
        for (Object pduAsObject : pdUnitsAsObjects) {
          final SmsMessage smsNachricht = SmsMessage
              .createFromPdu((byte[]) pduAsObject);
          if (smsNachricht != null) {
            final String smsText = new String(smsNachricht
                .getUserData());
            if (smsText != null && smsText
                .startsWith(AMANDO_SMS_KEY)) {
              final String absenderMobilnummer = 
                smsNachricht.getOriginatingAddress();

              // Notification mit den Ortsdaten aus dem
              // SMS-Text generieren.
              sendeNotification(context,
                  absenderMobilnummer, smsText);
            }
          }
        } // ende for
      }
    }
  }

  /**
   * Holt die Text-SMS (können mehrere sein) aus dem Intent
   * und isoliert die Absendernummer und den SMS-Text jeder
   * empfangenen SMS.
   * 
   * @param context
   *          Kontext der Anwendung
   * @param intent
   *          Intent, der die SMS enthält
   */
  private void verarbeiteTextSms(final Context context,
      final Intent intent) {
    Log.d(TAG, "onReceive(): Amando Text-SMS empfangen");
    final Bundle bundle = intent.getExtras();
    if (bundle != null) {
      // PDU: Protocol Description Unit
      final Object[] pdUnitsAsObjects = (Object[]) bundle
          .get("pdus");
      // Ab Android 1.6: android.telephony.SmsMessage
      // Bis Android 1.5: android.telephony.gsm.SmsMessage
      for (Object pduAsObject : pdUnitsAsObjects) {
        final SmsMessage smsNachricht = SmsMessage
            .createFromPdu((byte[]) pduAsObject);
        if (smsNachricht != null) {
          final String smsText = new String(smsNachricht
              .getMessageBody());
          if (smsText != null && smsText
              .startsWith(AMANDO_SMS_KEY)) {
            final String absenderMobilnummer = smsNachricht
                .getOriginatingAddress();

            // Notification mit den Ortsdaten aus dem
            // SMS-Text generieren.
            sendeNotification(context, absenderMobilnummer,
                smsText);
          }
        } // ende if
      } // ende for
    }
  }

  /**
   * Parst den SMS-Text und erzeugt ein GpsData-Objekt.
   * Anschließend wird das GpsData-Objekt mit einer
   * Notification verschickt. Die Notification startet die
   * Amando Anwendung. Falls Amando schon läuft, wird die
   * Startseite aufgerufen.
   * 
   * Beispiel SMS-Text:
   * amandoSmsKey#Tolle Kneipe#7.1152637#50.7066272#0.0
   *    #1264464001000#1
   * Schlüssel#Stichwort#Längengrad#Breitengrad#Höhe#
   * Zeitstempel#Nachverfolgen?
   * 
   * @param context
   *          Kontext der Anwendung
   * @param absenderMobilnummer
   *          Mobilnummer des Absenders der SMS
   * @param smsText
   *          Text in der SMS
   */
  public static void sendeNotification(
      final Context context,
      final String absenderMobilnummer,
      final String smsText) {
    Log.d(TAG,
        "sendeNotification(): Mobilnummer des " +
        "Absenders: " + absenderMobilnummer); // Mobilnummer
    Log.d(TAG, "sendeNotification(): SMS-Text: " + 
        smsText);
    final String[] token = smsText.split("#");
    final String stichwort = token[1];
    
    final boolean positionNachverfolgen =
        ("1".equals(token[6])) ? true : false;
    Log.d(TAG,
        "sendeNotification(): Position nachverfolgen: " 
        + positionNachverfolgen);

    final StringBuffer text = new StringBuffer();
    text.append("Absender: ");
    text.append(absenderMobilnummer);

    Log.d(TAG, "sendeNotification(): Notification-Text: " +
        text.toString());

    final Bundle bundle = new Bundle();
    bundle.putString(GeoKontakt.KEY_MOBILNUMMER,
        absenderMobilnummer);
    bundle.putString(GeoMarkierung.KEY_STICHWORT, 
        stichwort);

    final Location location = new Location(LOCATION_PROVIDER);
    location.setLongitude(Double.parseDouble(token[2]));
    location.setLatitude(Double.parseDouble(token[3]));
    location.setAltitude(Double.parseDouble(token[4]));
    location.setTime(Long.parseLong(token[5]));
    bundle.putParcelable(GpsData.KEY_LOCATION, location);

    bundle.putBoolean(KEY_NACHVERFOLGEN,
        positionNachverfolgen);

    final Intent activityIntent = new Intent(context,
        Startseite.class);

    activityIntent.putExtra(KEY_NOTIFICATION_NR,
        NOTIFICATION_ID);

    activityIntent.putExtra(KEY_CLIENT_INFO, bundle);
    
    // Sorgt dafür, dass die Activity neu gestartet wird,
    // falls sie nicht auf dem Activity-Stack liegt:
    activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    // Diese Flag sollte in Verbindung mit 
    // FLAG_ACTIVITY_NEW_TASK verwendet werden. Es sorgt 
    // dafür, dass falls die Anwendung schon läuft, 
    // die Activity trotzdem angezeigt wird. Der
    // Activity-Stack wird geleert und die Activity neu
    // gestartet.
    activityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

    // Der PendingIntent um die Startseite zu starten, wenn
    // der Anwender auf den Intent klickt
    final PendingIntent contentIntent = PendingIntent
        .getActivity(context,
        0, activityIntent, 0);

    Bitmap balloon =
        BitmapFactory.decodeResource(context.getResources(),
            R.drawable.amando_balloon);

    NotificationCompat.Action action =
        new NotificationCompat.Action(R.drawable.logo_klein,
            "Amando", contentIntent);
  
    Notification notification = new NotificationCompat.Builder(context)
        .setContentTitle(stichwort)
        .setContentText(text.toString())
        .setSmallIcon(R.drawable.ic_action_locate)
        .setLargeIcon(balloon) // v11
        .setPriority(Notification.PRIORITY_HIGH) // v16
        .setCategory(Notification.CATEGORY_MESSAGE) // v21
        .addAction(action)
        .setAutoCancel(true)
        .build();

    // Sound:
    notification.defaults |= Notification.DEFAULT_SOUND;

    // Vibration:
    notification.defaults |= Notification.DEFAULT_VIBRATE;
    notification.vibrate = new long[] { 100, 250 };

    // LED bei Notification blinken lassen:
    notification.defaults |= Notification.DEFAULT_LIGHTS;
    notification.ledARGB = 0xff00ff00;
    notification.ledOnMS = 300;
    notification.ledOffMS = 1000;
    notification.flags |= Notification.FLAG_SHOW_LIGHTS;
    notification.flags |= Notification.FLAG_AUTO_CANCEL;

    // Schicke die Notification ab:
    // String-Id als eindeutige Nummer der Notification.
    // Wird gebraucht, um die
    // Notification spaeter zu loeschen:
    final NotificationManager nm = 
        (NotificationManager) context
        .getSystemService(Context.NOTIFICATION_SERVICE);
    nm.notify(NOTIFICATION_ID, notification);

    // Falls die Anwendung schon läuft, wird die
    // Start-Activity angezeigt. Die empfängt die 
    // Ortsposiiton des Clients als Intent:
    context.startActivity(activityIntent);
  }

}
