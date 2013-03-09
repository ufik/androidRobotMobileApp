package cz.uhk.fim.activities;

import java.util.ArrayList;
import java.util.List;

import cz.uhk.fim.R;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Class with settings of the application.
 * @author Tomáš Voslař
 *
 */
public class PreferencesActivity extends PreferenceActivity {
	
	private SharedPreferences prefs = null;
	private CharSequence[] entries = {};
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    addPreferencesFromResource(R.xml.pref_general);
	    		
	    prefs = PreferenceManager.getDefaultSharedPreferences(this);
	    
	    ListPreference cameraSize = (ListPreference) findPreference("pref_camera_size");
	    
	    List<String> s = new ArrayList<String>();
	    
	    s.add("176x144");
        s.add("352x288");
        s.add("702x576");
      
	    entries = s.toArray(new CharSequence[2]);

	    cameraSize.setEntries(entries);
 	    cameraSize.setEntryValues(entries);
	}

}
