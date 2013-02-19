package cz.uhk.fim;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import cz.uhk.fim.nxt.NxtRobot;
import cz.uhk.fim.view.CameraPreview;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Display;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	String device;
	
	private NxtRobot robot;
	
	private Camera mCamera;
	
    private CameraPreview mView;
    public TextView connectionStatus;
    Handler handler = new Handler();
    private boolean sendingVideo = false;
    
    public Socket socket;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		device = "";
		
		// get device name from deviceAcitivity
		if(getIntent().getExtras() != null){
        	device = getIntent().getExtras().getString("device");
        }
		
		if(device.length() == 0){
			
			Intent i = new Intent(MainActivity.this, BtDeviceActivity.class);
			startActivity(i);
			
			finish();
		}else{
			
			// create connection with device
			robot = new NxtRobot();
			if(robot.createConnection(device)){
				Toast t = Toast.makeText(this, R.string.nxt_connection_success, 1000);
				t.show();
				
				setMovementButtonsActions();
			}else{
				Toast t = Toast.makeText(this, R.string.nxt_connection_failed, 1000);
				t.show();
			}
			
		// video streaming
		mCamera = getCameraInstance();
		
		Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(176, 144);
        mCamera.setParameters(parameters);
			
		connectionStatus = (TextView) findViewById(R.id.connection_status);
		
		/*mView = (VideoView) findViewById(R.id.videoView1);
	    mHolder = mView.getHolder();
	    mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);*/
	    
		mView = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mView);
	    
        Button btnUp = (Button) findViewById(R.id.button5);
		btnUp.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				 if(!sendingVideo){
					 sendingVideo = true;
					 
					// Run new thread to handle socket communications
				    Thread sendVideo = new Thread(new SendVideoThread());
				    sendVideo.start();
				    
				 }else{
					 sendingVideo = false;
					 
					 try {
						socket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				 }
				    
			}});
		}	
	}
	
	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance(){
	    Camera c = null;
	    try {
	        c = Camera.open(); // attempt to get a Camera instance
	    }
	    catch (Exception e){
	       
	    }
	    return c; // returns null if camera is unavailable
	}
	
	public int getRotationDegree(){
    	
    	Display display = ((WindowManager) MainActivity.this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
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
	
	public class CommandsThread implements Runnable{
		
		private BufferedReader in;
		String message = "";
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			// read commands for robot
            try {
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
            int charsRead = 10;
            
            char[] buffer = new char[10];
            
            try {
				while ((charsRead = in.read(buffer)) != -1) {
				    message = new String(buffer).substring(0, charsRead);
				    
				    handler.post(new Runnable(){

						@Override
						public void run() {
							
							robot.sendCommand(NxtRobot.STOP, 132);
							
							int command = 0;
							connectionStatus.setText(message);
							
							if(message.toString().equals("command-1")){
								command = NxtRobot.FORWARD;
							}else if(message.equals("command-2")){
								command = NxtRobot.BACKWARD;
							}else if(message.equals("command-3")){
								command = NxtRobot.LEFT;
							}else if(message.equals("command-4")){
								command = NxtRobot.RIGHT;
							}
							
							robot.sendCommand(command, 132);
							
							
						}
				    	
				    });
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * Video stream section
	 */
	public class SendVideoThread implements Runnable{
	    
		public void run(){
	        
			handler.post(new Runnable() {
                @Override
                public void run() {
                    connectionStatus.setText("Pripojeni...");
                }
            });
			
			// From Server.java
	        try {
	        		socket = new Socket("192.168.1.11", 1337);

	        		Thread commands = new Thread(new CommandsThread());
				    commands.start();
				    
	                    try{

	                    	handler.post(new Runnable() {
	        	                @Override
	        	                public void run() {
	        	                    connectionStatus.setText("Odesilam data...");
	        	                }
	        	            });
	                    	
	                    	long lastTime = System.currentTimeMillis();
	                    	
	                    	while(sendingVideo){

	                    		if((System.currentTimeMillis() - lastTime) > 100){
	                    			
		                    		final byte[] currentFrame = mView.getCurrentFrame();
		                    		final Bitmap bMap = BitmapFactory.decodeByteArray(currentFrame, 0, currentFrame.length);
		                    		
		                    		handler.post(new Runnable() {
			        	                @Override
			        	                public void run() {
			        	                    ImageView iv = (ImageView) findViewById(R.id.imageView1);
			        	                    iv.setImageBitmap(bMap);
			        	                }
			        	            });
		                    		
		                    		OutputStream os = socket.getOutputStream();
			                        
		                            os.write(currentFrame,0,currentFrame.length);
		                    		os.flush();
		                    		
		                    		lastTime = System.currentTimeMillis();
	                    		}

	                    	}
	                            
	                            handler.post(new Runnable() {
		        	                @Override
		        	                public void run() {
		        	                    connectionStatus.setText("Odpojen.");
		        	                }
		        	            });
	                            
	                            
	                    } catch (Exception e) {
	                        handler.post(new Runnable(){
	                            @Override
	                            public void run(){
	                                connectionStatus.setText("Connection failed.");
	                            }
	                        });
	                        e.printStackTrace();
	                    }
	                


	        } catch (Exception e){
	            handler.post(new Runnable() {
	                @Override
	                public void run() {
	                    connectionStatus.setText("Error");
	                    
	                }
	            });
	            e.printStackTrace();
	            
	        }
	        // End from server.java
	    }
	}
	
	public void test(Bitmap m){
		 ImageView iv = (ImageView) findViewById(R.id.imageView1);
		 iv.setImageBitmap(m);
	}
	
	private void setMovementButtonsActions() {
		//add listeners
				Button btnUp = (Button) findViewById(R.id.button1);
				btnUp.setOnTouchListener(new View.OnTouchListener() {
			        @Override
			        public boolean onTouch(View arg0, MotionEvent arg1) {
			            if (arg1.getAction()==MotionEvent.ACTION_DOWN){
			            	
			            	if(!robot.sendCommand(NxtRobot.FORWARD, 132)){
			            		Toast t = Toast.makeText(MainActivity.this, "Chyba spojeni", 1000);
				            	t.show();
			            	}
			            	
			            }else if(arg1.getAction() == MotionEvent.ACTION_UP){

			            	if(!robot.sendCommand(NxtRobot.STOP, 0)){
			            		Toast t = Toast.makeText(MainActivity.this, "Chyba spojeni", 1000);
				            	t.show();
			            	}
			            }

			            return true;
			        }
			    });
				
				Button btnRight = (Button) findViewById(R.id.button3);
				btnRight.setOnTouchListener(new View.OnTouchListener() {
			        @Override
			        public boolean onTouch(View arg0, MotionEvent arg1) {
			            if (arg1.getAction()==MotionEvent.ACTION_DOWN){
			            	
			            	if(!robot.sendCommand(NxtRobot.RIGHT, 132)){
			            		Toast t = Toast.makeText(MainActivity.this, "Chyba spojeni", 1000);
				            	t.show();
			            	}
			            	
			            }else if(arg1.getAction() == MotionEvent.ACTION_UP){
			            	
			            	if(!robot.sendCommand(NxtRobot.STOP, 0)){
			            		Toast t = Toast.makeText(MainActivity.this, "Chyba spojeni", 1000);
				            	t.show();
			            	}
			            }

			            return true;
			        }
			    });
				
				Button btnDown = (Button) findViewById(R.id.button2);
				btnDown.setOnTouchListener(new View.OnTouchListener() {
			        @Override
			        public boolean onTouch(View arg0, MotionEvent arg1) {
			            if (arg1.getAction()==MotionEvent.ACTION_DOWN){
			            	
			            	if(!robot.sendCommand(NxtRobot.BACKWARD, 132)){
			            		Toast t = Toast.makeText(MainActivity.this, "Chyba spojeni", 1000);
				            	t.show();
			            	}
			            	
			            }else if(arg1.getAction() == MotionEvent.ACTION_UP){
			            	
			            	if(!robot.sendCommand(NxtRobot.STOP, 0)){
			            		Toast t = Toast.makeText(MainActivity.this, "Chyba spojeni", 1000);
				            	t.show();
			            	}
			            }

			            return true;
			        }
			    });
				
				Button btnLeft = (Button) findViewById(R.id.button4);
				btnLeft.setOnTouchListener(new View.OnTouchListener() {
			        @Override
			        public boolean onTouch(View arg0, MotionEvent arg1) {
			            if (arg1.getAction()==MotionEvent.ACTION_DOWN){
			            	
			            	if(!robot.sendCommand(NxtRobot.LEFT, 132)){
			            		Toast t = Toast.makeText(MainActivity.this, "Chyba spojeni", 1000);
				            	t.show();
			            	}
			            	
			            }else if(arg1.getAction() == MotionEvent.ACTION_UP){
			            	
			            	if(!robot.sendCommand(NxtRobot.STOP, 0)){
			            		Toast t = Toast.makeText(MainActivity.this, "Chyba spojeni", 1000);
				            	t.show();
			            	}
			            }

			            return true;
			        }
			    });
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

}
