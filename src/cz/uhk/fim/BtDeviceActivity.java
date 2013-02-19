package cz.uhk.fim;

import java.util.Set;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * 
 * @author Tomáš Voslař
 */
public class BtDeviceActivity extends Activity {
	
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
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device);
        
        mBluetoothDevices = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        
        lv = (ListView) findViewById(R.id.btDevices);
        lv.setAdapter(mBluetoothDevices);
        
        // get default adapter, if is null we cannot continue
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        }
        
        // check whether bluetooth is enabled otherwise we enable it
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            
        }
        
        Button btn = (Button) findViewById(R.id.button1);
        btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				getDevices();
			}
		});
        
        Button btn2 = (Button) findViewById(R.id.button2);
        btn2.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				registerBtLookup();
			}
		});
        
        lv.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> av, View v, int index,
					long arg3) {

				Intent i = new Intent(BtDeviceActivity.this, MainActivity.class);
				i.putExtra("device", mBluetoothDevices.getItem(index).toString());
				startActivity(i);
				
			}
		});
        
        // Create a BroadcastReceiver for ACTION_FOUND
        mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // Add the name and address to an array adapter to show in a ListView
                    mBluetoothDevices.add(device.getName());
                    refreshDeviceList();
                }
            }
        };
    }
    
    private void registerBtLookup(){
    	mBluetoothDevices.clear();
    	
    	if(btReceiverRegistered){
	        // Register the BroadcastReceiver
	        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
	        registerReceiver(mReceiver, filter);
	        
	        btReceiverRegistered = true;
    	}
    }
    
    public void onDestroy(){
    	super.onDestroy();
    	
    	if(btReceiverRegistered) unregisterReceiver(mReceiver);
    }
    
    public void onPause(){
    	super.onPause();
    }
 
    public void onResume(){
    	super.onResume();
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