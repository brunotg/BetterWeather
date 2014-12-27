package demos.brunot.piscosoft.weatherapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.util.Log;

import demos.brunot.piscosoft.weatherapp.data.WeatherContract;
import demos.brunot.piscosoft.weatherapp.data.WeatherDbHelper;

/**
 * Created by brunot on 12/21/14.
 */
public class TestProvider extends AndroidTestCase {

    public static final String LOG_TAG = TestProvider.class.getSimpleName();

    public void testDeleteDb() throws Throwable {
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);

    }

    static public String TEST_CITY_NAME = "North Pole";
    static public String TEST_LOCATION = "99705";
    static public String TEST_DATE = "20141226";

    public void testInsertReadProvider(){

        WeatherDbHelper dbHelper = new WeatherDbHelper(mContext);

        ContentValues locationTestValues = TestDb.getLocationContentValues(TEST_CITY_NAME);

        long locationRowId;
        Uri locationRowUri = mContext.getContentResolver().insert(WeatherContract.LocationEntry.CONTENT_URI, locationTestValues);

        locationRowId = ContentUris.parseId(locationRowUri);
        //db.insert(WeatherContract.LocationEntry.TABLE_NAME, null, locationTestValues);

        //assertTrue( locationRowId != -1 );
        Log.d(LOG_TAG, "new row id: " + locationRowId);

        Cursor queryLocationCursor = mContext.getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                null,
                null,
                null,
                null);

        TestDb.validateCursor(locationTestValues, queryLocationCursor);

        Cursor queryEntryByLocationUri = mContext.getContentResolver().query(
                WeatherContract.LocationEntry.buildLocationUri(locationRowId),
                null,
                null,
                null,
                null);

        TestDb.validateCursor(locationTestValues,queryEntryByLocationUri);

        ContentValues weatherValues = TestDb.getWeatherContentValues(locationRowId);

        Uri weatherRowUri = mContext.getContentResolver().insert(WeatherContract.WeatherEntry.CONTENT_URI, weatherValues);
        long weatherRowId = ContentUris.parseId(weatherRowUri);

        Cursor weatherCursor = mContext.getContentResolver().query(
                WeatherContract.WeatherEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        TestDb.validateCursor(weatherValues, weatherCursor);
        weatherCursor.close();

        Uri u = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(TEST_LOCATION, TEST_DATE );
      weatherCursor =  mContext.getContentResolver().query(
                u,
                null,
                null,
                null,
                null
        );
        TestDb.validateCursor(weatherValues, weatherCursor);
       weatherCursor.close();

        dbHelper.close();
    }

    public void testUpdateLocation(){
        ContentValues LocationValues = TestDb.getLocationContentValues(TEST_CITY_NAME);
        Uri locationUri = mContext.getContentResolver().
                insert(WeatherContract.LocationEntry.CONTENT_URI, LocationValues);
        long locationRowId = ContentUris.parseId(locationUri);
        assertTrue( locationRowId != -1);
        Log.d(LOG_TAG, " new row id: "+ locationRowId);
        ContentValues updatedLocationValues = new ContentValues(LocationValues);
        updatedLocationValues.put(WeatherContract.LocationEntry._ID, locationRowId);
        updatedLocationValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, "Pucallpa");
        int count = mContext.getContentResolver().update(
                WeatherContract.LocationEntry.CONTENT_URI, updatedLocationValues, WeatherContract.LocationEntry._ID + "= ?",
                new String[] { Long.toString(locationRowId) });
        assertEquals(count, 1);

        Cursor locationQueryCursor = mContext.getContentResolver().query(
                WeatherContract.LocationEntry.buildLocationUri(locationRowId),
                null,
                null,
                null,
                null
        );
        TestDb.validateCursor(updatedLocationValues, locationQueryCursor);
    }

    public void testDeleteRecordsAtEnd(){
        deleteAllRecords();
    }

    public void deleteAllRecords(){
        mContext.getContentResolver().delete(
                WeatherContract.WeatherEntry.CONTENT_URI,
                null,
                null
        );
        mContext.getContentResolver().delete(
                WeatherContract.LocationEntry.CONTENT_URI,
                null,
                null
        );
        Cursor  cursor = mContext.getContentResolver().query(
                WeatherContract.WeatherEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals(0, cursor.getCount());
        cursor.close();

        cursor = mContext.getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    public void setUp() {
        deleteAllRecords();
    }


    public void testGetType(){
        String type = mContext.getContentResolver().getType(WeatherContract.WeatherEntry.CONTENT_URI);
        assertEquals(WeatherContract.WeatherEntry.CONTENT_TYPE, type);

        String testLocation = "94102";
        type = mContext.getContentResolver().getType(WeatherContract.WeatherEntry.buildWeatherLocation(testLocation));
        assertEquals(WeatherContract.WeatherEntry.CONTENT_TYPE, type);

        String testDate = "20141223";
        type = mContext.getContentResolver().getType(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(testLocation, testDate));

        assertEquals(WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE, type);

        type = mContext.getContentResolver().getType(WeatherContract.LocationEntry.CONTENT_URI);
        assertEquals(WeatherContract.LocationEntry.CONTENT_TYPE, type);

        type = mContext.getContentResolver().getType(WeatherContract.LocationEntry.buildLocationUri(1L));
        assertEquals(WeatherContract.LocationEntry.CONTENT_ITEM_TYPE, type);
    }
}
