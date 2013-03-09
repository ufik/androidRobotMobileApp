package cz.uhk.fim.nxt;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.UUID;

import cz.uhk.fim.R;
import cz.uhk.fim.activities.AndroidRobotActivity;
import lejos.pc.comm.NXTConnector;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

/**
 * Class for communication with NXT robot.
 * TODO documentation
 * @author Tomáš Voslař
 */
public class NxtRobot implements Runnable {
	/**
	 * 
	 */
	private BluetoothSocket nxtBTsocket = null;
	/**
     * 
     */
	private OutputStreamWriter nxtOsw = null;
	/**
     * 
     */
	private InputStreamReader nxtIsr = null;
	/**
	 * 
	 */
	NXTConnector nxtconn;

	private Handler mHandler = new Handler();

	BluetoothDevice mDevice = null;

	/* distance in cms */
	private int distance = 0;

	private Boolean isRunning = false;

	/**
	 * Static properties with NXT commands.
	 */
	public final static int FORWARD = 1;
	public final static int BACKWARD = 2;
	public final static int LEFT = 3;
	public final static int RIGHT = 4;
	public final static int FORWARD_LEFT = 5;
	public final static int FORWARD_RIGHT = 6;
	public final static int BACKWARD_LEFT = 7;
	public final static int BACKWARD_RIGHT = 8;
	public final static int STOP = 0;

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

			nxtOsw = new OutputStreamWriter(nxtBTsocket.getOutputStream()); // TODO
																			// mozna
																			// bude
																			// problem
																			// s
																			// predanou
																			// promennou,
																			// pripadne
																			// zkusit
																			// predat
																			// primo
																			// objekt
			nxtIsr = new InputStreamReader(nxtBTsocket.getInputStream());

			return nxtOsw;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public int getDistance() {
		return distance;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	@Override
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
}
