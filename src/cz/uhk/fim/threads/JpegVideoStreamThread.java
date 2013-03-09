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
 * Thread for sending jpeg pictures via Socket (UDP/TCP).
 * @author Tomáš Voslař
 *TODO documentation
 */
public class JpegVideoStreamThread implements Runnable {
	
	private DatagramSocket ds = null;
	private Socket s = null;
	private OutputStream os = null;
	private UIThreadHandler mHandler = null;
	private CameraPreview mCp = null;
	private Boolean isRunning = false;
	private int mStreamFps = 0;
	private int mTransferType = 0;
	
	private static final int UDP = 0;
	private static final int TCP = 1;
	
	public JpegVideoStreamThread(UIThreadHandler handler, CameraPreview cp, int streamFps, int transferType) {
		mHandler = handler;
		mCp = cp; 
		mStreamFps = streamFps;
		mTransferType = transferType;
	}
	
	public void terminate(){
		isRunning = false;
		
		mHandler.destroyCamera();
	}

	@Override
	public void run() {
		isRunning = true;
		
		if(mTransferType == UDP) createUDP();
		else if(mTransferType == TCP) createTCP();
		
		// transfer
		long lastTime = System.currentTimeMillis();
		while(isRunning){

        		if((System.currentTimeMillis() - lastTime) > (1000 / mStreamFps)){
        			
        			byte[] currentFrame = {0};
        			
            		if(mCp != null){
            			currentFrame = mCp.getCurrentFrame();
            		}

                    if(currentFrame != null){
                    	if(mTransferType == UDP) sendUDPPacket(currentFrame);
                		else if(mTransferType == TCP) sendTCPPacket(currentFrame);
                    }
            		lastTime = System.currentTimeMillis();
				}
		}
		
		if(mTransferType == UDP) closeUDP();
		else if(mTransferType == TCP) closeTCP();
	}
	
	private void createTCP(){
		try {
			s = new Socket("192.168.1.11", 1337);
			os = s.getOutputStream();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void createUDP(){
		// connection
		try {
			ds = new DatagramSocket();
			ds.connect(InetAddress.getByName("192.168.1.11"), 1400);
		} catch (SocketException e) {
			e.printStackTrace();
		}catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
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
	
	private void sendUDPPacket(byte[] frame){
		DatagramPacket packet = new DatagramPacket(frame, frame.length);
    	
    	try {
			if(ds != null) ds.send(packet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void closeUDP(){
		if(ds != null) ds.close();
	}
	
	private void closeTCP(){
		try {
			if(s != null) s.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
