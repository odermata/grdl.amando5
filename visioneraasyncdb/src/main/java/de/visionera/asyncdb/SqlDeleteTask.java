/**
 * 
 */
package de.visionera.asyncdb;

import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

/**
 *
 */
public class SqlDeleteTask extends AsyncTask<Void, Void, Integer> {

	private static final String TAG = "SqlDeleteTask";

	private SQLiteDatabase mDb;
	private String mTable;
	private String mSelection;
	private String[] mSelectionArgs;
	private Loader<Cursor> mLoader = null;

	/**
	 * Constructor if we don't need to notify a Loader of the change.
	 * 
	 * @param db
	 * @param table
	 * @param selection
	 * @param selectionArgs
	 */
	public SqlDeleteTask(SQLiteDatabase db, String table,
			String selection, String[] selectionArgs) {
		this(db,table,selection,selectionArgs,null);
	}

	/**
	 * Constructor to add a Loader that should be notified, if the change has
	 * been successful.
	 * 
	 * @param db
	 * @param table
	 * @param selection
	 * @param selectionArgs
	 * @param loader
	 */
	public SqlDeleteTask(SQLiteDatabase db, String table,
			String selection, String[] selectionArgs, Loader<Cursor> loader) {
		mDb = db;
		mTable = table;
		mSelection = selection;
		mSelectionArgs = selectionArgs;
		mLoader = loader;
	}

	@Override
	protected Integer doInBackground(Void... params) {
		try {
			return mDb.delete(mTable, mSelection, mSelectionArgs);
		} catch (Exception e) {
			Log.e(TAG, "Unable to delete data.", e);
			return null;
		}
	}

	@Override
	protected void onPostExecute(Integer result) {
		super.onPostExecute(result);
		if (result != null && result > 0) {
			Log.i(TAG, "Successfully deleted "+result+" rows from table " + mTable);
			if (mLoader != null) {
				mLoader.onContentChanged();
			}
		} else {
			Log.i(TAG,"No rows deleted.");
		}
		
	}
}
