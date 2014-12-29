package demos.brunot.piscosoft.weatherapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Vector;

import demos.brunot.piscosoft.weatherapp.data.WeatherContract;

/**
 * Created by brunot on 12/28/14.
 */

public class FetchWeatherTask extends AsyncTask<String, Void, Void> {

    private SimpleCursorAdapter mForecastAdapter;
    private boolean DEBUG = true;
    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
    String[] allData;
    private Context mContext = null;

    public FetchWeatherTask(Context context, SimpleCursorAdapter forecastAdapter){
        mContext = context;
        mForecastAdapter = forecastAdapter;
    }

    private String getReadableDateString(long time){
        Date date = new Date(time * 1000);
        SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
        return format.format(date).toString();
    }

    private String formatHighLows(double high, double low) {
        // For presentation, assume the user doesn't care about tenths of a degree.

        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        String highLowStr = roundedHigh + "/" + roundedLow;
        return highLowStr;
    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays,
                                            String locationSetting)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.

        // Location information
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";
        final String OWM_COORD_LAT = "lat";
        final String OWM_COORD_LONG = "lon";

        // Weather information.  Each day's forecast info is an element of the "list" array.
        final String OWM_LIST = "list";

        final String OWM_DATETIME = "dt";
        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";

        // All temperatures are children of the "temp" object.
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";

        final String OWM_WEATHER = "weather";
        final String OWM_DESCRIPTION = "main";
        final String OWM_WEATHER_ID = "id";

        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
        String cityName = cityJson.getString(OWM_CITY_NAME);
        JSONObject coordJSON = cityJson.getJSONObject(OWM_COORD);
        double cityLatitude = coordJSON.getLong(OWM_COORD_LAT);
        double cityLongitude = coordJSON.getLong(OWM_COORD_LONG);

        long locationId  = addLocation(locationSetting, cityName, cityLatitude, cityLongitude );

        Log.v(LOG_TAG, cityName + ", with coord: " + cityLatitude + " " + cityLongitude);

        Vector<ContentValues> cVVector = new Vector<ContentValues>(weatherArray.length());

        // The function referenced here is not yet implemented, so we've commented it out for now.
//        long locationID = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);

        String[] resultStrs = new String[numDays];
        for(int i = 0; i < weatherArray.length(); i++) {
            // For now, using the format "Day, description, hi/low"
            String day;
            String description;
            String highAndLow;
            double pressure;
            int humidity;
            double windSpeed;
            double windDirection;
            int weatherId;

            // Get the JSON object representing the day
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            // The date/time is returned as a long.  We need to convert that
            // into something human-readable, since most people won't read "1400356800" as
            // "this saturday".
            long dateTime = dayForecast.getLong(OWM_DATETIME);
            day = getReadableDateString(dateTime);

            pressure = dayForecast.getDouble(OWM_PRESSURE);
            humidity = dayForecast.getInt(OWM_HUMIDITY);
            windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
            windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

            // description is in a child array called "weather", which is 1 element long.
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);
            weatherId = weatherObject.getInt(OWM_WEATHER_ID);

            // Temperatures are in a child object called "temp".  Try not to name variables
            // "temp" when working with temperature.  It confuses everybody.
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            double high = temperatureObject.getDouble(OWM_MAX);
            double low = temperatureObject.getDouble(OWM_MIN);

            ContentValues weatherValues = new ContentValues();

            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATETEXT,
                    WeatherContract.getDbDateString(new Date(dateTime * 1000L)));
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);
            cVVector.add(weatherValues);

            highAndLow = formatHighLows(high, low);
            resultStrs[i] = day + " - " + description + " - " + highAndLow;
        }

        if (cVVector.size() > 0 ){
            ContentValues[] cvArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cvArray);
            int rowsInserted = mContext.getContentResolver()
                    .bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, cvArray);
            Log.v(LOG_TAG, "inserted "+ rowsInserted + " rows of weather data");
            if ( DEBUG ){
                Cursor weatherCursor = mContext.getContentResolver().query(
                        WeatherContract.WeatherEntry.CONTENT_URI,
                        null,
                        null,
                        null,
                        null
                );
                if (weatherCursor.moveToFirst()){
                    ContentValues resultValues = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues(weatherCursor, resultValues);
                    Log.v(LOG_TAG, "query succesful !");
                    logContentValues(resultValues);
                }
                else {
                    Log.v(LOG_TAG, "query failed !");
                }
            }
        }
        return resultStrs;
    }

    private void logContentValues(ContentValues contentValues){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
            for (Map.Entry<String, Object> item : contentValues.valueSet()){
                String key =  item.getKey();
                Log.v(LOG_TAG, key + " : " + contentValues.getAsString(key));
            }
        }
        else {
            for (String key : contentValues.keySet()){
                Log.v(LOG_TAG, key + " : " + contentValues.getAsString(key));
            }
        }
    }

    @Override
    protected Void doInBackground(String... params) {
        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.

        if (params.length == 0)
        {
            return null;
        }

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String format = "json";
        int numDays = 14;

        String locationQuery = params[0];
        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast

            final String FORECAST_BASE_URL =
                    "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String QUERY_PARAM = "q";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";
            Uri builtUri = Uri.parse( FORECAST_BASE_URL ).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, params[0])
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, params[1])
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                    .build();

            Log.d(LOG_TAG, "Ready to connect using "+ builtUri.toString());
            URL url = new URL(builtUri.toString());
            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }

            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            forecastJsonStr =  buffer.toString();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        try {
            String[] result = getWeatherDataFromJson(forecastJsonStr, numDays, locationQuery);
            allData = result;
        }
        catch (JSONException exception){
            Log.e(LOG_TAG, exception.getMessage(), exception);
            exception.printStackTrace();

        }
        return null;
    }

    private long addLocation(
            String locationSetting, String cityName, double lat, double lon){

        Log.v(LOG_TAG, "inserting " + cityName + ", with coord: "+ lat +  ", " + lon);
        Cursor cursor = mContext.getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                new String[] { WeatherContract.LocationEntry._ID },
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[] { locationSetting },
                null );
        if ( cursor.moveToFirst()) {
            Log.v(LOG_TAG, "Found it in the database ! ");
            int locationIdIndex = cursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            return cursor.getLong(locationIdIndex);
        }
        else {
            Log.v(LOG_TAG, "Didn't find it in the databse, inserting now! ");
            ContentValues locationValues = new ContentValues();
            locationValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);

            Uri locationInsertUri = mContext.getContentResolver()
                    .insert(WeatherContract.LocationEntry.CONTENT_URI, locationValues);
            return ContentUris.parseId(locationInsertUri);
        }

    }
}