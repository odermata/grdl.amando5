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
public class SqlUpdateTask extends AsyncTask<Void, Void, Integer> {

	private static final String TAG = "SqlUpdateTask";

	private SQLiteDatabase mDb;
	private String mTable;
	private ContentValues mValues;
	private String mSelection;
	private String[] mSelectionArgs;
	private Loader<Cursor> mLoader = null;

	/**
	 * Constructor if we don't need to notify a Loader of the change.
	 * 
	 * @param db A writable db connection.
	 * @param table
	 * @param selection
	 * @param selectionArgs
	 * @param values
	 */
	public SqlUpdateTask(SQLiteDatabase db, String table,
			ContentValues values,String selection, String[] selectionArgs) {
		this(db,table,values,selection,selectionArgs,null);
	}

	/**
	 * Constructor to add a Loader that should be notified, if the change has been successful.
	 * @param db
	 * @param table
	 * @param selection
	 * @param selectionArgs
	 * @param values
	 * @param loader
	 */
	public SqlUpdateTask(SQLiteDatabase db, String table,
			ContentValues values, String selection, String[] selectionArgs, Loader<Cursor> loader) {
		mDb = db;
		mTable = table;
		mValues = values;
		mSelection = selection;
		mSelectionArgs = selectionArgs;
		mLoader = loader;
	}

	@Override
	protected Integer doInBackground(Void... params) {
		try {
			return mDb.update(mTable, mValues, mSelection, mSelectionArgs);
		} catch (Exception e) {
			Log.e(TAG, "Unable to update data.", e);
			return null;
		}
	}

	@Override
	protected void onPostExecute(Integer result) {
		super.onPostExecute(result);
		if (result != null && result > 0) {
			Log.i(TAG, "Successfully changed "+result+" rows in table " + mTable);
			if (mLoader != null) {
				mLoader.onContentChanged();
			}
		} else {
			Log.i(TAG,"No rows changed.");
		}
	}
}
