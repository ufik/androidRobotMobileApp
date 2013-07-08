package cz.uhk.fim.threads;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import cz.uhk.fim.activities.AndroidRobotActivity.UIThreadHandler;
import cz.uhk.fim.view.CameraPreview;

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
 * Thread for sending jpeg pictures via Socket (UDP/TCP).
 * @author Tomáš Voslař
 */
public class JpegVideoStreamThread implements Runnable {
	/**
	 * Datagram socket for UDP communication.
	 */
	private DatagramSocket ds = null;
	
	/**
	 * Socket for TCP communication.
	 */
	private Socket s = null;
	
	/**
	 * Output stream for socket communication.
	 */
	private OutputStream os = null;
	
	/**
	 * Handler for communication with main activity.
	 */
	private UIThreadHandler mHandler = null;
	
	/**
	 * Camera preview holder.
	 */
	private CameraPreview mCp = null;
	
	/**
	 * Defines whether is thread running or not.
	 */
	private Boolean isRunning = false;
	
	/**
	 * Defines frames per second of video sequence.
	 */
	private int mStreamFps = 0;
	
	/**
	 * Defines transfer type. (TCP or UDP)
	 */
	private int mTransferType = 0;
	
	
	private int sended = 0;
	private int frames = 0;
	
	/**
	 * Constant definition - UDP protocol
	 */
	private static final int UDP = 0;
	
	/**
	 * Constant definition - TCP protocol
	 */
	private static final int TCP = 1;
	
	/**
	 * Constructor.
	 * @param handler
	 * @param cp
	 * @param streamFps
	 * @param transferType
	 */
	public JpegVideoStreamThread(UIThreadHandler handler, CameraPreview cp, int streamFps, int transferType) {
		mHandler = handler;
		mCp = cp; 
		mStreamFps = streamFps;
		mTransferType = transferType;
	}
	
	/**
	 * Terminates thread.
	 */
	public void terminate(){
		isRunning = false;
		
		mHandler.destroyCamera();
	}
	
	/**
	 * Convert bytes into human readable value.
	 * @param bytes
	 * @param si
	 * @return String human readable value
	 */
	public static String humanReadableByteCount(long bytes, boolean si) {
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
	
	/**
	 * Starts thread.
	 */
	public void run() {
		isRunning = true;
		
		if(mTransferType == UDP) createUDP();
		else if(mTransferType == TCP) createTCP();
		
		// transfer
		int corrector = 0;
		long lastTime = System.currentTimeMillis();
		long lastTime2 = System.currentTimeMillis();
		while(isRunning){
			
    			byte[] currentFrame = {0};
    			
        		if(mCp != null){
        			currentFrame = mCp.getCurrentFrame();
        		}

                if(currentFrame != null){
                	
                	if((System.currentTimeMillis() - lastTime) >= (1000 / (mStreamFps + corrector))){
                	
	                	sended += currentFrame.length;
	                	frames++;
	        			mHandler.changeStatus(2, String.valueOf(humanReadableByteCount(sended, true)));
	                	//mHandler.changeStatus(2, String.valueOf(corrector));
	                	
	                	if(mTransferType == UDP){
	                		sendUDPPacket(currentFrame);
	                	}
	            		else if(mTransferType == TCP){
	            			sendTCPPacket(currentFrame);
	            		}
	                	
	                	lastTime = System.currentTimeMillis();
	                	
                	}
                	// correcting frames per second
                	if((System.currentTimeMillis() - lastTime2) >= 1000){
                		
                		if(frames < mStreamFps) corrector++;
                		else if(frames > mStreamFps) corrector--;
                		
                		frames = 0;
                		lastTime2 = System.currentTimeMillis();
                	}
                	
                }
		}
		
		if(mTransferType == UDP) closeUDP();
		else if(mTransferType == TCP) closeTCP();
	}
	
	/**
	 * Creates TCP connection.
	 */
	private void createTCP(){
		try {
			s = new Socket("31.31.79.28", 1337);
			os = s.getOutputStream();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates UDP connection.
	 */
	private void createUDP(){
		// connection
		try {
			ds = new DatagramSocket();
			ds.connect(InetAddress.getByName("31.31.79.28"), 1400);
		} catch (SocketException e) {
			e.printStackTrace();
		}catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sends TCP packet.
	 * @param frame
	 */
	private void sendTCPPacket(byte[] frame){
		try {
			if(os != null){
				os.write(frame, 0, frame.length);
				os.flush(); // TODO implement, this do nothing
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sends UDP packet.
	 * @param frame
	 */
	private void sendUDPPacket(byte[] frame){
		DatagramPacket packet = new DatagramPacket(frame, frame.length);
    	
    	try {
			if(ds != null) ds.send(packet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Closes UDP connection.
	 */
	private void closeUDP(){
		if(ds != null) ds.close();
	}
	
	/**
	 * Closes TCP connection.
	 */
	private void closeTCP(){
		try {
			if(s != null) s.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}