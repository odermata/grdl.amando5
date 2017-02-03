package de.visionera.androidbuch.amando5.gui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import de.visionera.androidbuch.amando5.R;
import de.visionera.androidbuch.amando5.db.GeoKontaktTbl;
import de.visionera.androidbuch.amando5.kontakt.GeoKontakt;
import de.visionera.androidbuch.amando5.kontakt.GeoKontaktSpeicher;
import de.visionera.androidbuch.amando5.kontakt.GeoKontaktVerzeichnisLoader;
import de.visionera.androidbuch.amando5.kontakt.Sortierung;


public class GeoKontakteAuflisten extends ListActivity 
    implements LoaderManager.LoaderCallbacks<Cursor> {

  private static final String KEY_ADRESSBUCH_URI = "GeoKontakteAuflisten.adressbuchUri";

  /** Kuerzel fuers Logging. */
  private static final String TAG = GeoKontakteAuflisten.
    class.getSimpleName();

  /** Parametername fuer Intent Bundle. */
  static final String IN_PARAM_KONTAKT_ID = "KONTAKT_ID";

  /** Parameter name fuer Intent Bundle. */
  static final String SELECT_KONTAKT = "SELECT_KONTAKT";
  
  /** Rueckgabe aus Kontakt importieren. */
  public static final int ANDROID_KONTAKT_AUSGEWAEHLT = 1;
  public static final int KONTAKT_ERZEUGT = 2;
  public static final int KONTAKT_BEARBEITET = 3;
  
  /** 
   * Spalten des Cursors der Geokontakte, die
   * in der Liste angezeigt werden. 
   */
  private static final String[] ANZEIGE_KONTAKTE = 
    new String[] {
    GeoKontaktTbl.NAME, 
    GeoKontaktTbl.STICHWORT_POS};

  /** IDs im SimpleListView Layout. */
  private static final int[] SIMPLE_LIST_VIEW_IDS = 
    new int[] {
    android.R.id.text1,
    android.R.id.text2 };

  private static final int LOADER_ID_KONTAKTLISTE = 1;
  private static final int LOADER_ID_ADRESSBUCHEINTRAG = 2;

  private LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacks;

  private SimpleCursorAdapter mKontaktAdapter; 

  /** 
   * Zeigt an ob die Activity zur Auswahl eines GeoKontakts
   * aufgerufen wurde.
   */
  private boolean mSelectionMode;
  
  /** Schnittstelle zum persistenten Speicher. */
  private GeoKontaktSpeicher mKontaktSpeicher;
  
  private Sortierung mAktuelleSortierung = Sortierung.STANDARD;
  
  @Override
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    Log.d(TAG, "onCreate(): entered...");
    
    setContentView(R.layout.geokontakte_auflisten);    
    
    final Bundle extras = getIntent().getExtras();
    if (extras != null &&
        extras.containsKey(SELECT_KONTAKT)) {
      mSelectionMode = true;
      setTitle(R.string
          .txt_geokontakt_auflisten_kontaktauswahl);
    } else {
      setTitle(R.string.txt_geokontakt_auflisten_titel);     
    } 
    
    // Allgemeine Form:
    registerForContextMenu(findViewById(android.R.id.list));
    // Abkürzung für ListView:
    //registerForContextMenu(getListView());
    
    ((Spinner) this.findViewById(R.id.sp_sortierung)).
    setOnItemSelectedListener(
        mSpinnerItemAuswahlListener);

    mKontaktSpeicher = new GeoKontaktSpeicher(this);
    
    // Hier bereitet man den CursorAdapter vor. Er wird aber noch nicht
    // an einen Cursor gebunden. Dies geschieht erst, nachdem der Cursor
    // nach einer Datenanfrage zurückgegeben wurde (onLoadFinished).
    // Der letzte Parameter ist ebenfalls 0. Das vermeidet, dass der 
    // Adapter einen eigenen ContentObserver für den Cursor registriert.
    // Diese Aufgabe übernimmt der CursorLoader.
    mKontaktAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2,
        null, ANZEIGE_KONTAKTE, SIMPLE_LIST_VIEW_IDS, 0);
 
    setListAdapter(mKontaktAdapter);
 
    // Referenz auf das Objekt, das sich um die Callbacks nach einer
    // Datenanfrage kümmert. Ist i.A. die Activity oder das aufrufende
    // Fragment.
    mLoaderCallbacks = this;
 
    // Registriert einen Loader mit ID LOADER_ID_KONTAKTLISTE beim LoaderManager.
    // Ab hier übernimmt der Manager die Kontrolle über den Lebenszyklus des Loaders.
    LoaderManager lm = getLoaderManager();
    lm.initLoader(LOADER_ID_KONTAKTLISTE, null, mLoaderCallbacks);        
  }
  
  @Override
  protected void onStart() {
    super.onStart();    
  }
  
  @Override
  protected void onDestroy() {
    getLoaderManager().destroyLoader(LOADER_ID_KONTAKTLISTE);
    mKontaktSpeicher.schliessen();
    super.onDestroy();
  }

  /**
   * Zeige die Liste der GeoKontakte an.
   */
  private void zeigeGeokontakte() {
     getLoaderManager().restartLoader(LOADER_ID_KONTAKTLISTE, null, mLoaderCallbacks);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(
        R.menu.geokontakt_auflisten, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.opt_geokontakt_anlegen: 
        final GeoKontakt neuerKontakt = new GeoKontakt();
        // Neuen Kontakt mit Anfangsdaten versehen
        neuerKontakt.startWerteSetzen();
        final Intent i = new Intent(this,
            GeoKontaktBearbeiten.class);    
        startActivityForResult(i,KONTAKT_ERZEUGT);
        return true;
      
      case R.id.opt_geokontakt_importieren: 
        final Intent intent = 
            new Intent(Intent.ACTION_PICK, 
            ContactsContract.Contacts.CONTENT_URI);
        
        startActivityForResult(intent, 
            ANDROID_KONTAKT_AUSGEWAEHLT);        
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

  @Override
  protected void onListItemClick(ListView l, View v, 
      int position, long id) {
    super.onListItemClick(l, v, position, id);
    
    if (!mSelectionMode) { // Standardfall: 
      final Intent i = new Intent(this, 
          GeoKontaktAnzeigen.class);
      i.putExtra(GeoKontaktAnzeigen.IN_PARAM_KONTAKT_ID, 
          id);
      Log.d(TAG, "Details anzeigen fuer Kontakt " + id);
      startActivity(i);
    } else {
      final Intent intent = new Intent();
      intent.putExtra(IN_PARAM_KONTAKT_ID, id);
      setResult(Activity.RESULT_OK, intent);
      finish();
    }
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    final AdapterView.AdapterContextMenuInfo info = 
      (AdapterView.AdapterContextMenuInfo) item.
      getMenuInfo();
    if (info.id == AdapterView.INVALID_ROW_ID) {
      Log.d(TAG, "Kein Listeneintrag ausgewählt!");
      return false;
    }
    switch (item.getItemId()) {
      case R.id.opt_geokontakt_details: 
        final Intent i = new Intent(
            this, GeoKontaktAnzeigen.class);
        i.putExtra(GeoKontaktAnzeigen.IN_PARAM_KONTAKT_ID, 
            info.id);
        startActivity(i);
        return true;
      case R.id.opt_geokontakt_bearbeiten: 
        final Intent j = new Intent(this, 
            GeoKontaktBearbeiten.class);
        j.putExtra(GeoKontaktBearbeiten.IN_PARAM_KONTAKT_ID,
            info.id);
        startActivityForResult(j,KONTAKT_BEARBEITET);
        return true;
      
      case R.id.opt_geokontakt_loeschen:         
        mKontaktSpeicher.loescheGeoKontakt(info.id);
        getLoaderManager().getLoader(LOADER_ID_KONTAKTLISTE).onContentChanged();
        return true;
      
      default: 
        Log.w(TAG, "unbekannte Option gewaehlt: " + item); 
        return super.onContextItemSelected(item);
    }
  }
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    
    if (v.getId() == android.R.id.list) {
      getMenuInflater().inflate(
          R.menu.geokontakt_auflisten_kontext, menu);
    }
    super.onCreateContextMenu(menu, v, menuInfo);
  }

  
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    Log.d(TAG,"onCreateLoader "+id);
    switch( id ) {
      case LOADER_ID_KONTAKTLISTE: 
        return new GeoKontaktVerzeichnisLoader(this, mKontaktSpeicher, null, mAktuelleSortierung);
        
      case LOADER_ID_ADRESSBUCHEINTRAG:
        Uri adressbuchUri = null;
        if( args != null ) {
          adressbuchUri = Uri.parse(args.getString(KEY_ADRESSBUCH_URI));
        }
        return new CursorLoader(GeoKontakteAuflisten.this, adressbuchUri, null, null, null, null);
    }
    return null;
  }


  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    Log.d(TAG,"onLoadFinished "+loader.getId());
    switch( loader.getId() ) {
      case LOADER_ID_KONTAKTLISTE: 
        // Daten sind geladen. Der Cursor wird an den Adapter gebunden.
        mKontaktAdapter.swapCursor(data);
        break;
      case LOADER_ID_ADRESSBUCHEINTRAG:
        if( data != null ) {
          // Content Provider hat Adressbucheintrag gefunden.
          // Daten werden in amando-db kopiert.
          importiereAusgewaehltenKontakt(data);
        }
        break;
    }
  }


  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    Log.d(TAG,"onLoaderReset "+loader.getId());
    switch( loader.getId() ) {
      case LOADER_ID_KONTAKTLISTE: 
        mKontaktAdapter.swapCursor(null);    
        break;
      case LOADER_ID_ADRESSBUCHEINTRAG:
        
    }
  }
  
  

  
  /**
   * Wenn aufgerufene Intents Daten zurueckgeben landet
   * man hier. 
   * 
   * @param requestCode Vorher übergebener Request-Code
   * @param resultCode In der Sub-Activity gesetzter
   *   Result-Code
   * @param data Von der Sub-Activity übergebene
   *   Ergebnisdaten
   */
  @Override
  protected void onActivityResult(int requestCode, 
      int resultCode, Intent data) {
    Log.d(TAG, "onActivityResult(): entered...");
    if (requestCode == ANDROID_KONTAKT_AUSGEWAEHLT) {
      if (resultCode == RESULT_OK) {  
        Bundle bundle = new Bundle();
        bundle.putString(KEY_ADRESSBUCH_URI,data.getDataString());
        getLoaderManager().restartLoader(LOADER_ID_ADRESSBUCHEINTRAG, bundle , mLoaderCallbacks);
        Log.i(TAG,"suche kontakt in adressbuch");
        return;
      }
    }  
    if (requestCode == KONTAKT_ERZEUGT ||
        requestCode == KONTAKT_BEARBEITET) {
      getLoaderManager().getLoader(LOADER_ID_KONTAKTLISTE).onContentChanged();
    }
  }


  /**
   * Speichert einen im Android Addressbuch ausgewählten
   * Kontakt als neuen Geokontakt.
   * 
   * @param cursor Cursor zeigt auf ausgewählten Kontakt
   */
  private void importiereAusgewaehltenKontakt(Cursor cursor) {

    if (cursor != null && cursor.moveToFirst()) {  
      final GeoKontakt geoKontakt = new GeoKontakt();
      // Besser den LOOKUP-KEY verwenden. 
      // Kontaktdatensynchronisation kann die Id 
      // des Kontakts ändern!
      final String lookupKey = cursor.getString(
          cursor.getColumnIndex(
          ContactsContract.Contacts.LOOKUP_KEY)); 
      geoKontakt.lookupKey = lookupKey;
      // DISPLAY_NAME liefert Vor- und Zuname!
      final String displayName = cursor.getString(
          cursor.getColumnIndex(
          ContactsContract.Contacts.DISPLAY_NAME)); 
      geoKontakt.name = displayName; 
      
      // Mobilnummer ermitteln:
      final String hatTelefonNummer = cursor.getString(
          cursor.getColumnIndex(
            ContactsContract.Contacts.HAS_PHONE_NUMBER)); 
      if ("1".equals(hatTelefonNummer)) {   
        final String kontaktId = cursor.getString(
            cursor.getColumnIndex(
              ContactsContract.Contacts._ID)); 
        // Nur die Mobilnummer ermitteln, falls vorhanden:  
        final String whereKlausel = ContactsContract.
          CommonDataKinds.Phone.CONTACT_ID +
            " = " + kontaktId +
            " and " + ContactsContract.CommonDataKinds.
              Phone.TYPE + // Spalte: DATA2
            " = " + ContactsContract.CommonDataKinds.
              Phone.TYPE_MOBILE; // = 2
        final Cursor phones = getContentResolver().query(
            ContactsContract.CommonDataKinds.Phone.
              CONTENT_URI, null, 
            whereKlausel, 
            null, null);       
        // Es könnte auch mehrere Mobilnummern geben, 
        // nehmen wir die erste... 
        if (phones.moveToNext()) {              
          final String mobilNummer = phones.getString(
              phones.getColumnIndex(
                  ContactsContract.CommonDataKinds.
                  Phone.NUMBER));                
          Log.d(TAG, "onActivityResult(): mobilNummer = " + 
              mobilNummer);
          geoKontakt.mobilnummer = mobilNummer;
        } else { // keine Mobilnummer im Kontakt: Warnung
          keineMobilnummerDialog();
          return;
        }
        phones.close(); 
        
        // Neuen Kontakt mit Anfangsdaten versehen
        geoKontakt.startWerteSetzen();
        // Speichere GeoKontakt in DB:
        mKontaktSpeicher.speichereGeoKontakt(geoKontakt);
        getLoaderManager().getLoader(LOADER_ID_KONTAKTLISTE).onContentChanged();
      }          
    } // end if (cursor.moveToFirst())
    else {
      Log.e(TAG, "Kontakt konnte nicht aus den " +
          "Android-Kontakten importiert werden");
    }
    cursor.close();
  }

  /**
   * Gibt eine Nachricht aus wenn ein zu importiertender
   * Kontakt keine Mobilfunknummer hat.
   */
  private void keineMobilnummerDialog() {
    final AlertDialog.Builder builder = 
      new AlertDialog.Builder(this);
    builder.setMessage(
        "Auswahl nicht möglich! Dieser Kontakt besitzt " +
          "keine Mobilnummer.")
           .setCancelable(false)
           .setPositiveButton("OK", 
               new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, 
                   int id) {
                 dialog.dismiss();
               }
           });
    builder.create().show();
  }
 
  /**
   * Ersetzt nach einer Änderung der Sortierung den
   * Cursor, den die Liste als Grundlage verwendet.
   */
  private AdapterView.OnItemSelectedListener 
      mSpinnerItemAuswahlListener = 
        new AdapterView.OnItemSelectedListener() {
    @Override
    public void onItemSelected(AdapterView<?> arg0,
        View arg1, int position, long id) {

      switch (position) {
        case 0: mAktuelleSortierung = Sortierung.STANDARD;
          zeigeGeokontakte();
        break;
        case 1: mAktuelleSortierung = Sortierung.NAME;
          zeigeGeokontakte();
        break;
        default: Log.w(TAG,"Sortierung nicht geaendert. Unbekannte positionsId "+id);
        break;
      }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
      
    }
  };
  
}
