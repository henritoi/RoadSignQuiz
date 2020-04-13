package com.example.roadsignquiz;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A placeholder fragment containing a simple view.
 */
public class SettingsActivityFragment extends PreferenceFragment {
    private static final String TAG = "SettingsActivityFragmen";

    // luo preferences käyttöliittymän tiedoston prefrences.xml avulla joka res/xml kansiossa
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.d(TAG, "onCreate: Create");
        addPreferencesFromResource(R.xml.preferences); // ladataan XML:sta

    }

}
