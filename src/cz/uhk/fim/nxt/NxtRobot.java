package cz.uhk.fim.nxt;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.UUID;
import lejos.pc.comm.NXTConnector;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

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
 * Class for communication with NXT robot.
 * @author Tomáš Voslař
 */
public class NxtRobot implements Runnable {
	
	/**
	 * Bluetooth socket for communication with nxt brick. 
	 */
	private BluetoothSocket nxtBTsocket = null;
	
	/**
     * Output stream writer for Bluetooth socket. 
     */
	private OutputStreamWriter nxtOsw = null;
	
	/**
	 * Handler for outside communication with this class.
	 */
	private Handler mHandler = new Handler();
	
	/**
     * Input stream writer for Bluetooth socket. 
     */
	private InputStreamReader nxtIsr = null;
	
	/**
	 * NXT connector instance holder. 
	 */
	NXTConnector nxtconn;

	/**
	 * Holder for Bluetooth device (NXT brick).
	 */
	BluetoothDevice mDevice = null;

	/**
	 * Ultra sonic values holder.
	 */
	private int distance = 0;
	
	/**
	 * Horizontal direction holder.
	 */
	private String azimuth = null;
	
	/**
	 * Position of the device.
	 */
	private String position = null;

	private Boolean isRunning = false;

	/**
	 * Primitive constructor.
	 */
	public NxtRobot(Handler handler, BluetoothDevice device) {
		mHandler = handler;
		mDevice = device;

	}

	/**
	 * Destroy nxt bluetooth connection.
	 * 
	 * @return boolean returns true if destroying of connection was succesfull,
	 *         false otherwise
	 */
	public Boolean terminate() {
		isRunning = false;
		return true;
	}

	/**
	 * Create BluetoothConnection and create outputWritter for communication
	 * with NXT robot.
	 * 
	 * @param BluetoothDevice
	 *            device with NXT robot
	 * @return Boolean returns true if connection was succesfull, false
	 *         otherwise
	 */
	public OutputStreamWriter createConnection(BluetoothDevice device) {
		try {

			nxtBTsocket = device.createRfcommSocketToServiceRecord(UUID
					.fromString("00001101-0000-1000-8000-00805F9B34FB")); // hardcoded
																			// UUID
																			// for
																			// NXT
			nxtBTsocket.connect();

			nxtOsw = new OutputStreamWriter(nxtBTsocket.getOutputStream()); 
			nxtIsr = new InputStreamReader(nxtBTsocket.getInputStream());

			return nxtOsw;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Start thread.
	 */
	public void run() {

		isRunning = true;

		int charsRead = 20;

		char[] buffer = new char[20];

		Log.d("sonar", "starts");
		try {

			while ((charsRead = nxtIsr.read(buffer)) != -1 && isRunning) {
				final String message = new String(buffer).substring(0,
						charsRead);
				Log.d("sonar", message);
				// sends message via handler to UI thread
				setDistance(Integer.valueOf(message));
			}

		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			nxtIsr.close();
			if (nxtBTsocket != null) {
				nxtBTsocket.close();
				nxtBTsocket = null;
			}

			nxtOsw.close();
			Log.d("robot nxt", "BT closed.");
		} catch (IOException e) {
			Log.d("robot nxt", "BT error while closing.");
			e.printStackTrace();
		}
	}
	
	/**
	 * Getter of the distance value.
	 * @return int distance
	 */
	public int getDistance() {
		return distance;
	}

	/**
	 * Setter for the distance value.
	 * @param int distance
	 */
	public void setDistance(int distance) {
		this.distance = distance;
	}
	
	/**
	 * Getter of the horizontal direction.
	 * @return String horizontal direction
	 */
	public String getAzimuth() {
		return azimuth;
	}

	/**
	 * Setter for the horizontal direction.
	 * @param azimuth
	 */
	public void setAzimuth(String azimuth) {
		this.azimuth = azimuth;
	}

	/**
	 * Getter of the position.
	 * @return String position
	 */
	public String getPosition() {
		return position;
	}

	/**
	 * Setter for the position.
	 * @param String position
	 */
	public void setPosition(String position) {
		this.position = position;
	}
}