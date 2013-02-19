package cz.uhk.fim.view;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;


import cz.uhk.fim.MainActivity;
import cz.uhk.fim.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ImageView;

/** A basic Camera preview class */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CAMERA";
	private SurfaceHolder mHolder;
    private Camera mCamera;
    private Context c;
    
    private byte[] currentFrame = null;
    
    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;
        c = context;
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
    }
    
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            if(holder != null && mCamera != null) mCamera.setPreviewDisplay(holder);
            
            mCamera.startPreview();
            Log.d("tag", "start preview");
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (holder.getSurface() == null){
        	mCamera.stopPreview();
          // preview surface does not exist
          return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
          // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
        	
        	mCamera.setPreviewDisplay(holder);
        	
        	Display display = ((WindowManager) c.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        	int rotation = display.getRotation();
                        
            int degrees = 0;
            switch (rotation) {
                case Surface.ROTATION_0: degrees = 0; break;
                case Surface.ROTATION_90: degrees = 90; break;
                case Surface.ROTATION_180: degrees = 180; break;
                case Surface.ROTATION_270: degrees = 270; break;
            }
            
            if(degrees == 0 || degrees == 180)
            	mCamera.setDisplayOrientation(90);
            else
            	mCamera.setDisplayOrientation(0);
            
            mCamera.setPreviewCallback(new PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                	
                    if(data.length > 0){
                    	currentFrame = data;
                    }
                }

            });
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }
    
    public byte[] getCurrentFrame(){
    	if(this.currentFrame != null){
	    	
    		try {
    			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    			ByteArrayOutputStream byteStream2 = new ByteArrayOutputStream();
    			Camera.Parameters parameters = mCamera.getParameters(); 
                Size size = parameters.getPreviewSize(); 
                YuvImage image = new YuvImage(currentFrame, parameters.getPreviewFormat(), size.width, size.height, null); 
    			
                Matrix mat = new Matrix();
	        	mat.postRotate(((MainActivity) c).getRotationDegree());
	        	
    			image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, byteStream);
    	        
    			Bitmap bMap = BitmapFactory.decodeByteArray(byteStream.toByteArray(), 0, byteStream.toByteArray().length);
	        	Bitmap bMapRotate = Bitmap.createBitmap(bMap, 0, 0, bMap.getWidth(), bMap.getHeight(), mat, true);
    			bMapRotate.compress(Bitmap.CompressFormat.JPEG, 70, byteStream2);
	        	
    			return byteStream2.toByteArray();
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
    		
    	}
    	
    	return null;
    }
    
}
  
