package de.visionera.androidbuch.amando5.common;

import java.util.List;

import android.telephony.SmsManager;
import android.util.Log;
import de.visionera.androidbuch.amando5.receiver.SmsBroadcastReceiver;

/**
 * @author Arno Becker, 2010 visionera GmbH
 */
public final class AmandoSmsUtil {

  /** Tag für die LogCat. */
  private static final String TAG = "AmandoSmsUtil";

  /** SMS soll als Daten-SMS verschickt werden. */
  public static final int TYP_DATEN_SMS = 1;
  
  /** SMS soll als Text-SMS verschickt werden. */
  public static final int TYP_TEXT_SMS = 2;

  /**
   * Port, auf dem die Benachrichtigungs-SMS verschickt
   * wird.
   */
  public static final short SMS_DATEN_PORT = 15873;

  /** 
   * Utility-Klasse soll nicht instanziiert werden können. 
   */
  private AmandoSmsUtil() { }

  /**
   * 
   * @param empfaengerMobilnummern Liste mit den 
   *   Mobilnummern des Empfängers.
   * @param geoMarkierung Aktuelle eigene Position
   * @param positionNachverfolgen Einmalige 
   *   Positionsmeldung, oder werden ab jetzt fortlaufend 
   *   die Positionsänderungen an den Amando-Server
   *   übertragen?
   * @param smsType Daten- oder Text-SMS
   */
  public static void sendeSms(
      final List<String> empfaengerMobilnummern,
      final GeoMarkierung geoMarkierung,
      final boolean positionNachverfolgen,
      int smsType) {
    final String smsText = erzeugeSmsText(geoMarkierung,
        positionNachverfolgen);
    for (String mobilnummer : empfaengerMobilnummern) {
      Log.d(TAG, "sendeSms(): Mobilnummer: " + 
          mobilnummer);
      Log.d(TAG, "sendeSms(): SMS-Text: " + smsText);
      switch (smsType) {
        case TYP_DATEN_SMS:
          sendeDatenSms(mobilnummer, smsText);
          break;
        case TYP_TEXT_SMS:
          sendeTextSms(mobilnummer, smsText);
          break;
        default:
          break;
      }
    }
  }

  /**
   * Sendet eine Daten-SMS.
   * 
   * @param mobilnummer Empfänger-Mobilnummer.
   * @param smsText Text der SMS mit der Ortsposition.
   */
  private static void sendeDatenSms(
      final String mobilnummer, final String smsText) {
    // Android 2.x: android.telephony.SmsManager
    final SmsManager smsManager = SmsManager.getDefault();

    final byte[] udh = smsText.getBytes();
    Log.d(TAG, "sendeDatenSms(): sende Daten-SMS an " +
        mobilnummer + ". Port: " + SMS_DATEN_PORT);
    // Tipp: fuege PendingIntents fuer die Rueckmeldung, ob
    // die SMS korrekt
    // verschickt wurde hinzu
    smsManager.sendDataMessage(mobilnummer, null,
        SMS_DATEN_PORT,
        udh, null, null);
  }

  /**
   * Sendet eine Text-SMS.
   * 
   * @param mobilnummer Empfänger-Mobilnummer.
   * @param smsText Text der SMS mit der Ortsposition.
   */
  private static void sendeTextSms(
      final String mobilnummer, final String smsText) {
    // Android 2.x: android.telephony.SmsManager
    final SmsManager smsManager = SmsManager.getDefault();

    Log.d(TAG, "sendeTextSms(): Sende Text-SMS an die " +
        "Mobilnummer");
    smsManager.sendTextMessage(mobilnummer, null,
        smsText, null, null);
  }
  
  /**
   * Erzeugt SMS-Text. Beispiel: amandoSmsKey#Tolle
   * Kneipe#7.1152637#50.7066272#0.0#1264464001000#1
   * schlüssel...#Stichwort...#Längengrad#Breitengrad#Höhe#
   * Zeitstempel.#Nachverfolgen?
   * 
   * @param geoMarkierung Aktuelle eigene Position.
   * @param positionNachverfolgen Einmalige 
   *   Positionsmeldung, oder werden ab jetzt fortlaufend 
   *   die Positionsänderungen an den Amando-Server
   *   übertragen?
   * @return Text der SMS.
   */
  private static String erzeugeSmsText(
      final GeoMarkierung geoMarkierung,
      final boolean positionNachverfolgen) {
    final StringBuilder smsTextBuilder = 
      new StringBuilder();
    smsTextBuilder.append(SmsBroadcastReceiver.AMANDO_SMS_KEY);
    smsTextBuilder.append("#");
    if (geoMarkierung.stichwort != null) {
      smsTextBuilder.append(geoMarkierung.stichwort
          .replaceAll("#", " "));
    }
    smsTextBuilder.append("#");
    smsTextBuilder
        .append(geoMarkierung.gpsData.location.getLongitude());
    smsTextBuilder.append("#");
    smsTextBuilder
        .append(geoMarkierung.gpsData.location.getLatitude());
    smsTextBuilder.append("#");
    smsTextBuilder.append(geoMarkierung.gpsData.location.getAltitude());
    smsTextBuilder.append("#");
    smsTextBuilder
        .append(geoMarkierung.gpsData.location.getTime());
    smsTextBuilder.append("#");
    smsTextBuilder
        .append(positionNachverfolgen ? "1" : "0");

    Log.d(TAG, "erzeugeSmsText(): Länge der SMS: "
        + smsTextBuilder.toString().length());
    Log.d(TAG, "erzeugeSmsText(): SMS-Text: "
        + smsTextBuilder.toString());

    return smsTextBuilder.toString();
  }

  /**
   * Parsen des SMS-Textes. Die SMS wurde an den Client
   * geschickt, damit dieser die Position des SMS-Senders
   * kennt, bzw. nachverfolgen kann. Beispiel SMS-Text:
   * amandoSmsKey#Tolle
   * Kneipe#7.1152637#50.7066272#0.0#1264464001000#1
   * schlüssel...#Stichwort...#Längengrad#Breitengrad#Höhe#
   * Zeitstempel.#Nachverfolgen?
   * 
   * @param geoMarkierung
   *          zu fuellendes GeoMarkierung-Objekt
   * @param smsText
   *          der zu parsende SMS-Text
   * @return positionNachverfolgen-Flag
   */
  /*
   * public static boolean parseSmsText(GeoMarkierung
   * geoMarkierung, final String smsText) throws
   * IllegalArgumentException { String[] token =
   * smsText.split("#"); if (token.length != 7) { throw new
   * IllegalArgumentException
   * ("SMS-Text konnte nicht geparst werden: " + smsText); }
   * geoMarkierung.stichwort = token[1];
   * 
   * GpsData gpsData = new
   * GpsData(Double.parseDouble(token[2]),
   * Double.parseDouble(token[3]),
   * Double.parseDouble(token[4]),
   * Long.parseLong(token[5])); geoMarkierung.gpsData =
   * gpsData;
   * 
   * return ("1".equals(token[6])) ? true : false; }
   */

}
