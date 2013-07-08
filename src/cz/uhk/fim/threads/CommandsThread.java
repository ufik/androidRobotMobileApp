package cz.uhk.fim.threads;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Hashtable;
import android.util.Log;
import cz.uhk.fim.activities.AndroidRobotActivity.UIThreadHandler;

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
 * Thread for communication with NXT robot. It sends commands via BluetoothSocket.
 * @author Tomáš Voslař
 */
public class CommandsThread implements Runnable {
	/**
	 * Buffered reader for socket. 
	 */
	private BufferedReader in;
	
	/**
	 * Received message.
	 */
	private String message = "";
	
	/**
	 * Handler for communication with main activity.
	 */
	private UIThreadHandler mHandler;
	
	/**
	 * Communication socket.
	 */
	private Socket mCommunicationSocket;
	
	/**
	 * Defines whether thread is running or not.
	 */
	private Boolean isRunning = false;
	
	/* Constructor */
	public CommandsThread(UIThreadHandler handler, Socket socket) {
		mHandler = handler;
		mCommunicationSocket = socket;
	}
	
	/**
	 *  Terminate thread 
	 */
	public void terminate(){
		isRunning = false;
	}
	
	/**
	 * Starts thread.
	 */
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

				Hashtable<String, String> tmp = decodeCommand(message);

				if (tmp.containsKey("command")) {

					int speed = 0;
					int steering = 0;

					try {
						if(tmp.get("speed") != null){
							speed = Integer.valueOf(tmp.get("speed").toString());
						}
						
						if(tmp.get("steering") != null){
							steering = Integer.valueOf(tmp.get("steering").toString());
						}
					} catch (NumberFormatException e) {
						Log.d("speed", "Error while parsing speed.");
					}
										
					mHandler.rCommand(speed, steering);
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
	 * Decodes received command and splits it into hashtable.
	 * @param String command to decode
	 * @return HashTable decoded and splited command
	 */
	private Hashtable<String, String> decodeCommand(String command) {

		String[] tmp = command.toString().split(";");

		Hashtable<String, String> r = new Hashtable<String, String>();

		for (String string : tmp) {
			if (string.contains("command"))
				r.put("command", string);
			else{
				String[] tmp2 = string.split("#");
				r.put("speed", tmp2[0]);
				if(tmp2.length > 1) r.put("steering", tmp2[1]);
			}
		}

		return r;
	}

}