package demos.brunot.piscosoft.weatherapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity {
    private final String LOG_TAG = ForecastFragment.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new ForecastFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (id == R.id.action_map){
            openPreferredLocationInMap();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void openPreferredLocationInMap(){
        Uri location = getLocation();
        Intent mapIntent = getMapIntent(location);
        if ( mapIntent.resolveActivity(getPackageManager()) != null ){
            startActivity(mapIntent);
        }
        else {
            Log.d(LOG_TAG, "MAPS ARE NOT SUPPORTED IN YOUR SHITTY DEVICE");
        }
    }

    public Intent getMapIntent(Uri geoLocation){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);
        return intent;
    }


    public Uri getLocation(){
        SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(this);
        String location = sharedPrefs.getString(
                getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));

        //Uri geoLocation = Uri.parse("geo:0,0?").buildUpon()
          //      .appendQueryParameter("q", location)
            //    .build();
        Uri geoLocation = Uri.parse("geo:0,0?q=" + location);
        return geoLocation;
    }

}