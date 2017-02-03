package de.visionera.androidbuch.amando5.gui;

import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import de.visionera.androidbuch.amando5.R;

/**
 * @author Arno Becker, 2015 visionera GmbH
 */
public class HilfeAnzeigen extends Activity {

  /** Kuerzel fuers Logging. */
  private static final String TAG = HilfeAnzeigen.class
      .getSimpleName();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.v(TAG, "onCreate(): entered...");
    setContentView(R.layout.hilfe_anzeigen);

    final WebView view = 
      (WebView) findViewById(R.id.webview);
    view.getSettings().setJavaScriptEnabled(true);
    
    // dies hier ginge beispielsweise auch:
    // view.loadUrl("http://www.visionera.de");
    
    initialisiereWebKit(view, this);
    view.bringToFront();
  }
  
  /**
   * Initialisiert WebKit mit einer HTML-Seite aus dem
   * Ressourcen-Ordner.
   * 
   * @param view WebView zur Darstellung von 
   *     Webinhalten.
   * @param context Kontext der Anwendung
   */
  private void initialisiereWebKit(
      final WebView view, final Context context) {
    final String mimetype = "text/html";
    final String encoding = "UTF-8";
    String htmldata;
    
    final int contextMenueId = R.raw.hilfe_komplett;    
    final InputStream is = context.getResources()
        .openRawResource(contextMenueId);

    try {
      if (is != null && is.available() > 0) {
        final byte[] bytes = new byte[is.available()];
        is.read(bytes);
        htmldata = new String(bytes);
        view.loadDataWithBaseURL(null, htmldata, mimetype,
            encoding, null);
      }
    } catch (IOException e) { }
  }

}
