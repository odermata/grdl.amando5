package de.visionera.androidbuch.amando5.kontakt;

import android.content.Context;
import android.database.Cursor;
import de.visionera.asyncdb.AbstractCursorLoader;

/**
 * Loader speziell für die Liste von Geokontakten. 
 */
public class GeoKontaktVerzeichnisLoader extends AbstractCursorLoader {

	private GeoKontaktSpeicher mDao;

	private CharSequence mNamensFilter;
	private Sortierung mSortierung = Sortierung.STANDARD;

	public GeoKontaktVerzeichnisLoader(Context context) {
		super(context);
		mDao = new GeoKontaktSpeicher(context);
	}

	/**
	 * 
	 * @param context Context der aufrufenden Android-Komponente (z.B. Activity).
	 * @param dao
	 * @param namensFilter optionaler Filter zur Beschraenkung der Ergebnismenge.
	 * @param sortierung optionales Sortierkritierum. 
	 */
	public GeoKontaktVerzeichnisLoader(Context context, GeoKontaktSpeicher dao, CharSequence namensFilter, Sortierung sortierung) {
		super(context);
		mDao = dao;
		mNamensFilter = namensFilter;
		mSortierung = sortierung;
	}

	 public GeoKontaktVerzeichnisLoader(Context context, GeoKontaktSpeicher dao) {
	   this(context,dao,null,Sortierung.STANDARD);
	 }
	
	@Override
	protected Cursor loadCursorData() {
		Cursor cursor = mDao.ladeGeoKontaktListe(mSortierung, mNamensFilter);
		return cursor;
	}
}
