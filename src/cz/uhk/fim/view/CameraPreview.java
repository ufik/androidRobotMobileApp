package cz.uhk.fim.view;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import cz.uhk.fim.activities.AndroidRobotActivity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

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
            Log.d(TAG, "start preview");
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    /**
     * When surface is changed this method is invoked.
     */
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
          // ignore: tried to stop a non-existent preview
        	e.printStackTrace();
        }

        // start preview with new settings
        try {
        	
        	mCamera.setPreviewDisplay(holder);
        	mCamera.setDisplayOrientation(90);
        	
        	// this method save image frame into variable
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
    
    /**
     * Returns one image frame from camera preview.
     * @return byteArray image
     */
    public byte[] getCurrentFrame(){
    	if(this.currentFrame != null){
	    	
    		try {
    			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    			ByteArrayOutputStream byteStream2 = new ByteArrayOutputStream();
    			Camera.Parameters parameters = mCamera.getParameters(); 
                Size size = parameters.getPreviewSize(); 
                YuvImage image = new YuvImage(currentFrame, parameters.getPreviewFormat(), size.width, size.height, null); 
    			
                Matrix mat = new Matrix();
	        	mat.postRotate(((AndroidRobotActivity) c).getRotationDegree());
	        	
    			image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, byteStream);
    	        
    			Bitmap bMap = BitmapFactory.decodeByteArray(byteStream.toByteArray(), 0, byteStream.toByteArray().length);
	        	Bitmap bMapRotate = Bitmap.createBitmap(bMap, 0, 0, bMap.getWidth(), bMap.getHeight(), mat, true);
    			bMapRotate.compress(Bitmap.CompressFormat.JPEG, 70, byteStream2);
	        	
    			return byteStream2.toByteArray();
			} catch (Exception e) {
				// TODO: handle exception
				Log.d(TAG, "Error while creating image.");
				e.printStackTrace();
			}
    	}
    	
    	return null;
    }
    
}
  
