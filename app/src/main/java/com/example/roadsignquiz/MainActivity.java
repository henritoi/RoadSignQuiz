package com.example.roadsignquiz;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Set;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    public static final String CHOICES = "pref_numberOfChoices";
    public static final String SIGN_GROUPS = "pref_signGroupsToInclude";

    private boolean phoneDevice = true;
    private boolean preferencesChanged = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Created!");
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Log.d(TAG, "onCreate: Toolbar done" + toolbar);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(
                        preferencesChangeListener
                );
        int screenSize = getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK;
        if (screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE ||
                screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE) phoneDevice = false;

        if(phoneDevice) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (preferencesChanged) {
            // kun perusasetukset on tehty, niin alustetaan MainActivityFragmetn
            // ja käynnistetään visailu
            MainActivityFragment quizFragment = (MainActivityFragment)
                    getSupportFragmentManager().findFragmentById(
                            R.id.quizFragment);
            quizFragment.updateGuessRows(
                    PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.updateSignGroup(
                    PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.resetQuiz();
            preferencesChanged = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu: Created");
        int orientation = getResources().getConfiguration().orientation;
        if(orientation == Configuration.ORIENTATION_PORTRAIT) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        /*
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        */

        //Intent preferencesIntent = new Intent(this, SettingsActivity.class);
        Intent preferencesIntent = new Intent(this, SettingsActivity.class);
        startActivity(preferencesIntent);
        return super.onOptionsItemSelected(item);
    }

    private SharedPreferences.OnSharedPreferenceChangeListener preferencesChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    preferencesChanged = true;

                    MainActivityFragment quizFragment = (MainActivityFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.quizFragment);

                    if(key.equals(CHOICES)) {
                        quizFragment.updateGuessRows(sharedPreferences);
                        quizFragment.resetQuiz();
                    }else if(key.equals(SIGN_GROUPS)) {
                        Set<String> signGroups = sharedPreferences.getStringSet(SIGN_GROUPS, null);
                        if(signGroups != null && signGroups.size() > 0) {
                            quizFragment.updateSignGroup(sharedPreferences);
                            quizFragment.resetQuiz();
                        }else {
                            signGroups = new ArraySet<>();
                            signGroups.add(getString(R.string.default_sign_group));
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putStringSet(SIGN_GROUPS, signGroups);
                            editor.apply();

                            Toast.makeText(
                                    MainActivity.this,
                                    R.string.restarting_quiz,
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
                    Toast.makeText(
                            MainActivity.this,
                            R.string.restarting_quiz, Toast.LENGTH_SHORT
                    ).show();
                }
            };
}
