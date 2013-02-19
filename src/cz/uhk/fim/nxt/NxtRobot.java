package cz.uhk.fim.nxt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Set;
import java.util.UUID;

import lejos.pc.comm.NXTConnector;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

/**
 * 
 * @author Tomáš Voslař
 *
 */
public class NxtRobot {
	
    private BluetoothSocket nxtBTsocket = null;
    private static OutputStreamWriter nxtOsw = null;
    private static InputStream nxtDis = null;
	
    NXTConnector nxtconn;
    
	public final static int FORWARD = 1;
	public final static int BACKWARD = 2;
	public final static int LEFT = 3;
	public final static int RIGHT = 4;
	public final static int FORWARD_LEFT = 5;
	public final static int FORWARD_RIGHT = 6;
	public final static int BACKWARD_LEFT = 7;
	public final static int BACKWARD_RIGHT = 8;
	public final static int STOP = 0;
	
	public NxtRobot() {
		
	}
		
	/**
	 * Destroy nxt bluetooth connection.
	 * @return boolean
	 */
	public Boolean destroyConnection() {
        try {
            if (nxtBTsocket != null) {
                nxtBTsocket.close();
                nxtBTsocket = null;
            }
            nxtOsw = null;      
            return true;
        } catch (IOException e) {
            return false;
        }
    }
	
	/**
	 * 
	 * @param device
	 */
	public Boolean createConnection(String device) {
		try {
            
			BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
            Set<BluetoothDevice> bondedDevices = btAdapter.getBondedDevices();
            BluetoothDevice nxtDevice = null;
            
            for (BluetoothDevice bluetoothDevice : bondedDevices){
                if (bluetoothDevice.getName().equals(device)) {
                    nxtDevice = bluetoothDevice;
                    break;
                }
            } 

            if (nxtDevice == null){
                return false;
            }             

            nxtBTsocket = nxtDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            nxtBTsocket.connect();
            nxtOsw = new OutputStreamWriter(nxtBTsocket.getOutputStream());
            
            return true;
        } catch (IOException e) {
        		return false;
        }
	}
	
	public Boolean sendCommand(int command, int value) {
        
		if (nxtOsw == null) {
            return false;
        }

        try {
        	nxtOsw.write(command);
        	//nxtOsw.write(value);
        	nxtOsw.flush();
           
            return true;
        } catch (IOException ioe) { 
            return false;            
        }
    }
	
}
