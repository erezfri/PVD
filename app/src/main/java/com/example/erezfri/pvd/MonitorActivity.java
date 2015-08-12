package com.example.erezfri.pvd;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;


public class MonitorActivity extends ActionBarActivity{

    private static final String UUID_SERIAL_PORT_PROFILE
            = "00001101-0000-1000-8000-00805F9B34FB";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CONNECT_DEVICE=2;
    private BluetoothAdapter myBluetoothAdapter;
    private Boolean BluetoothCond = false;

    static final int REQUEST_VIDEO_CAPTURE = 1;

    // Member object for the bluetooth services
    private BluetoothService mBTService = null;
    // Connection mechanism (side)
    private int mConnectSide = BluetoothService.CLIENT;
    // Name of the connected device
    private String mConnectedDeviceName = null;

    private BluetoothSocket mSocket = null;
    private BufferedReader mBufferedReader = null;

    //start for camera preview
    private Camera mCamera;
    private TextureView mTextureView;
    //end for camera preview

    // the scope graph customized view object
    private PlotDynamic mGraph;
    private ArrayList<byte[]> Packets;

    //for the camera
    private Context myContext;
    private boolean cameraFront = false;
    private CameraPreview mPreview;
    private LinearLayout cameraPreview;
    private Button capture;
    private MediaRecorder mediaRecorder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.monitor_screen);

        // take an instance of BluetoothAdapter - Bluetooth radio
        if (myBluetoothAdapter==null){
            myBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        }
        if (myBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(getApplicationContext(), "Device does not support Bluetooth",
                    Toast.LENGTH_SHORT).show();
        }

        //start for camera preview
       // mTextureView = (TextureView)findViewById(R.id.textureView) ;
       // mTextureView.setSurfaceTextureListener(this);
        //setContentView(mTextureView);
        //end for camera preview

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext = this;
        initialize();


    }
    private int findBackFacingCamera() {
        int cameraId = -1;
        // Search for the back facing camera
        // get the number of cameras
        int numberOfCameras = Camera.getNumberOfCameras();
        // for every camera check
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                cameraFront = false;
                break;
            }
        }
        return cameraId;
    }

      public void onSearchButtonClick(View view) {
        if (!myBluetoothAdapter.isEnabled()) {
            Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);
//
//            Toast.makeText(getApplicationContext(), "Bluetooth turned on",
//                    Toast.LENGTH_LONG).show();
        }else {
            BluetoothCond=true;
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        }


    }


    public void Plot(){
        //Initialize view variables
        mGraph = new PlotDynamic(this,1,1);
        mGraph.setTitle("Test");
        mGraph.setColor("123".split(""), PlotDynamic.Colorpart.plots);
        mGraph.setColor("1".split(""), PlotDynamic.Colorpart.backgroud);
        mGraph.setColor("2".split(""), PlotDynamic.Colorpart.grid);


        //bulid view
        LinearLayout Scope = new LinearLayout(this);
        Scope.setOrientation(LinearLayout.VERTICAL);
        Scope.addView(mGraph);
        setContentView(Scope);

        Packets = new ArrayList<byte[]>();
        return;
    }
    //start for camera preview
//    @Override
//    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//        mCamera = Camera.open();
//
//        Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
//        mTextureView.setLayoutParams(new FrameLayout.LayoutParams(
//                previewSize.width / 2, previewSize.height / 2, Gravity.BOTTOM));
//
//        try {
//            mCamera.setPreviewTexture(surface);
//        } catch (IOException t) {
//        }
//
//        mCamera.startPreview();
//        mCamera.setDisplayOrientation(90);
//
//    }
//    @Override
//    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
//        // Ignored, the Camera does all the work for us
//    }
//    @Override
//    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
//        mCamera.stopPreview();
//        mCamera.release();
//        return true;
//    }
//    @Override
//    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//        // Update your view here!
//    }
//    //end for camera preview


    public void onResume() {
        super.onResume();
        if (!hasCamera(myContext)) {
            Toast toast = Toast.makeText(myContext, "Sorry, your phone does not have a camera!", Toast.LENGTH_LONG);
            toast.show();
            finish();
        }
        if (mCamera == null) {
            // if the front facing camera does not exist
//            if (findFrontFacingCamera() < 0) {
//                Toast.makeText(this, "No front facing camera found.", Toast.LENGTH_LONG).show();
//               // switchCamera.setVisibility(View.GONE);
//            }
            mCamera = Camera.open(findBackFacingCamera());
            mCamera.setDisplayOrientation(90);
            mPreview.refreshCamera(mCamera);
        }
    }
    public void initialize() {
        cameraPreview = (LinearLayout) findViewById(R.id.camera_preview);

        mPreview = new CameraPreview(myContext, mCamera);
        cameraPreview.addView(mPreview);


        capture = (Button) findViewById(R.id.button_capture);
        capture.setOnClickListener(captrureListener);

       // switchCamera = (Button) findViewById(R.id.button_ChangeCamera);
       // switchCamera.setOnClickListener(switchCameraListener);
    }
    @Override
    protected void onPause() {
        super.onPause();
        // when on Pause, release camera in order to be used from other
        // applications
        releaseCamera();
    }
    private boolean hasCamera(Context context) {
        // check if the device has camera
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }
    boolean recording = false;
    View.OnClickListener captrureListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (recording) {
                // stop recording and release camera
                mediaRecorder.stop(); // stop the recording
                releaseMediaRecorder(); // release the MediaRecorder object
                Toast.makeText(MonitorActivity.this, "Video captured!", Toast.LENGTH_LONG).show();
                recording = false;
            } else {
                if (!prepareMediaRecorder()) {
                    Toast.makeText(MonitorActivity.this, "Fail in prepareMediaRecorder()!\n - Ended -", Toast.LENGTH_LONG).show();
                    finish();
                }
                // work on UiThread for better performance
                runOnUiThread(new Runnable() {
                    public void run() {
                        // If there are stories, add them to the table

                        try {
                            mediaRecorder.start();
                        } catch (final Exception ex) {
                            // Log.i("---","Exception in thread");
                        }
                    }
                });

                recording = true;
            }
        }
    };
    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset(); // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
            mCamera.lock(); // lock camera for later use
        }
    }
    private boolean prepareMediaRecorder() {

        mediaRecorder = new MediaRecorder();

        mCamera.unlock();
        mediaRecorder.setCamera(mCamera);

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));

        mediaRecorder.setOutputFile("/sdcard/myvideo.mp4");
        mediaRecorder.setMaxDuration(600000); // Set max duration 60 sec.
        mediaRecorder.setMaxFileSize(50000000); // Set max file size 50M

        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            releaseMediaRecorder();
            return false;
        }
        return true;

    }
    private void releaseCamera() {
        // stop and release camera
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }


    //bluetooth
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mBTService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, "Not Connected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mBTService.write(send);

        }
    }

    //bluetooth connection
    private void connectDevice(Intent data) {
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = myBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mBTService.connect(device);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode){
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if(resultCode== Activity.RESULT_OK){
                    if (mBTService==null) {
                        mBTService = new BluetoothService(this, mHandler, mConnectSide);
                    }
                    connectDevice(data);
                }
                break;
            case REQUEST_ENABLE_BT:

                // When the request for enabling bluetooth returns
                if (resultCode == Activity.RESULT_OK){
                        //connectDevice(getIntent());
                    if (!BluetoothCond) {
                        Intent serverIntent = new Intent(this, DeviceListActivity.class);
                        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                    }
                    if (mBTService==null) {
                        mBTService = new BluetoothService(this, mHandler, mConnectSide);
                    }
                    //Bluetooth is now enabled, so set up bluetoothServie
                    //mBTService = new BluetoothService(this, mHandler,mConnectSide);
                }
                else{
                    //User didn't enable bluetooth or error was occurred
                   // Log.d(TAG, "BT not enabled");
                    Toast.makeText(this,"Bluetooth was not enabled", Toast.LENGTH_SHORT).show();
                    finish();

                }

        }

    }

    private void takeVideo() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }

    public void videoFunc(){
        Intent intent = new Intent(this, AndroidVideoCaptureExample.class);
        startActivity(intent);
    }


    // The Handler that gets information back from the BluetoothService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothService.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            break;
                    }
                    break;
                case BluetoothService.MESSAGE_WRITE:
                    //byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    // String writeMessage = new String(writeBuf);
                    break;
                case BluetoothService.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String msgString = new String(readBuf);
                    if (msgString.startsWith("TAKEPICTURE"))
                    {
                        videoFunc();
                    }
                    Toast.makeText(getApplicationContext(), msgString, Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothService.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(BluetoothService.DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothService.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(BluetoothService.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };


}
