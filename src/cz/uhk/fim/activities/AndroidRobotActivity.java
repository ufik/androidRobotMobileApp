package cz.uhk.fim.activities;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import cz.uhk.fim.R;
import cz.uhk.fim.helpers.TrackPoint;
import cz.uhk.fim.helpers.TrackPointItem;
import cz.uhk.fim.nxt.NxtRobot;
import cz.uhk.fim.threads.H263VideoStreamThread;
import cz.uhk.fim.threads.JpegVideoStreamThread;
import cz.uhk.fim.threads.ServerCommunicationThread;
import cz.uhk.fim.view.CameraPreview;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

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
 * Class handles activity of android robot, contains socket communication (video stream, robot commands) 
 * and bluetooth communication with NXT brick.
 * @author Tomáš Voslař
 */
public class AndroidRobotActivity extends Activity implements SensorEventListener, LocationListener {
	
	/**
	 *  Connected bluetooth device (NXT robot). 
	 */
	private BluetoothDevice device;
	
	/**
	 *  Decides whether is robot connected. 
	 */
	private Boolean isRobotConnected = false;
	
	/**
	 *  Camera for recording video. 
	 */
	private Camera mCamera = null;
	
	/**
	 *  Preview of the camera. 
	 */
    private CameraPreview mView;
    
    /**
     *  Info text about connection status. 
     */
    public TextView connectionStatus;
    
    /**
     *  Decides whether video is been sending. 
     */
    private boolean isConnected = false;
    
    /**
     *  Socket for communication with server. 
     */
    public Socket communicationSocket;
    
    /**
     * Local communication socket for video sequence.
     */
    public Socket localSocket;
    
    /**
     * Local server socket for video sequence.
     */
    public Socket localServerSocket;
    
    /**
     * Thread class for jpeg streaming.
     */
    private JpegVideoStreamThread jvs = null;
    
    /**
     * Thread with jpeg streaming class.
     */
    private Thread jvst = null;
    
    /**
     *  Decides whether is flashlight on or not. 
     */
    private Boolean isTorchOn = false;
    
    /**
     *  Shared preferences holder 
     */
    private SharedPreferences prefs = null;
    
    /**
     * Frame layout for preview of camera.
     */
    private FrameLayout preview = null;
    
    /**
     * Robot class instance.
     */
    private NxtRobot robot = null;
    
    /**
     * Stream writer for Bluetooth communication.
     */
    private OutputStreamWriter robotStreamWriter = null;
    
    /**
     * Jpeg stream constant.
     */
    private static final int JPEG_STREAM = 0;
    
    /**
     * Mpeg stream constant.
     */
    private static final int MPEG4_STREAM = 1;
    
    /**
     * Progress dialog holder.
     */
    private ProgressDialog pd;
    
    /**
     * Defines type of video streaming.
     */
    private int streamType = 1;
    
    /**
     * Defines frames per second for video sequence.
     */
    private int streamFps = 10;
    
    /**
     * Defines type of streaming protocol.
     */
    private int transferType = 0;
    
    /**
     * Handler for communication outside of this activity.
     */
    private UIThreadHandler mHandler = null;
    
    /**
     * Thread class with server communication.
     */
    private ServerCommunicationThread sc = null;
    
    /**
     * Thread for server communication class.
     */
    private Thread sct = null;
    
    /**
     * Resolution width for images.
     */
	private int cameraWidth;
	
	/**
	 * Resolution height for images.
	 */
	private int cameraHeight;
	
	/**
	 * Type of camera, defines front or back sided camera.
	 */
	private int cameraType;
	
	/**
	 * Thread class for h263 streaming.
	 */
	private H263VideoStreamThread hvs;
	
	/**
	 * Thread for h263 streaming class.
	 */
	private Thread hvst;
    
	/**
	 * Location manager for gps location.
	 */
	private LocationManager lm = null;
	
	/**
	 * Location listener for gps location.
	 */
	private LocationListener ls = null;
	
	/**
	 * Sensor manager.
	 */
	private SensorManager sm;
	
	/**
	 * Sensor instance.
	 */
	private Sensor s;
	
	/**
	 * Speed units.
	 */
	private int units = 0;
	
	/**
	 * Horizontal direcition of device.
	 */
	private float azimuth = 0;
	
	/**
	 *  After start of this activity. 
	 */
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		loadGpsAndSensor();
		
		// starts connection with NXT brick
		pd = ProgressDialog.show(AndroidRobotActivity.this, getString(R.string.loading_nxt_title), getString(R.string.loading_nxt_text));
		new Thread(new RobotConnection()).start();
		
		mHandler = new UIThreadHandler();
		
	}
	
	/**
	 * Loads gps manager and sensor manager.
	 */
	private void loadGpsAndSensor(){
		// Acquire a reference to the system Location Manager
		lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		// Register the listener with the Location Manager to receive location updates
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		
		sm = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
	    s = sm.getDefaultSensor(Sensor.TYPE_ORIENTATION);
	    sm.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	/* when activity goes to sleep */
	protected void onPause(){
    	super.onPause();
    	
    	releaseCamera();
    	
    	if(ls != null) lm.removeUpdates(ls);
		sm.unregisterListener(this);
    }
	
	/* when activity goes to sleep */
	protected void onResume(){
    	super.onResume();
    	
    	loadSettings();
    	setCameraInstance();
    	
    	loadGpsAndSensor();
		 
    }

	/* when activity is being destroyed */
    protected void onDestroy(){
    	super.onDestroy();
    	
    	cleanUpThreads();
    	
    	if(ls != null) lm.removeUpdates(ls);
		sm.unregisterListener(this);
    }
	
	/**
	 *  Handler for communication with other threads 
	 */
	public class UIThreadHandler extends Handler {
	    
    	public void rCommand(final int speed, final int steering){
    		mHandler.post(new Runnable() {
                public void run() {
                	Log.d("handler", "handler" + String.valueOf(speed));
                	robotSendCommand(speed, steering);
                }
            });
    	}
    	
    	public void tTorch(){
    		mHandler.post(new Runnable() {
                public void run() {
                	toggleTorch();
                }
            });
    	}
    	
    	public void changeStatus(final int status, final String text){
    		mHandler.post(new Runnable() {
                public void run() {
                	setTextValue(status, text);
                }
            });
    	}
    	
    	public void destroyCamera(){
    		mHandler.post(new Runnable() {
                public void run() {
                	releaseCamera();
                }
            });
    	}
	}
		
	/* Tries to create connection between NXT brick and Android device */
	private void initConnection(){
		// get device name from BluetoothDeviceActivity
		if(getIntent().getExtras() != null && !isRobotConnected){
			device = getIntent().getExtras().getParcelable("device");
        	Log.d("test", "onCreate invoked");
			
        	// create new instance of nxtRobot
        	robot = new NxtRobot(mHandler, device);
        	
        	// try to connect via bluetooth
			if((robotStreamWriter = robot.createConnection(device)) != null){
				// show info to user
				createToast(R.string.nxt_connection_success);
				
				// after sucessfull connection, we can run thread with nxt reader
				Thread robotThread = new Thread(robot);
	        	robotThread.start();
				
				// we are now connected
				isRobotConnected = true;
				setMovementButtonsActions();
			}else{
				createToast(R.string.nxt_connection_failed);
				isRobotConnected = true; // TODO pozor prepsat na false po testovani
			}
        }
		
		if(pd != null) pd.dismiss();
		loadSettings();
		
		
		//setCameraInstance();
		
		// if we are not connected return to previous activity and try another connection
		if(!isRobotConnected){
			Intent i = new Intent(AndroidRobotActivity.this, BluetoothConnectionActivity.class);
			startActivity(i);
			finish();
		}
	}
	
	/* Local thread for smooth boot of the activity */
	private class RobotConnection implements Runnable{
		public void run() {			
			
			Long lt = System.currentTimeMillis();
			while(System.currentTimeMillis() - lt < 250){} // we need to main UI finish onCreate method
			
			mHandler.post(new Runnable(){
				public void run() {
					initConnection();
				}});
		}
	}
	
	/* Load sharedPreferences in local variables */
	private void loadSettings(){
		// get shared preferences
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		// set type of video stream
		streamType = Integer.parseInt(prefs.getString("pref_video_type", "0"));
		streamFps = Integer.parseInt(prefs.getString("pref_fps", "10"));
		transferType = Integer.parseInt(prefs.getString("pref_transfer_type", "0"));
		cameraType = Integer.parseInt(prefs.getString("pref_camera_type", "0"));
		units = prefs.getInt("units", 0); // TODO prepsat ostatni nastaveni na tento tvar
		
		String size = prefs.getString("pref_camera_size", "176x144");
        
        int index = size.indexOf("x");
        
        if(index != -1){
	        cameraWidth = Integer.valueOf(size.substring(0, index));
	        cameraHeight = Integer.valueOf(size.substring(index + 1, size.length()));
        }
	}
	
    /* Turned on/off connection with server via Socket */
	private void toggleServerConnection(){
		
		if(!isConnected){
			if(mCamera == null) setCameraInstance();
			turnOnSocket();
		 }else{
			cleanUpThreads();
		 }
	}
	/* Stop video thread and release camera. */
	@SuppressLint("NewApi")
	private void cleanUpThreads(){
		isConnected = false;
		
		// robot thread kill
    	/*if(robot != null){
    		robot.terminate();
    		
    		try {
				if(robotThread != null) robotThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}*/
		
    	// serverCommunication thread kill
    	if(sc != null){
    		sc.terminate();
    		
    		/*try {
				if(sct != null) sct.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
    	}
    	
    	// jpeg stream thread kill
    	if(jvs != null){
    		jvs.terminate();
    		
    		/*try {
				if(jvst != null) jvst.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
    	}
    	
    	// h263 video stream thread kill
    	if(hvs != null){
    		hvs.terminate();
    		
    		/*try {
				if(hvst != null) hvst.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
    	}
    	
    	releaseCamera();
    	// camera and mediaRecorder release
		//releaseRecorder();
		// releaseCamera();
		// refresh menu
    	if (Integer.parseInt(Build.VERSION.SDK) >= 11) invalidateOptionsMenu();
	}
	
	/* Start video thread. */
	@SuppressLint("NewApi")
	private void turnOnSocket(){
		isConnected = true;
		
		// Run new thread to handle socket communications
	    //Thread sendVideo = new Thread(new SendVideoThread());
		
	    //sendVideo.start();
		if(streamType == JPEG_STREAM){
			jvs = new JpegVideoStreamThread(mHandler, mView, streamFps, transferType);
			jvst = new Thread(jvs);
			jvst.start();
		}else if(streamType == MPEG4_STREAM){
			hvs = new H263VideoStreamThread(mHandler, mCamera, streamFps, transferType, mView);
			hvst = new Thread(hvs);
			hvst.start();
		}
		
		// starts communication thread (TCP socket)
		sc = new ServerCommunicationThread(mHandler, robot);
		sct = new Thread(sc);
		sct.start();
	    
		if (Integer.parseInt(Build.VERSION.SDK) >= 11) invalidateOptionsMenu();
	}

	/* Turned on or off flashlight of the camera. */
	private void toggleTorch(){
		
		if(mCamera != null){
			if(!isTorchOn){
				Camera.Parameters parameters = mCamera.getParameters();
		        parameters.setFlashMode( Camera.Parameters.FLASH_MODE_TORCH );
		        mCamera.setParameters(parameters);
		        isTorchOn = true;
			}else{
				Camera.Parameters parameters = mCamera.getParameters();
		        parameters.setFlashMode( Camera.Parameters.FLASH_MODE_OFF );
		        mCamera.setParameters(parameters);
		        isTorchOn = false;
			}
		}else{
			Log.d("torch", "neni pristupna kamera");
		}
	}
	
	/**
	 * Sets preview of the camera
	 * @return
	 */
	private Boolean setPreview(){
		if(mCamera != null){
		    	    
			mView = new CameraPreview(this, mCamera);
			preview = (FrameLayout) findViewById(R.id.camera_preview);
	        preview.addView(mView);

		    return true;
	    }else{
	    	return false;
	    }
	}
	
	/**
	 * A safe way to get an instance of the Camera object. 
	 * @return Camera an instance of camera
	 */
	private boolean setCameraInstance(){
	    
	    try {
	        mCamera = Camera.open(cameraType); // attempt to get a Camera instance
	        
	        // sets camera options and camera preview			
 			Camera.Parameters parameters = mCamera.getParameters();
 	        parameters.setPreviewSize(cameraWidth, cameraHeight);
 	        mCamera.setParameters(parameters);

 	    	setPreview();
	    }catch (Exception e){
	       return false;
	    }
	    
	    return true;
	}
	
	/* Release camera for other applications and turn off preview */
	private void releaseCamera(){
		
		if(mView != null) preview.removeView(mView);
		
        if (mCamera != null){
        	mCamera.stopPreview();
        	mCamera.setPreviewCallback(null); 
        	mCamera.release();
            mCamera = null;
        }
        
        Log.d("camerea","Camera has been destroyed.");
    }
	
	/**
	 * Get actual rotation of display in degrees.
	 * @return int degrees
	 */
	public int getRotationDegree(){
    	
    	Display display = ((WindowManager) AndroidRobotActivity.this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    	int rotation = display.getRotation();
                    
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        
		if(degrees == 0 || degrees == 180)
			rotation = 90;
        else
        	rotation = 0;
        
		return rotation;
    }
	
	/**
	 * Sets ultrasonic values, gps and video status.
	 */
	private void setTextValue(int text, String value){
		
		TextView status = null;
		
		switch (text) {
		case 0: // ultra sensor
			status = (TextView) findViewById(R.id.ultra_status);
			break;
		case 1: // GPS
			status = (TextView) findViewById(R.id.gps_status);
			break;
		case 2: // video transfer
			status = (TextView) findViewById(R.id.video_status);
			break;
		}
		
		status.setText(value);
	}
	
	/**
	 * Adds listener to button with given command and value for robot manipulating.
	 * @param idString
	 * @param command
	 * @param value
	 */
	private void setMovementButtonAction(int idString, final int command, final int value){
		Button btnUp = (Button) findViewById(idString);
		btnUp.setOnTouchListener(new View.OnTouchListener() {
	        @Override
	        public boolean onTouch(View arg0, MotionEvent arg1) {
	            if (arg1.getAction()==MotionEvent.ACTION_DOWN){
	            	
	            	if(!robotSendCommand(command, value))
	            		createToast(R.string.robot_connection_error);
	            	
	            }else if(arg1.getAction() == MotionEvent.ACTION_UP){

	            	if(!robotSendCommand(command, 0))
	            		createToast(R.string.robot_connection_error);
	            }
	            return true;
	        }
	    });
	}
	
	/* This method sets listeners for movement buttons. Only for testing */
	private void setMovementButtonsActions() {
		//add listeners
		/*setMovementButtonAction(R.id.button1, NxtRobot.FORWARD, 132);
		setMovementButtonAction(R.id.button2, NxtRobot.BACKWARD, 132);
		setMovementButtonAction(R.id.button3, NxtRobot.RIGHT, 132);
		setMovementButtonAction(R.id.button4, NxtRobot.LEFT, 132);	*/		
	}
	
	/**
	 *  Change menu items visibility. 
	 */
	public boolean onPrepareOptionsMenu(Menu menu){
		
		MenuItem connect = menu.findItem(R.id.menu_connect);
		MenuItem disconnect = menu.findItem(R.id.menu_disconnect);

        if(isConnected){
	        connect.setVisible(false);
	        disconnect.setVisible(true);
        }else{
        	connect.setVisible(true);
            disconnect.setVisible(false);
        }

		return true;
	}
	
	/**
	 *  Create option menu method. 
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	/**
	 *  On select some of the options. 
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.menu_connect:
	        	
	            toggleServerConnection();
	       	
	            return true;
	        case R.id.menu_disconnect:
	        	
	            toggleServerConnection();
	       	
	            return true;    
	        case R.id.menu_exit:
	            
	        	finish();
	        	
	            return true;
	        case R.id.menu_settings:
	            
	        	Intent i = new Intent(AndroidRobotActivity.this, PreferencesActivity.class);
				startActivity(i);
				
				cleanUpThreads();
	        	
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	/**
	 * Sends command via BluetoothSocket.
	 * @param int command
	 * @param int value
	 * @return Boolean returns true if was sending successful, false otherwise
	 */
	public Boolean robotSendCommand(int speed, int steering) {
        
		if (robotStreamWriter == null) {
            return false;
        }

        try {
        	
        	String c = speed + ";" + steering + "\n";
        	Log.d("robot", "command=" + c);
        	robotStreamWriter.write(c);
        	robotStreamWriter.flush();
        	Log.d("robot", "command=end");
            return true;
        } catch (IOException ioe) { 
            return false;            
        }
    }	
	
	/**
	 * Create toast which is immediately shown to user.
	 * @param resId
	 */
	private void createToast(String text){
		
		Toast t = Toast.makeText(AndroidRobotActivity.this, text, Toast.LENGTH_LONG);
		t.show();
	}
	
	/**
	 * Create toast which is immediately shown to user.
	 * @param resId
	 */
	private void createToast(int resId){
		
		Toast t = Toast.makeText(AndroidRobotActivity.this, resId, Toast.LENGTH_LONG);
		t.show();
	}
	
	/**
	 * Create toast, which is immediately shown to user.
	 * @param resId
	 * @param length
	 */
	private void createToast(int resId, int length){
		
		Toast t = Toast.makeText(AndroidRobotActivity.this, resId, length);
		t.show();
	}

	/**
	 * When location is changed this methods is fired.
	 */
	public void onLocationChanged(Location location) {
		updateLocation(location);
	}
	
	/**
	 * Gets and saves location values.
	 * @param location
	 */
	private void updateLocation(Location location) {
		
		TrackPointItem tpi = new TrackPointItem(location.getLongitude(), location.getLatitude(), location.getTime());
		
		TrackPoint tp = new TrackPoint();
		tp.addPoint(tpi);
		
		float speedFinal = 0;
		
		if(location.getSpeed() == 0.0){
			speedFinal = tp.getSpeed();
		}else{
			speedFinal = location.getSpeed();
		}
		
		TextView speed = (TextView) this.findViewById(R.id.speed);
		speed.setText(tp.calculateInto(speedFinal, units));
		
		TextView gps = (TextView) this.findViewById(R.id.gps_status);
		gps.setText("long: " + location.getLongitude() + " lat: " + location.getLatitude());
		
		if(robot != null) robot.setPosition(location.getLongitude() + ":" + location.getLatitude());
	}

	/**
	 *  Not used. 
	 */
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	/**
	 *  Not used. 
	 */
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	/**
	 *  Not used. 
	 */
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}

	/**
	 *  Not used. 
	 */
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	/**
	 *  When sensor value is changed. 
	 */
	public void onSensorChanged(SensorEvent event) {
		azimuth = event.values[0];
		updateCompass();
	}
	
	/* Update compass value. */
	private void updateCompass(){
		if(azimuth >= 0 && azimuth < 45){
			setAzimuth("S");
		}else if(azimuth >= 45 && azimuth < 90) {
			setAzimuth("SV");
		}else if(azimuth >= 90 && azimuth < 135) {
			setAzimuth("V");
		}else if(azimuth >= 180 && azimuth < 225) {
			setAzimuth("JV");
		}else if(azimuth >= 225 && azimuth < 270) {
			setAzimuth("J");
		}else if(azimuth >= 270 && azimuth < 315) {
			setAzimuth("JZ");
		}else if(azimuth >= 315 && azimuth < 360) {
			setAzimuth("Z");
		}
	}
	
	/* Sets text with azimuth value. */
	private void setAzimuth(String azimuth){
		TextView tc = (TextView) this.findViewById(R.id.compass);
		
		tc.setText(azimuth);
		if(robot != null) robot.setAzimuth(azimuth);
	}
}
