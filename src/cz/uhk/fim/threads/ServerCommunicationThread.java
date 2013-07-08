package cz.uhk.fim.threads;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import cz.uhk.fim.activities.AndroidRobotActivity.UIThreadHandler;
import cz.uhk.fim.nxt.NxtRobot;

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
 * Class for TCP communication with server app.
 * @author Tomáš Voslař 
 */
public class ServerCommunicationThread implements Runnable {
	/**
	 * NXT robot instance holder.
	 */
	private NxtRobot mRobot = null;
	
	/**
	 * Handler for communication with main activity.
	 */
	private UIThreadHandler mHandler = null;
	
	/**
	 * Defines whether is thread running or not.
	 */
	private Boolean isRunning = false;
	
	/**
	 * Socket for TCP communication.
	 */
	private Socket mSocket = null;
	
	/**
	 * Thread class for communication with NXT brick.
	 */
	private CommandsThread c = null;
	
	/**
	 * Thread for communication with NXT brick class.
	 */
	private Thread ct = null;

	/**
	 * Constructor.
	 * @param handler
	 * @param robot
	 */
	public ServerCommunicationThread(UIThreadHandler handler, NxtRobot robot) {
		mRobot = robot;
		mHandler = handler;
	}
	
	/**
	 *  Terminate thread 
	 */
	public void terminate() {	
		isRunning = false;
		
		// terminate commands thread
		if(c != null) c.terminate();
	}

	/**
	 *  Starts thread. 
	 */
	public void run() {

		isRunning = true;

		try {

			mSocket = new Socket("31.31.79.28", 1330);
			
			c = new CommandsThread(mHandler, mSocket);
			ct = new Thread(c);
			ct.start();

			OutputStream os = mSocket.getOutputStream();
			
			Long lt = System.currentTimeMillis();
			
			while (isRunning) {
				
				if((System.currentTimeMillis() - lt) > 1000){
				
					mHandler.changeStatus(0, String.valueOf(mRobot.getDistance()));
					
					String i = mRobot.getDistance() + ";" + mRobot.getAzimuth() + ";" + mRobot.getPosition() + ";" + System.currentTimeMillis();
					
					os.write(i.getBytes());
					os.flush();
					
					lt = System.currentTimeMillis();
				}
			}

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {

			if(mSocket != null) mSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
