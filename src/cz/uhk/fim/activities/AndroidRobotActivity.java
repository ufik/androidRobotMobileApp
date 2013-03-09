package cz.uhk.fim.activities;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramSocket;
import java.net.Socket;
import cz.uhk.fim.R;
import cz.uhk.fim.nxt.NxtRobot;
import cz.uhk.fim.threads.CommandsThread;
import cz.uhk.fim.threads.JpegVideoStreamThread;
import cz.uhk.fim.threads.ServerCommunicationThread;
import cz.uhk.fim.view.CameraPreview;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
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
 * Class handles activity of android robot, contains socket communication (video stream, robot commands) 
 * and bluetooth communication with NXT brick.
 * @author Tomáš Voslař
 */
public class AndroidRobotActivity extends Activity {
	
	/* Connected bluetooth device (NXT robot). */
	BluetoothDevice device;
	/* Decides whether is robot connected. */
	Boolean isRobotConnected = false;
	/* Camera for recording video. */
	private Camera mCamera = null;
	/* Preview of the camera. */
    private CameraPreview mView;
    /* Info text about connection status. */
    public TextView connectionStatus;
    /* Decides whether video is been sending. */
    private boolean isConnected = false;
    /* Socket for communication with server. */
    public Socket communicationSocket;
    public Socket localSocket;
    public Socket localServerSocket;
    
    private JpegVideoStreamThread jvs = null;
    private Thread jvst = null;
    
    /* Decides whether is flashlight on or not. */
    private Boolean isTorchOn = false;
    /* Shared preferences holder */
    SharedPreferences prefs = null;
    
    InputStreamReader test = null;
    
    private MediaRecorder recorder = null;
    
    private FrameLayout preview = null;
    
    private NxtRobot robot = null;
    private Thread robotThread = null;
    
    private OutputStreamWriter robotStreamWriter = null;
    
    private static final int JPEG_STREAM = 0;
    private static final int MPEG4_STREAM = 1;
    ProgressDialog pd;
    private int streamType = 1;
    private int streamFps = 10;
    private int transferType = 0;
    
    private UIThreadHandler mHandler = null;
    
    private ServerCommunicationThread sc = null;
    private Thread sct = null;
	private int cameraWidth;
	private int cameraHeight;
	private int cameraType;
    
	/* After start of this activity. */
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// starts connection with NXT brick
		pd = ProgressDialog.show(AndroidRobotActivity.this, getString(R.string.loading_nxt_title), getString(R.string.loading_nxt_text));
		new Thread(new RobotConnection()).start();
		
		mHandler = new UIThreadHandler();
	}
	
	/* when activity goes to sleep */
	protected void onPause(){
    	super.onPause();
    	
    }
	
	/* when activity goes to sleep */
	protected void onResume(){
    	super.onResume();
    	
    	loadSettings();
    }

	/* when activity is being destroyed */
    protected void onDestroy(){
    	super.onDestroy();
    	
    	cleanUpThreads();
    }
	
	/* Handler for communication with other threads */
	public class UIThreadHandler extends Handler {
	    
    	public void rCommand(final int command, final int value){
    		mHandler.post(new Runnable() {
                public void run() {
                	Log.d("handler", "handler" + String.valueOf(command));
                	robotSendCommand(command, value);
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
    	
    	public void changeStatus(final String text){
    		mHandler.post(new Runnable() {
                public void run() {
                	setStatus("test" + text);
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
		
		// if we are not connected return to previous activity and try another connection
		if(!isRobotConnected){
			Intent i = new Intent(AndroidRobotActivity.this, BluetoothConnectionActivity.class);
			startActivity(i);
			finish();
		}else{
			// save textView into variable for further settings
			connectionStatus = (TextView) findViewById(R.id.connection_status);
		}
	}
	
	/* Local thread for smooth boot of the activity */
	private class RobotConnection implements Runnable{
		public void run() {			
			
			Long lt = System.currentTimeMillis();
			while(System.currentTimeMillis() - lt < 250){} // we need to main UI finish onCreate method, this is not nice, but it works at least
			
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
    		
    		try {
				if(sct != null) sct.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
    	
    	// jpeg stream thread kill
    	if(jvs != null){
    		jvs.terminate();
    		
    		try {
				if(jvst != null) jvst.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
    	
    	// camera and mediaRecorder release
		releaseRecorder();
		// releaseCamera();
		// refresh menu
		invalidateOptionsMenu();
	}
	
	/* Start video thread. */
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
			// TODO no dodelat...
		}
		
		// starts communication thread (TCP socket)
		sc = new ServerCommunicationThread(mHandler, robot);
		sct = new Thread(sc);
		sct.start();
	    
		invalidateOptionsMenu();
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
        
        createToast("Kamera pryc.");
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
        recorder.setVideoFrameRate(streamFps);
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
	
	/* Thread for communication with server. It sends video packets via Socket. */
	private class SendVideoThread implements Runnable{
	    
		public void run(){
	        
			mHandler.post(new Runnable() {
                public void run() {
                	setStatus("Pripojuji...");
                }
            });
			
			// From Server.java
	        try {
	        		//videoSocket = new Socket("192.168.1.11", 1337);
	        		
	                    try{
	                    	mHandler.post(new Runnable() {
	        	                @Override
	        	                public void run() {
	        	                	setStatus("Odesilam data..."); // TODO nahradit stringem
	        	                }
	        	            });
	                    	
	                    	// sends jpeg pictures
	                    	if(streamType == JPEG_STREAM){	               
	                    		
	                    		long lastTime = System.currentTimeMillis();
	                    		//OutputStream os = socket.getOutputStream();
		                    	
		                    	while(isConnected){
		                    		if((System.currentTimeMillis() - lastTime) > (1000 / streamFps)){
		                    			
		                    			byte[] currentFrame = {0};
		                    			
			                    		if(mView != null){
			                    			currentFrame = mView.getCurrentFrame();
			                    		}
	
			                            if(currentFrame != null){
			                            	//os.write(currentFrame,0,currentFrame.length);
			                            	//os.flush();
			                            }
			                    		
			                    		lastTime = System.currentTimeMillis();
                    				}
		                    	}
	                    	
		                    // sends mpeg4 video segments
	                    	}else if(streamType == MPEG4_STREAM){
	                    		
	                    		//final ParcelFileDescriptor pfd = ParcelFileDescriptor.fromSocket(socket);
	                    		
	                    		Log.d("video", "running");
	                    		mHandler.post(new Runnable(){
                                    @Override
                                    public void run(){
                                        
                                    	/*mCamera.unlock();
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
										} catch (Exception e) {
											e.printStackTrace();
										}*/
                                    }
                                
                			});

	                    		while(isConnected){}
	                    	}
	                    	mHandler.post(new Runnable() {public void run() {
		        	                	setStatus("Odpojen."); // TODO nahradit stringem
		        	                	cleanUpThreads();
		        	            }});
	                            
	                            
	                    } catch (Exception e) {
	                    	mHandler.post(new Runnable(){public void run(){
	                            	setStatus("Connection failed."); // TODO nahradit stringem
	                            	cleanUpThreads();
	                        }});
	                        e.printStackTrace();
	                    }
	        } catch (Exception e){
	        	mHandler.post(new Runnable() {public void run() {
	                	setStatus("Error"); // TODO nahradit stringem
	                	cleanUpThreads();
	            }});
	            e.printStackTrace();
	            
	        }	        
	        /*try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
	    }
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
	
	/* This method sets listeners for movement buttons. */
	private void setMovementButtonsActions() {
		//add listeners
		setMovementButtonAction(R.id.button1, NxtRobot.FORWARD, 132);
		setMovementButtonAction(R.id.button2, NxtRobot.BACKWARD, 132);
		setMovementButtonAction(R.id.button3, NxtRobot.RIGHT, 132);
		setMovementButtonAction(R.id.button4, NxtRobot.LEFT, 132);			
	}
	
	/* Change menu items visibility. */
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
	
	/* Create option menu method. */
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	/* On select some of the options. */
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
	 * @return Boolean returns true if was sending succesfull, false otherwise
	 */
	public Boolean robotSendCommand(int command, int value) {
        
		if (robotStreamWriter == null) {
            return false;
        }

        try {
        	
        	String c = command + ";" + value + "\n";
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
	 * Sets connection status textView.
	 * @param text
	 */
	public void setStatus(String text){
		connectionStatus.setText(text);
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

	
}
