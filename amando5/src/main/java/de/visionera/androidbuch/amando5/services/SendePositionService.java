package de.visionera.androidbuch.amando5.services;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import de.visionera.androidbuch.amando5.NetzwerkKonfigurator;
import de.visionera.androidbuch.amando5.common.GpsData;
import de.visionera.androidbuch.amando5.kontakt.GeoKontakt;
import de.visionera.androidbuch.amando5.kontakt.GeoPosition;
import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

/**
 * Der Service dient zum Versenden der eigenen Geoposition 
 * an den Amando-Server. Nach dem
 * Verschicken einer SMS ("Position senden") mit den
 * Geo-Koordinaten, baut dieser Service eine Verbindung zum
 * Server auf und schickt jedesmal, wenn sich die Position
 * signifikant ändert, eine neue Position zum Server. Als
 * Schlüssel zur Identifizierung des Positionssenders dient
 * die eigene Mobilnummer. 
 * 
 * Dieser Service ist ein Beispiel für die Implementierung 
 * eines IntentService.
 * 
 * @author Arno Becker, 2015 visionera GmbH
 */
public class SendePositionService extends IntentService {
  
  /** Tag für die LogCat. */
  private static final String TAG = 
      SendePositionService.class.getSimpleName();
  
  private static GeoPosition mPosition;
  
  /** URL für die HTTP-Verbindung zum Server. */
  private String mUrlString = "http://" 
      + NetzwerkKonfigurator.SERVER_IP + ":" 
      + NetzwerkKonfigurator.HTTP_PORTNUM 
      + "/amandoserver/GeoPositionsService";
  
  public SendePositionService() {
    super(TAG);    
  }
  
  /** 
   * Sende die eigene aktuelle Position an den Server. 
   * 
   * @param position Aktuelle eigene Position.
   */
  @Override
  protected void onHandleIntent(Intent intent) {
    Log.d(TAG, "onHandleIntent(): entered. " 
        + "Sende eigene Position an Amando-Server");
    
    sendeEigenePosition();     
    Log.d(TAG, "onHandleIntent(): leave...");
  } 
  
  /**
   * Versendet die eigene aktuelle Position via HTTP. Die
   * Implementierung nutzt den Apache HttpClient.
   * 
   * @param position
   *          aktuelle eigene Position
   */
  private void sendeEigenePosition() {
    Log.d(TAG, "_sendePosition(): URL: " + mUrlString);

    final DefaultHttpClient client = 
      new DefaultHttpClient();
    final HttpPost httpPost = new HttpPost(mUrlString);

    final List<NameValuePair> postParameters = 
      new ArrayList<NameValuePair>();
    postParameters.add(
        new BasicNameValuePair(GeoKontakt.KEY_MOBILNUMMER,
            mPosition.mMobilnummer));
    postParameters.add(
        new BasicNameValuePair(GpsData.KEY_LAENGENGRAD,
        String.valueOf(mPosition.mGpsData.getLaengengrad())));
    postParameters.add(
        new BasicNameValuePair(GpsData.KEY_BREITENGRAD,
        String.valueOf(mPosition.mGpsData.getBreitengrad())));
    postParameters.add(
        new BasicNameValuePair(GpsData.KEY_HOEHE,
        String.valueOf(mPosition.mGpsData.getHoehe())));
    postParameters.add(
        new BasicNameValuePair(GpsData.KEY_ZEITSTEMPEL,
        String.valueOf(mPosition.mGpsData.getZeitstempel())));
    try {
      httpPost.setEntity(new UrlEncodedFormEntity(
          postParameters));
    } catch (UnsupportedEncodingException e2) {
      Log.e(TAG, e2.getMessage());
    }

    try {
      Log.d(TAG, "_sendePosition(): Request abschicken...");
      client.execute(httpPost);      
    } catch (ClientProtocolException e1) {
      Log.e(TAG, e1.getMessage());
    } catch (IOException e1) {
      Log.e(TAG, e1.getMessage());
    }
    
    Log.d(TAG, "_sendePosition(): leave...");
  }
  
  public static void setPosition(GeoPosition position) {
    mPosition = position;
  }
}
