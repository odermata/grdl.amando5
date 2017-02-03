/**
 * 
 */
package de.visionera.asyncdb;

import android.content.ContentValues;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

/**
 *
 */
public class SqlInsertTask extends AsyncTask<Void, Void, Long> {

	private static final String TAG = "SqlInsertTask";

	private SQLiteDatabase mDb;
	private String mTable;
	private String mNullColumnHack;
	private ContentValues mValues;
	private Loader<Cursor> mLoader = null;

	/**
	 * Constructor if we don't need to notify a Loader of the change.
	 * 
	 * @param db
	 * @param table
	 * @param nullColumnHack
	 * @param values
	 */
	public SqlInsertTask(SQLiteDatabase db, String table,
			String nullColumnHack, ContentValues values) {
		this(db,table,nullColumnHack,values,null);
	}

	/**
	 * Constructor to add a Loader that should be notified, if the change has been successful.
	 * @param db
	 * @param table
	 * @param nullColumnHack
	 * @param values
	 * @param loader
	 */
	public SqlInsertTask(SQLiteDatabase db, String table,
			String nullColumnHack, ContentValues values, Loader<Cursor> loader) {
		this.mDb = db;
		this.mTable = table;
		this.mNullColumnHack = nullColumnHack;
		this.mValues = values;
		this.mLoader = loader;
	}

	@Override
	protected Long doInBackground(Void... params) {
		try {
			return mDb.insertOrThrow(mTable, mNullColumnHack, mValues);
		} catch (Exception e) {
			Log.e(TAG, "Unable to insert data.", e);
			return null;
		}
	}

	@Override
	protected void onPostExecute(Long result) {
		super.onPostExecute(result);
		if (result != null && result > 0) {
			Log.i(TAG, "Successfully added row with id=" + result
					+ " to table " + mTable);
			if (mLoader != null) {
				mLoader.onContentChanged();
			}
		}
	}
}
