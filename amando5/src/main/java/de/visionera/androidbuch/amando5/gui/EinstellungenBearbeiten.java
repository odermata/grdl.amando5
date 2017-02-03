/**
 * 
 */
package de.visionera.androidbuch.amando5.gui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import de.visionera.androidbuch.amando5.R;

/**
 * Zeigt die Liste der verfügbaren Programmeinstellungen und
 * ihre Werte an. Es besteht die Möglichkeit, die
 * Einstellungen zu ändern und zu speichern.
 * 
 * @author Marcus Pant, Arno Becker, 2015 visionera GmbH
 */
public class EinstellungenBearbeiten extends
    PreferenceActivity {
  
  /** Kuerzel fuers Logging. */
  public static final String EINSTELLUNGEN_NAME =
      EinstellungenBearbeiten.class.getSimpleName();

  /** Menueoption: Bearbeiten. */
  private static final int EINSTELLUNG_BEARBEITEN_ID =
      Menu.FIRST;
  
  /** Menueoption: Zurück. */
  private static final int ZURUECK_ID = Menu.FIRST + 1;
  
  /** Menueoption: Beenden. */
  private static final int AMANDO_BEENDEN_ID =
      Menu.FIRST + 2;

  @Override
  @SuppressLint("NewApi")
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    
    // Deprecated, da ab Android 3.0 PreferenceFragments
    // verwendet werden sollen. Da wir aber hier noch
    // keine Compatibility Library einsetzen, muss diese
    // Methode verwendet werden.
    this.addPreferencesFromResource(
        R.xml.amando_einstellungen);
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    menu.add(0, EINSTELLUNG_BEARBEITEN_ID, Menu.NONE,
        R.string.men_einstellungen_bearbeiten);
    menu.add(0, ZURUECK_ID, Menu.NONE,
        R.string.men_zurueck);
    menu.add(0, AMANDO_BEENDEN_ID, Menu.NONE,
        R.string.men_amando_beenden);

    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case AMANDO_BEENDEN_ID:
        finish();
        return true;
      default:
        Log.w(EINSTELLUNGEN_NAME,
            "unbekannte Option gewaehlt: " + item);
        return super.onOptionsItemSelected(item);
    }
  }
}
