/**
 * 
 */
package de.visionera.androidbuch.amando5.db;


/**
 * Stellt Zufallsdaten für Dummy-Anwendungen bereit.
 */
public final class SpieldatenGenerator {

  /**
   * Nachnamen für Testdatensätze.
   */
  private static final String[] NACHNAMEN = new String[] {
      //"Winnifred de la Grande Manchande",
      "Berthold Schmitz",
      "Uschi Chantal Schulze",
      "Anneliese Rodriguez-Faltenschneider",
      "Bartolomäus Weissenbaum",
      "Jean Paul Küppers",
      "Berthold Pöttgens",
      "Norbert Stänz",
      "Herbert Winterschneider",
      "Bettina Buttergarten",
      "Rosemarie Gabriele Hinterkeuter"
      };

  
  /**
   * Mobilnummern für Testdatensätze.
   */
  private static final String[] MOBILNUMMERN = 
    new String[] {
    "00418722334455",
    "00491111123456"
    };

  /**
   * Name des Testusers "Simulant".
   */
  public static final String SIMULANT_NAME = 
    "Simon Simulant";
  /**
   * Mobilnummer des Testusers "Simulant".
   */
  public static final String SIMULANT_MOBILNR = 
    "5554";

  /**
   * Liefert einen Namen aus der Menge gültiger
   * Nachnamen zurück.
   * @return beliebiger Eintrag aus NACHNAMEN.
   */
  public static String erzeugeName(int pos) {
    return NACHNAMEN[pos];
  }

  /**
   * Liefert eine Mobilnummer aus der Menge gültiger
   * Nummern zurück.
   * @return beliebiger Eintrag aus MOBILNUMMERN.
   */
  public static String erzeugeMobilnummer() {
    return MOBILNUMMERN[(int) (System
        .currentTimeMillis() % MOBILNUMMERN.length)];
  }

  /**
   * Utilityklasse wird nur statisch genutzt.
   */
  private SpieldatenGenerator() { }
 
}
