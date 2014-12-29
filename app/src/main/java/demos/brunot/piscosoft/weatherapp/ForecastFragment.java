package demos.brunot.piscosoft.weatherapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Date;

import demos.brunot.piscosoft.weatherapp.data.WeatherContract;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    private String mLocation;
    private static final int FORECAST_LOADER = 0; // each loader has an id, in case you want to have multiple loaders
    private final String LOG_TAG = ForecastFragment.class.getSimpleName();
    //ArrayAdapter<String> mForecastAdapter;
    SimpleCursorAdapter mForecastAdapter;

    private static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATETEXT,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING
    };

    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_DESC = 2;
    public static final int COL_WEATHER_MAX_TEMP = 3;
    public static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_LOCATION_SETTING = 5;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String startDate = WeatherContract.getDbDateString(new Date());
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATETEXT + " ASC";
        mLocation = Utility.getPreferredLocation(getActivity());
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(mLocation, startDate);
        Log.d("ForecastFragment ", "URI : " + weatherForLocationUri.toString());

        return new CursorLoader(
                getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mForecastAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mForecastAdapter.swapCursor(null);
    }

    public ForecastFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
    }

    @Override
    public void onStart(){
        updateWeatherData();
        super.onStart();
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        //fetchData();
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    public void updateWeatherData(){
        FetchWeatherTask fetchWeatherTask = new FetchWeatherTask(getActivity(), mForecastAdapter);
        //"94102,usa"
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = prefs.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
        String units = prefs.getString(getString(R.string.pref_temperature_unit_key), getString(R.string.pref_temperature_unit_metric));
        fetchWeatherTask.execute(location, units);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if (id == R.id.action_refresh){
            updateWeatherData();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

         mForecastAdapter = new SimpleCursorAdapter(
                 getActivity(),
                 R.layout.list_item_forecast,
                 null,
                 new String[] { WeatherContract.WeatherEntry.COLUMN_DATETEXT,
                         WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
                         WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                         WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
                 },
                 new int[] { R.id.list_item_date_textview,
                 R.id.list_item_forecast_textview,
                 R.id.list_item_high_textview,
                 R.id.list_item_low_textview },
                 0
         );

        mForecastAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                boolean isMetric = Utility.isMetric(getActivity());
                switch (columnIndex) {
                    case COL_WEATHER_MAX_TEMP:
                    case COL_WEATHER_MIN_TEMP: {
                        // we have to do some formatting and possibly a conversion
                        ((TextView) view).setText(Utility.formatTemperature(
                                cursor.getDouble(columnIndex), isMetric));
                        return true;
                    }
                    case COL_WEATHER_DATE: {
                        String dateString = cursor.getString(columnIndex);
                        TextView dateView = (TextView) view;
                        dateView.setText(Utility.formatDate(dateString));
                        return true;
                    }
                }
                return false;
            }
        });
        ListView listViewForecast = (ListView) rootView.findViewById(
                R.id.list_view_forecast);

        listViewForecast.setAdapter(mForecastAdapter);
        listViewForecast.setOnItemClickListener( new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent weatherDetailIntent = new Intent(getActivity(), WeatherDetailActivity.class);
                weatherDetailIntent.putExtra(Intent.EXTRA_TEXT, "placeholdeer");
                //weatherDetailIntent.setData("test");
                startActivity(weatherDetailIntent);

                /**
                Toast toast = Toast.makeText(getActivity(), mForecastAdapter.getItem(position), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP|Gravity.CENTER, 0, 0);
                toast.show();
                 **/
            }
        });

        return rootView;
    }
}