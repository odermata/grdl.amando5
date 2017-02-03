package de.visionera.androidbuch.amando5;

/**
 * Konfigurationsklasse für die Netzwerkeinstellungen.
 * 
 * Wird vom NetzwerkService für die Kommunikation mit 
 * dem Amando-Server gebraucht.
 * 
 * 
 * 
 * @author Arno Becker, 2015 visionera GmbH
 *
 */
public final class NetzwerkKonfigurator {

  /**
   * IP-Adresse des Amando Servers, wenn die visionera- 
   * Serverinstallation verwendet wird.
   */
  public static final String SERVER_IP = 
    "176.9.8.92";
      
  /**
   * Portnummer des Amando Servers, wenn die visionera- 
   * Serverinstallation verwendet wird.
   */
  public static final int SOCKET_PORTNUM = 9380;

    /**
   * HTTP-Portnummer des Amando Servers, wenn die 
   * visionera-Serverinstallation verwendet wird.
   */
  public static final int HTTP_PORTNUM = 8082;
  
//  /** 
//   * IP-Adresse des Amando Servers, wenn eine lokale 
//   * Serverinstallation verwendet wird.
//   */
//  private static final String SERVER_IP = 
//    "10.0.2.2"; 
//  
//  /** 
//   * Portnummer des Amando Servers, wenn eine lokale 
//   * Serverinstallation verwendet wird.
//   */
//  private static final int SOCKET_PORTNUM = 9380; 
//  
//  /**
//   * HTTP-Portnummer des Amando Servers, wenn eine lokale 
//   * Serverinstallation verwendet wird.
//   */
//  private static final int HTTP_PORTNUM = 8082;
  
}
