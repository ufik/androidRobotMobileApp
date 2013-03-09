package cz.uhk.fim.threads;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Hashtable;
import android.util.Log;
import cz.uhk.fim.activities.AndroidRobotActivity.UIThreadHandler;
import cz.uhk.fim.nxt.NxtRobot;

/**
 * Thread for communication with NXT robot. It sends commands via BluetoothSocket.
 * @author Tomáš Voslař
 *
 */
public class CommandsThread implements Runnable {
	// TODO refract
	private BufferedReader in;
	String message = "";
	private UIThreadHandler mHandler;
	private Socket mCommunicationSocket;
	private Boolean isRunning = false;
	
	/* Constructor */
	public CommandsThread(UIThreadHandler handler, Socket socket) {
		mHandler = handler;
		mCommunicationSocket = socket;
	}
	
	/* Terminate thread */
	public void terminate(){
		isRunning = false;
	}
	
	@Override
	public void run() {
		isRunning = true;
		
		// read commands from robot
		try {
			in = new BufferedReader(new InputStreamReader(mCommunicationSocket.getInputStream()));

		} catch (IOException e1) {
			e1.printStackTrace();
		}

		int charsRead = 30;

		char[] buffer = new char[30];

		try {

			while ((charsRead = in.read(buffer)) != -1 && isRunning) {
				message = new String(buffer).substring(0, charsRead);

				int command = NxtRobot.STOP; // it means stop

				Hashtable<String, String> tmp = decodeCommand(message);

				if (tmp.containsKey("command")) {

					if (tmp.get("command").equals("command-1")) {
						command = NxtRobot.FORWARD;
					} else if (tmp.get("command").equals("command-2")) {
						command = NxtRobot.BACKWARD;
					} else if (tmp.get("command").equals("command-3")) {
						command = NxtRobot.LEFT;
					} else if (tmp.get("command").equals("command-4")) {
						command = NxtRobot.RIGHT;
					} else if (tmp.get("command").equals("command-torch")) {
						mHandler.tTorch();
					}

					int speedPercentage = 0;

					try {
						if(tmp.get("speed") != null) speedPercentage = Integer.valueOf(tmp.get("speed").toString());
					} catch (NumberFormatException e) {
						// FIXME chtelo by to preskocit do dalsi iterace
						Log.d("speed", "Error while parsing speed.");
					}
					Log.d("INTERNET command", "test");
					// recalculation of speed
					float speed = 640f * (Float.valueOf(speedPercentage) / 100);

					mHandler.rCommand(command, (int) speed);
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param command
	 * @return
	 */
	private Hashtable<String, String> decodeCommand(String command) {

		String[] tmp = command.toString().split(";");

		Hashtable<String, String> r = new Hashtable<String, String>();

		for (String string : tmp) {
			if (string.contains("command"))
				r.put("command", string);
			else
				r.put("speed", string);
		}

		return r;
	}

}