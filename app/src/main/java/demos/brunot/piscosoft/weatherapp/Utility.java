package demos.brunot.piscosoft.weatherapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.text.DateFormat;
import java.util.Date;

import demos.brunot.piscosoft.weatherapp.data.WeatherContract;

/**
 * Created by brunot on 12/28/14.
 */
public class Utility {
    public static String getPreferredLocation(Context context){
        SharedPreferences   prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_location_key),
                context.getString(R.string.pref_location_default));
    }

    public static boolean isMetric(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_temperature_unit_key),
                context.getString(R.string.pref_temperature_unit_metric))
                .equals(context.getString(R.string.pref_temperature_unit_metric));
    }

    static String formatTemperature(double temperature, boolean isMetric) {
        double temp;
        if ( ! isMetric ) { // is farenheit
            temp = temperature * 1.8 + 32; //9*temperature/5+32;
        } else {
            temp = temperature;
        }
        return String.format("%.0f", temp);
    }

    static String formatDate(String dateString) {
        Date date = WeatherContract.getDateFromDb(dateString);
        return DateFormat.getDateInstance().format(date);
    }
}
