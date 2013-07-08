package cz.uhk.fim.activities;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import cz.uhk.fim.R;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

/**
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
 * Bluetooth class for searching near by devices.
 * @author Tomáš Voslař
 */
public class BluetoothConnectionActivity extends Activity {
	
	/**
	 * Constant for requesting Bluetooth connection
	 */
	private static final int REQUEST_ENABLE_BT = 1;
	
	/**
	 * Holder for bluetooth adapter
	 */
	BluetoothAdapter mBluetoothAdapter;
	
	/**
	 * Holds founded devices.
	 */
	ArrayAdapter<String> mBluetoothDevices;
	
	/** 
	 * Broadcast receiver for device searching.
	 */
	BroadcastReceiver mReceiver;
	
	/**
	 * List view for names of founded devices.
	 */
	ListView lv;
	
	/**
	 * Shows whether is broadcast receiver registered.
	 */
	Boolean btReceiverRegistered = false;
	
	/**
	 * Holds names of founded devices.
	 */
	List<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();
	
	/**
	 * Initialize progress dialog.
	 */
	ProgressDialog pd;

	/**
	 * Shared preferences of application.
	 */
	private SharedPreferences prefs;

	/**
	 * Define whether is auto connect enabled.
	 */
	private Boolean autoConnect;
	
    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device);
        
        mBluetoothDevices = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        
        lv = (ListView) findViewById(R.id.btDevices);
        lv.setAdapter(mBluetoothDevices);
        
        loadSettings();
        
        // get default adapter, if is null we cannot continue
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        }else{
        	// register device discovery broadcast
            registerBtLookup();
            
            if(autoConnect){
            	startBtLookup();
            	pd = ProgressDialog.show(BluetoothConnectionActivity.this, getString(R.string.loading_bt_discovery_title), getString(R.string.loading_bt_discovery_text));
            }
            	
            //check whether bluetooth is enabled otherwise we enable it
	        if (!mBluetoothAdapter.isEnabled()) {
		        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
	             
	        }
        }

        Button btn = (Button) findViewById(R.id.search);
        btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mBluetoothAdapter.cancelDiscovery();
				startBtLookup();
				
				pd = ProgressDialog.show(BluetoothConnectionActivity.this, getString(R.string.loading_bt_discovery_title), getString(R.string.loading_bt_discovery_text));
			}
		});
        
        lv.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> av, View v, int index,
					long arg3) {
				
				mBluetoothAdapter.cancelDiscovery();
				
				// create connection with device
				String device = mBluetoothDevices.getItem(index).toString();
				
				BluetoothDevice finalDevice = null;
				for (BluetoothDevice dev : devices) {
					
					if(dev.getName().equals(device)){
						finalDevice = dev;
						break;
					}
				}
				
				if(finalDevice != null){
					
					Intent i = new Intent(BluetoothConnectionActivity.this, AndroidRobotActivity.class);
					i.putExtra("device", finalDevice);
					startActivity(i);
					
					finish();
				
				}else{
					Toast t = Toast.makeText(BluetoothConnectionActivity.this, "chyba", Toast.LENGTH_LONG);
					t.show();
				}
				
			}
		});
 
    }
    
    /* Load sharedPreferences in local variables */
	private void loadSettings(){
		// get shared preferences
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		// set type of video stream
		autoConnect = prefs.getBoolean("pref_auto_connect", false);
		
	}
    
	/**
	 * Starts discovery of Bluetooth devices.
	 */
    private void startBtLookup(){
    	mBluetoothDevices.clear();
    	mBluetoothAdapter.startDiscovery();
    }
    
    /**
     * Unregister receiver when activity is destroyed.
     */
    public void onDestroy(){
    	super.onDestroy();
    	
    	if(btReceiverRegistered){
    		unregisterReceiver(mReceiver);
    		btReceiverRegistered = false;
    	}
    }
    
    /**
     * Unregister receiver when activity is paused.
     */
    public void onPause(){
    	super.onPause();
    	
    	if(btReceiverRegistered){
    		unregisterReceiver(mReceiver);
    		btReceiverRegistered = false;
    	}
    }
 
    /**
     * Register receiver when activity is active.
     */
    public void onResume(){
    	super.onResume();
    	
    	if(!btReceiverRegistered) registerBtLookup();
    }
    
    /**
     * Method called when request result is received.
     * @param requestCode
     */
    private void onActivityResult(int requestCode){
    	
    	// Bluetooth has been enabled
    	if(requestCode == REQUEST_ENABLE_BT){
    		getDevices();
    	}
    }
    
    /**
     * Register Bluetooth lookup for devices.
     */
    private void registerBtLookup(){
    	// Create a BroadcastReceiver for ACTION_FOUND
        mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d("btAction", action);
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // Add the name and address to an array adapter to show in a ListView
                    
                    mBluetoothDevices.add(device.getName());
                    devices.add(device);
                    
                    refreshDeviceList();
                    
                    if(!autoConnect) pd.dismiss();
                    
                    if((device.getName().equals("NXT") || device.getName().equals("HTC Vision")) && autoConnect){ // TODO dopsat do nastaveni povolene zarizeni
                    	mBluetoothAdapter.cancelDiscovery();
                    	
                    	Intent i = new Intent(BluetoothConnectionActivity.this, AndroidRobotActivity.class);
    					i.putExtra("device", device);
    					startActivity(i);
    					
    					finish();
                    }
                }

            }
        };
        
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        
        btReceiverRegistered = true;
    }
    
    /**
     * Refresh list view with devices names.
     */
    private void refreshDeviceList(){
    	mBluetoothDevices.notifyDataSetChanged();
    }
    
    /**
     * Get bounded devices and refreshs list view.
     */
    private void getDevices(){
    	
    	mBluetoothDevices.clear();
    	
    	Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
    	// If there are paired devices
    	if (pairedDevices.size() > 0) {
    	    // Loop through paired devices
    	    for (BluetoothDevice device : pairedDevices) {
    	        // Add the name and address to an array adapter to show in a ListView
    	    	Log.e("a", device.getName());
    	    	mBluetoothDevices.add(device.getName());
    	    }
    	}
    	
		refreshDeviceList();
    }
}