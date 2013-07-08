package cz.uhk.fim.activities;

import java.util.ArrayList;
import java.util.List;
import cz.uhk.fim.R;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
 * 
 *   Copyright (C) <2013>  <Tomáš Voslař (t.voslar@gmail.com)>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *   
 * Class with settings of the application.
 * @author Tomáš Voslař
 *
 */
public class PreferencesActivity extends PreferenceActivity {
	
	/**
	 * Shared preferences
	 */
	private SharedPreferences prefs = null;
	
	/**
	 * Holds camera resolutions.
	 */
	private CharSequence[] entries = {};
	
	/* After start of this activity.  */
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
