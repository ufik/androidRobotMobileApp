package cz.uhk.fim.activities;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import cz.uhk.fim.R;
import cz.uhk.fim.nxt.NxtRobot;
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
 * TODO documentation
 * @author Tomáš Voslař
 */
public class BluetoothConnectionActivity extends Activity {
	
	/**
	 * Constant for requesting bluetooth connection
	 */
	private static final int REQUEST_ENABLE_BT = 1;
	
	/**
	 * Holder for bluetooth adapter
	 */
	BluetoothAdapter mBluetoothAdapter;
	
	/**
	 * 
	 */
	ArrayAdapter mBluetoothDevices;
	
	/**
	 * 
	 */
	BroadcastReceiver mReceiver;
	
	/**
	 * 
	 */
	ListView lv;
	
	/**
	 * 
	 */
	Boolean btReceiverRegistered = false;
	
	/**
	 * 
	 */
	List<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();
	
	ProgressDialog pd;

	private SharedPreferences prefs;

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
            	
            // check whether bluetooth is enabled otherwise we enable it
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
					Toast t = Toast.makeText(BluetoothConnectionActivity.this, "chyba", 1000);
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
    
    private void startBtLookup(){
    	mBluetoothDevices.clear();
    	mBluetoothAdapter.startDiscovery();
    }
    
    public void onDestroy(){
    	super.onDestroy();
    	
    	if(btReceiverRegistered){
    		unregisterReceiver(mReceiver);
    		btReceiverRegistered = false;
    	}
    }
    
    public void onPause(){
    	super.onPause();
    	
    	if(btReceiverRegistered){
    		unregisterReceiver(mReceiver);
    		btReceiverRegistered = false;
    	}
    }
 
    public void onResume(){
    	super.onResume();
    	
    	if(!btReceiverRegistered) registerBtLookup();
    }
    
    /**
     * 
     * @param requestCode
     */
    private void onActivityResult(int requestCode){
    	
    	// bluetooth has been enabled
    	if(requestCode == REQUEST_ENABLE_BT){
    		getDevices();
    		
    	}

    	// bluetooth has not been enabled
    	else if(requestCode == RESULT_CANCELED){
    		
    	}
    }
    
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
    
    private void refreshDeviceList(){
    	mBluetoothDevices.notifyDataSetChanged();
    }
    
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