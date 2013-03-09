package cz.uhk.fim.threads;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import android.util.Log;
import cz.uhk.fim.activities.AndroidRobotActivity.UIThreadHandler;
import cz.uhk.fim.nxt.NxtRobot;

/**
 * TODO documentation
 * @author Tomáš Voslař
 * 
 */
public class ServerCommunicationThread implements Runnable {

	private NxtRobot mRobot = null;
	private UIThreadHandler mHandler = null;
	private Boolean isRunning = false;
	private Socket mSocket = null;
	private CommandsThread c = null;
	private Thread ct = null;

	/* Constructor */
	public ServerCommunicationThread(UIThreadHandler handler, NxtRobot robot) {
		mRobot = robot;
		mHandler = handler;
	}
	
	/* Terminate thread */
	public void terminate() {	
		isRunning = false;
		
		// terminate commands thread
		if(c != null) c.terminate();
	}

	public void run() {

		isRunning = true;

		try {

			mSocket = new Socket("192.168.1.11", 1330);
			
			c = new CommandsThread(mHandler, mSocket);
			ct = new Thread(c);
			ct.start();

			OutputStream os = mSocket.getOutputStream();
			
			Long lt = System.currentTimeMillis();
			
			while (isRunning) {
				
				if((System.currentTimeMillis() - lt) > 1500){
				
					mHandler.changeStatus(String.valueOf(mRobot.getDistance()));
	
					os.write(mRobot.getDistance());
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
