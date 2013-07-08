package cz.uhk.fim.threads;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.annotation.SuppressLint;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.ParcelFileDescriptor;
import android.util.Log;
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
 * Class for video streaming h263 video sequence.
 * @author Tomáš Voslař
 */
@SuppressLint("NewApi")
public class H263VideoStreamThread implements Runnable {
	
	/**
	 * Datagram socket for UDP communication.
	 */
	private DatagramSocket ds = null;
	
	/**
	 * Socket for TCP communication.
	 */
	private Socket s = null;
	
	/**
	 * Defines whether is thread running or not.
	 */
	private boolean isRunning;
	
	/**
	 * Handler for communication with main activity.
	 */
	private UIThreadHandler mHandler;
	
	/**
	 * Media recorder instance holder.
	 */
	private MediaRecorder recorder;
	
	/**
	 * Camera instance holder.
	 */
	private Camera mCamera;
	
	/**
	 * Defines frames per second of video sequence.
	 */
	private int mStreamFps;
	
	/**
	 * Camera preview holder.
	 */
	private CameraPreview mView;
	
	/**
	 * Output stream for socket communication.
	 */
	private OutputStream os;
	
	/**
	 * Defines transfer type. (TCP or UDP)
	 */
	private int mTransferType;
	
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
	 * @param camera
	 * @param streamFps
	 * @param transferType
	 * @param view
	 */
	public H263VideoStreamThread(UIThreadHandler handler, Camera camera, int streamFps, int transferType, CameraPreview view) {
		this.mHandler = handler;
		this.mCamera = camera;
		this.mStreamFps = streamFps;
		this.mView = view;
		this.mTransferType = transferType;
	}
	
	/**
	 * Terminates thread.
	 */
	public void terminate(){
		isRunning = false;
		
		releaseRecorder();
		mHandler.destroyCamera(); // TODO stop and destroy recorder then camera
	}
	
	/**
	 *  Starts thread.
	 */
	public void run() {
		isRunning = true;
		
		ParcelFileDescriptor pfd = null;
		
		if(mTransferType == UDP){
			createUDP();
			pfd = ParcelFileDescriptor.fromDatagramSocket(ds);
		}
		else if(mTransferType == TCP){
			createTCP();
			pfd = ParcelFileDescriptor.fromSocket(s);
		}

		mCamera.unlock();
		setRecorderInstance(pfd);
    	
        try {
            recorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        try {
        	recorder.start();
        	
        	while(isRunning){}
        	
		} catch (Exception e) {
			e.printStackTrace();
		}
        
        if(mTransferType == UDP) closeUDP();
		else if(mTransferType == TCP) closeTCP();
	}
	
	/**
	 * Creates new TCP socket.
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
	 * Creates new UDP socket.
	 */
	private void createUDP(){
		// connection
		try {
			ds = new DatagramSocket();
			ds.connect(InetAddress.getByName("31.31.79.28"), 1339);
		} catch (SocketException e) {
			e.printStackTrace();
		}catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns new instance of MediaRecorder with special settings
	 * @param pfd
	 * @return
	 */
	private Boolean setRecorderInstance(ParcelFileDescriptor pfd) {
		
		// instance
		recorder = new MediaRecorder();
        // settings
		recorder.setCamera(mCamera);
		recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);      
        recorder.setOutputFile(pfd.getFileDescriptor());
        recorder.setVideoFrameRate(10);
        recorder.setVideoSize(176,144);
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
        recorder.setPreviewDisplay(mView.getHolder().getSurface());
        return true;
	}
	
	/* Release recorder safely. */
	private void releaseRecorder() {
		if(recorder != null){
			try {
				recorder.stop();
				recorder.reset();
				recorder.release();
				recorder = null;
			} catch (Exception e) {
				Log.d("tag", "nepodarilo se uvolnit mediaRecorder");
				e.printStackTrace();
			}
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
