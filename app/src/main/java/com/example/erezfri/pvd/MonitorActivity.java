        package com.example.erezfri.pvd;

        import android.app.Activity;
        import android.app.ProgressDialog;
        import android.bluetooth.BluetoothAdapter;
        import android.bluetooth.BluetoothDevice;
        import android.bluetooth.BluetoothSocket;
        import android.content.Context;
        import android.content.Intent;
        import android.content.pm.ActivityInfo;
        import android.content.pm.PackageManager;
        import android.content.res.Configuration;
        import android.graphics.Bitmap;
        import android.graphics.Canvas;
        import android.hardware.Camera;

        import android.hardware.camera2.CameraCaptureSession;

        import android.media.CamcorderProfile;
        import android.media.MediaMetadataRetriever;
        import android.media.MediaPlayer;
        import android.media.MediaRecorder;
        import android.net.Uri;
        import android.os.Bundle;
        import android.os.Environment;
        import android.os.Handler;
        import android.os.Looper;
        import android.os.Message;
        import android.provider.MediaStore;
        import android.support.v7.app.ActionBarActivity;
        import android.view.TextureView;
        import android.view.View;
        import android.view.WindowManager;
        import android.widget.Button;
        import android.widget.LinearLayout;
        import android.widget.ProgressBar;
        import android.widget.TextView;
        import android.widget.Toast;

        import java.io.BufferedReader;
        import java.io.File;
        import java.io.FileNotFoundException;
        import java.io.FileOutputStream;
        import java.io.FileWriter;
        import java.io.IOException;
        import java.nio.ByteBuffer;
        import java.text.SimpleDateFormat;
        import java.util.ArrayList;
        import java.util.Calendar;
        import java.util.Date;
        import java.util.Timer;
        import java.util.TimerTask;


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
    //end for camera preview

    // the scope graph customized view object
    //GRAPH
    private PlotDynamic mGraph;
    public int mGraphMultiCount=0;
    private ArrayList<byte[]> Packets;
    public int[] mGraphControlInd = new int[]{0}; //graph view index ‐ may determine the front graph
    public int[] mGraphMultiInd= new int[]{-1,0,-1};
    public int[] mSampCountPos; //the position of the counter of the sensor's samples number
    private boolean mSampCountPosFlag;
    private LinearLayout graphPreview;
    public int mGraphMultiMax;
    public int mGraphMultiNum=0,mGraphControlNum=0;

    //for the camera
    private Context myContext;
    private boolean cameraFront = false;
    private CameraPreview mPreview;
    private LinearLayout cameraPreview;
    private Button capture;
    private MediaRecorder mediaRecorder;
    private int mSensorNum = 0;

    private Timer myTimer;

    private MediaStore mediaStore;

    private boolean recordingStatus = false;

    //FILES
    public ArrayList<File> mFileGroup;
    public ArrayList<FileWriter> mFileWriterGroup;
    private String sampleName = "";

    //Loading
    ProgressDialog progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.monitor_screen);
        if(getResources().getBoolean(R.bool.tablet_mode)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        // take an instance of BluetoothAdapter - Bluetooth radio
        if (myBluetoothAdapter==null){
            myBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        }
        if (myBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(getApplicationContext(), "Device does not support Bluetooth",
                    Toast.LENGTH_SHORT).show();
        }
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
        }else {
            BluetoothCond=true;
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        }


    }

//GRAPH

    public void Plot(){
        //Initialize view variables
        if (mGraph == null) {
            mGraph = new PlotDynamic(this, 1, 1);

            mGraph.setTitle("The Sensor Activity");
            String[] mGraphGroupColor = new String[]{"#FF002060", "#FFFF0000", "#FF4A7EBB"};
            String[] mGraphBackColor = new String[]{"#FFBCBCBC"};
            String[] mGraphGridColor = new String[]{"#FFE0E0E0"};
            mGraph.setColor(mGraphGroupColor, PlotDynamic.Colorpart.plots);
            mGraph.setColor(mGraphBackColor, PlotDynamic.Colorpart.backgroud);
            mGraph.setColor(mGraphGridColor, PlotDynamic.Colorpart.grid);

            //bulid view
            graphPreview = (LinearLayout) findViewById(R.id.graph_preview);
            graphPreview.addView(mGraph);
        }
        graphPreview.setVisibility(View.VISIBLE);
        Packets = new ArrayList<byte[]>();
        return;
    }



    //start for camera preview
    public void onResume() {
        super.onResume();
        if (!hasCamera(myContext)) {
            Toast toast = Toast.makeText(myContext, "Sorry, your phone does not have a camera!", Toast.LENGTH_LONG);
            toast.show();
            finish();
        }
        if (mCamera == null) {
            mCamera = Camera.open(findBackFacingCamera());
            if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                mCamera.setDisplayOrientation(90);
                // mCamera.setDisplayOrientation(0);
            } else {
                mCamera.setDisplayOrientation(0);
            }
            //  mCamera.setDisplayOrientation(90);
            mPreview.refreshCamera(mCamera);
        }
    }
    public void initialize() {
        cameraPreview = (LinearLayout) findViewById(R.id.camera_preview);

        mPreview = new CameraPreview(myContext, mCamera);
        cameraPreview.addView(mPreview);

        capture = (Button) findViewById(R.id.button_capture);
        capture.setOnClickListener(captrureListener);
    }


    public void handleStartStop() {
        if (recording) {
            // stop recording and release camera
            mediaRecorder.stop(); // stop the recording
            releaseMediaRecorder(); // release the MediaRecorder object
            //Toast.makeText(MonitorActivity.this, "Video captured!", Toast.LENGTH_LONG).show();
            recording = false;
        } else {
            try {
                if (!prepareMediaRecorder()) {
                    Toast.makeText(MonitorActivity.this, "Fail in prepareMediaRecorder()!\n - Ended -", Toast.LENGTH_LONG).show();
                    finish();
                }
            } catch (IOException e) {
                e.printStackTrace();
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

    public void movieToFrames() {

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/WPD/";
        String filePath = path + sampleName + ".mp4" ;
        MediaPlayer mp = MediaPlayer.create(this, Uri.parse(filePath));
        int videoLengthInMillis = mp.getDuration();
        retriever.setDataSource(filePath);
        Calendar c = Calendar.getInstance();
        String storageDir = Environment.getExternalStorageDirectory().getAbsolutePath()+ "/WPD/Pictures" + c.getTime().toString();
        File imagesDir = new File(storageDir);
        if(!imagesDir.exists()) {
            imagesDir.mkdirs();
        }

        for(int i=33333;i<videoLengthInMillis*1000;i+=33333){ //assuming 30 frames per second.

            Bitmap bitmap = retriever.getFrameAtTime(i,MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            String timeStamp = new SimpleDateFormat("yyyyMMdd").format(new Date());
            timeStamp = timeStamp + "_" + i/33333 + "_frame";
            String FILENAME = storageDir + "/" + timeStamp + ".jpg";
            FileOutputStream fos = null;

            try {
                fos = new FileOutputStream(FILENAME);
            } catch (FileNotFoundException e1) {}
            try {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, fos);
                fos.flush();
                fos.close();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        retriever.release();
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
            handleStartStop();
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
    private boolean prepareMediaRecorder() throws IOException {

        mediaRecorder = new MediaRecorder();

        mCamera.unlock();
        mediaRecorder.setCamera(mCamera);

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        //  mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW));
        // mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        // mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/WPD/";
        File dir = new File(path);
        if(!dir.exists())
            dir.mkdirs();
        Calendar c = Calendar.getInstance();
        sampleName =  "sample_" + c.getTime().toString();
        String myFile = path + sampleName + ".mp4" ;
        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));
        //mediaRecorder.setVideoFrameRate();
        mediaRecorder.setOutputFile(myFile);
        //    mediaRecorder.setOutputFile(Environment.getExternalStorageDirectory().getPath());

        mediaRecorder.setMaxDuration(3000000); // Set max duration 60 sec * 5 = 5 minutes.//TODO think about it
        mediaRecorder.setMaxFileSize(800000000); // Set max file size 800M//TODO think about it
        if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            mediaRecorder.setOrientationHint(90);
            // mCamera.setDisplayOrientation(0);
        } else {
            mediaRecorder.setOrientationHint(0);
        }
        //mediaRecorder.setOrientationHint(90);
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
    //end for camera preview


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
                    if (msgString.startsWith("START"))
                    {
                        recordingStatus = true;
                        TextView t = (TextView)findViewById(R.id.recordingStatus);
                        handleStartStop();
                        t.setVisibility(View.VISIBLE);//RECORDING...
                        Toast.makeText(getApplicationContext(),"The video started", Toast.LENGTH_SHORT).show();
                        int indexStart = msgString.indexOf("=") + 1;
                        int indexEnd = msgString.indexOf("@");
                        mSensorNum = Integer.parseInt(msgString.substring(indexStart, indexEnd));
                        mSampCountPos=new int[mSensorNum];
                        mSampCountPosFlag = false;
                        for (int i=0;i<mSensorNum;i++){if (mGraphControlInd[i]!=-1) mGraphControlNum++;}
                        Plot();

                    }
                    else if (msgString.startsWith("STOP")){
                        recordingStatus = false;
                        mGraphControlNum = 0;
                        TextView t = (TextView)findViewById(R.id.recordingStatus);
                        handleStartStop();
                        t.setVisibility(View.INVISIBLE);
                        Toast.makeText(getApplicationContext(),"The video stopped, please wait", Toast.LENGTH_SHORT).show();
                        graphPreview = (LinearLayout) findViewById(R.id.graph_preview);
                        graphPreview.setVisibility(View.INVISIBLE);


                        //file:
                        CreateFile(sampleName + ".csv");
                        Packets2File(Packets);
                        System.gc();

                        //make frames
                        new Thread(new Runnable() {
                            @Override
                            public void run()
                            {
                                movieToFrames();
                                System.gc();

                            }
                        }).start();
                        Toast.makeText(getApplicationContext(),"Done", Toast.LENGTH_SHORT).show();
                    }
                    else if (msgString.startsWith("SampCountPos") && recordingStatus) {

                        int indexStartI = msgString.indexOf("[") + 1;
                        int indexEndI = msgString.indexOf("]");
                        int indexStartVal = msgString.indexOf("=") + 1;
                        int indexEndVal = msgString.indexOf("@");
                        mSampCountPos[Integer.parseInt(msgString.substring(indexStartI, indexEndI))] =
                                Integer.parseInt(msgString.substring(indexStartVal, indexEndVal));
                        mSampCountPosFlag = true;
                    }
                    else if (recordingStatus && mSampCountPosFlag){   //packets
                        Packets.add(readBuf.clone());
                        GraphAddData(readBuf, mGraph);
                        mGraph.invalidate();
                    }

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

    //=====
    //FILES
    //=====
    /**
     * Creates samples files - each sensor has its own file
     * (used by the controller and may be used by the multi-sensor)
     */
    public void CreateFile(String filename){
        //if(D_SAMPLE2FILE){
        String state = Environment.getExternalStorageState();
        if (!(state.equals(Environment.MEDIA_MOUNTED))) {
            Toast.makeText(this ,"Media is not mounted" ,Toast.LENGTH_SHORT).show();
            finish();
        }
        mFileGroup =new ArrayList<File>();
        mFileWriterGroup= new ArrayList<FileWriter>();

        //create files and it's writers
        //File path2 = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/WPD";
        try{
            File file = new File(path, filename);
            mFileGroup.add(file);
            if (file.exists()){
                file.delete();
            }
            file.createNewFile();
            mFileGroup.add(file);
            mFileWriterGroup.add(new FileWriter(file));
            //put file tables titles
            for (int i=0;i<mSensorNum;i++){
                FileWriter filewriter = mFileWriterGroup.get(i);

                filewriter.append("time[sec]");
                filewriter.append(',');
                filewriter.append("value,");
                filewriter.append('\n');
            }
        }
        catch(IOException e)
        {
            //Toast.makeText(activity, e.getMessage() ,Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * Writes the pull of packets to the files.
     * (used by the controller and may be used by the multi-sensor)
     */
    public void Packets2File(ArrayList<byte[]> Packets){
        float startTime = Float.MAX_VALUE;
        for (byte[] p: Packets){
            ByteBuffer Packet = ByteBuffer.wrap(p);//for each packet
            //  at the packet - for each sensor i

            for(int i=0;i<mSensorNum;i++){
                //get appropriate filewriter
                FileWriter filewriter = mFileWriterGroup.get(i);
                //set position to the start of the sensor i message
                Packet.position(mSampCountPos[i]);

                //write to files
                long x=Packet.getInt();
                long samplesNum=100;
                try{
                    for (long n=0;n<samplesNum;n++){
                        float timeFloat = Packet.getFloat();
                        if (timeFloat < startTime) startTime = timeFloat;
                        timeFloat = timeFloat-startTime;
                        String timeStr = Float.toString(timeFloat);
                        filewriter.append(timeStr);
                        filewriter.append(',');
                        String value = Float.toString(Packet.getFloat());
                        filewriter.append(value);
                        filewriter.append('\n');
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        //close files
        for(int i=0;i<mSensorNum;i++){
            FileWriter filewriter = mFileWriterGroup.get(i);
            try{
                filewriter.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Add sensors' packets' samples to the controller graph object.
     * @param PacketBuf - packet
     * @param Graph - plotDynamic graph object
     */
    public void GraphAddData(byte[] PacketBuf,PlotDynamic Graph){
        ByteBuffer Packet = ByteBuffer.wrap(PacketBuf);
        // for each sensor i at the packet
        // check if user asked to plot sensor's i data (if not continue to next)
        for(int i=0;i<mSensorNum;i++){
            //set position to the start of the sensor i message
            Packet.position(mSampCountPos[i]);
            //user ask to plot
            if(mGraphControlInd[i]!=-1){
                int samplesNum=Packet.getInt();
                for (int j=0;j<samplesNum;j++){
                    float xVal = Packet.getFloat();
                    float yVal = Packet.getFloat();
                    Graph.addData(xVal,yVal, mGraphControlInd[i]);
                }
            }
        }
    }


}