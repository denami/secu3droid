/* Secu3Droid - An open source, free manager for SECU-3 engine
 * control unit
 * Copyright (C) 2013 Maksim M. Levin. Russia, Voronezh
 * 
 * SECU-3  - An open source, free engine control unit
 * Copyright (C) 2007 Alexey A. Shabelnikov. Ukraine, Gorlovka
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contacts:
 *            http://secu-3.org
 *            email: mmlevin@mail.ru
*/

package org.secu3.android;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import java.util.HashSet;
import java.util.Set;

import org.secu3.android.api.io.Secu3Logger;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	private final static String LOG_TAG = "SettingsActivity";

	public final static int PROTOCOL_UNKNOWN = 0;
	public final static int PROTOCOL_12042013_SPRING_RELEASE = 1;
	public final static int PROTOCOL_28082013_SUMMER_RELEASE = 2;
	public final static int PROTOCOL_14012014_WINTER_RELEASE = 3;
	public final static int PROTOCOL_16052014_SPRING_RELEASE = 4;
	public final static int PROTOCOL_10022015_WINTER_RELEASE = 5;
	public final static int PROTOCOL_DEVELOPER_RELEASE = 6;
	
	private SharedPreferences sharedPref ;
	private BluetoothAdapter bluetoothAdapter = null;
	private String versions[] = null;
	private String CSVDelimeters[] = null;
	
	public static int getProtocolVersion(Context ctx) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		int version = 0;
		String versionS = sharedPreferences.getString(ctx.getString(R.string.pref_protocol_version_key), null);
		if (versionS != null) {
			version = Integer.parseInt(versionS);
		}
        return version+1;
	}
	
	public static boolean isSensorLoggerEnabled (Context ctx){
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		return sharedPreferences.getBoolean(ctx.getString(R.string.pref_write_log_key), false);
	}	
		
	public static String getCSVDelimeter(Context ctx) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		String delimeter = sharedPreferences.getString(ctx.getString(R.string.pref_log_csv_delimeter_key), ctx.getString(R.string.defaultCsvDelimeter));
		try {
			int idx = delimeter.indexOf("\"");
			return 	delimeter.substring(idx+1,idx+2);
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		return ";";
	}
	
	public static boolean isKeepScreenAliveActive (Context ctx) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		return sharedPreferences.getBoolean(ctx.getString(R.string.pref_keep_screen_key), false);
	}
	
	public static boolean isWakeLockEnabled (Context ctx) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		return sharedPreferences.getBoolean(ctx.getString(R.string.pref_wakelock_key), false);
	}
	
	@SuppressWarnings("deprecation")
	private void updatePreferenceSummary(){
		String deviceName = "";
        ListPreference prefDevices = (ListPreference)findPreference(getString(R.string.pref_bluetooth_device_key));
        String deviceAddress = sharedPref.getString(getString(R.string.pref_bluetooth_device_key), null);
        if (BluetoothAdapter.checkBluetoothAddress(deviceAddress)){
        	deviceName = bluetoothAdapter.getRemoteDevice(deviceAddress).getName();
        }
        prefDevices.setSummary(getString(R.string.pref_bluetooth_device_summary, deviceName));
        
        String protocolVersion = versions[getProtocolVersion(this)-1];
        ListPreference prefVersions = (ListPreference)findPreference(getString(R.string.pref_protocol_version_key));        
        prefVersions.setSummary(protocolVersion);
        
		findPreference(getString(R.string.pref_write_log_path)).setSummary(String.format(getString(R.string.pref_write_log_path_summary), Secu3Logger.getDefaultPath()));
		
        Preference speedPulses = findPreference(getString(R.string.pref_speed_pulse_key));
        speedPulses.setSummary(sharedPref.getString(getString(R.string.pref_speed_pulse_key), getString(R.string.defaultSpeedPulse)));
        
        ListPreference csvDelimeter = (ListPreference)findPreference(getString(R.string.pref_log_csv_delimeter_key));
        csvDelimeter.setSummary(getString(R.string.pref_log_csv_delimeter_summary, sharedPref.getString(getString(R.string.pref_log_csv_delimeter_key), getString(R.string.defaultCsvDelimeter))));
    }   

	@SuppressWarnings("deprecation")
	private void updatePreferenceList(){
        // update bluetooth device summary
		updatePreferenceSummary();
		// update bluetooth device list
        ListPreference prefDevices = (ListPreference)findPreference(getString(R.string.pref_bluetooth_device_key));
        Set<BluetoothDevice> pairedDevices = new HashSet<>();
        if (bluetoothAdapter != null){
        	pairedDevices = bluetoothAdapter.getBondedDevices();  
        }
        String[] entryValues = new String[pairedDevices.size()];
        String[] entries = new String[pairedDevices.size()];
        int i = 0;
    	    // Loop through paired devices
        for (BluetoothDevice device : pairedDevices) {

        	entryValues[i] = device.getAddress();
            entries[i] = device.getName();
            i++;
        }
        prefDevices.setEntryValues(entryValues);
        prefDevices.setEntries(entries);
        
        ListPreference prefVersions = (ListPreference)findPreference(getString(R.string.pref_protocol_version_key));
        entryValues = new String[versions.length];
        for (int j = 0; j != versions.length; j++) {
        	entryValues[j] = Integer.toString(j);
        }
        prefVersions.setEntryValues(entryValues);
        prefVersions.setEntries(versions);
        
        ListPreference prefCSV = (ListPreference)findPreference(getString(R.string.pref_log_csv_delimeter_key));                
        prefCSV.setEntryValues(CSVDelimeters);
        prefCSV.setEntries(CSVDelimeters);        
        
        Preference pref;        
        pref = findPreference(getString(R.string.pref_connection_retries_key));
        String maxConnRetries = sharedPref.getString(getString(R.string.pref_connection_retries_key), getString(R.string.defaultConnectionRetries));
        pref.setSummary(getString(R.string.pref_connection_retries_summary,maxConnRetries));
        this.onContentChanged();
    }	
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_night_mode_key), false)?R.style.AppBaseTheme:R.style.AppBaseTheme_Light);		
		super.onCreate(savedInstanceState);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		addPreferencesFromResource(R.xml.preferences);				
        versions = getResources().getStringArray(R.array.protocol_versions);
        CSVDelimeters = getResources().getStringArray(R.array.csv_delimeters);
		
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();	
        Preference pref = findPreference(getString(R.string.pref_about_key));
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {		
			@Override
			public boolean onPreferenceClick(Preference preference) {
				SettingsActivity.this.displayAboutDialog();
				return true;
			}
		});          
	}		

	@Override
	protected void onResume() {
        sharedPref.registerOnSharedPreferenceChangeListener(this);
		this.updatePreferenceList();		
		super.onResume();		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		sharedPref.unregisterOnSharedPreferenceChangeListener(this);		
	}	

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (getString(R.string.pref_bluetooth_device_key).equals(key)) {
			updatePreferenceSummary();
			updatePreferenceList();			
		} else if (getString(R.string.pref_protocol_version_key).equals(key)) {
			updatePreferenceSummary();
			updatePreferenceList();					
		} else if (getString(R.string.pref_log_csv_delimeter_key).equals(key)) {
			updatePreferenceSummary();
			updatePreferenceList();
		}
		else if (getString(R.string.pref_speed_pulse_key).equals(key))
		{
			updatePreferenceSummary();
		}
		if (getString(R.string.pref_night_mode_key).equals(key)) {
			getApplicationContext().setTheme(sharedPreferences.getBoolean(getString(R.string.pref_night_mode_key), false)?R.style.AppBaseTheme:R.style.AppBaseTheme);
		}
	}
	
	private void displayAboutDialog(){
        View messageView = getLayoutInflater().inflate(R.layout.about, null, false);
        // we need this to enable html links
        TextView textView = (TextView) messageView.findViewById(R.id.about_license);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        // When linking text, force to always use default color. This works
        // around a pressed color state bug.
        int defaultColor = textView.getTextColors().getDefaultColor();
        textView.setTextColor(defaultColor);
        textView = (TextView) messageView.findViewById(R.id.about_sources);
        textView.setTextColor(defaultColor);
        
        textView = (TextView)messageView.findViewById(R.id.about_version_name);
        try {
        	PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        	textView.setText(pInfo.versionName);
        } catch (Exception e) {
			Log.e(LOG_TAG,e.getMessage());
        }
       
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
	    	if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_night_mode_key), false))
	    	{
	    		builder.setInverseBackgroundForced(true);
	    	}
	    }
		builder.setTitle(R.string.about_title);
		builder.setIcon(R.drawable.gplv3_icon);
        builder.setView(messageView);
		builder.show();
	}	
}
