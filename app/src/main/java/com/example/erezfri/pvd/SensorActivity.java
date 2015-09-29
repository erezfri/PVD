package com.example.erezfri.pvd;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SensorActivity extends ActionBarActivity implements SensorEventListener {

    //timer
    private Handler mHandler = new Handler();
    private long startTime;
    private long elapsedTime;
    private final int REFRESH_RATE = 100;
    private String hours,minutes,seconds,milliseconds;
    private long secs,mins,hrs,msecs;
    private boolean stopped = true;
    private Runnable startTimer;

    public static final int CONTROLLER=0;
    public static final int MULTI_SENSOR=1;

    //sensor variables
    public static final boolean WITHTIME = true;
    public static final boolean WITHOUTTIME = false;

    public static final int axesX =0;
    public static final int axesY =1;
    public static final int axesZ =2;

    public static final boolean MODIFY = true;
    public static final boolean TRANSPERENT = false;

    //graph variables
    public static final boolean TOPLOT = true;
    public static final boolean NOTTOPLOT = false;
    public static final boolean GRIDON=true;
    public static final boolean GRIDOFF=false;


    //bluetooth
    private BluetoothService mBTService = null;
    // Connection mechanism (side)
    private int mConnectSide = BluetoothService.SERVER;
    // Name of the connected device
    private String mConnectedDeviceName = null;

    private BluetoothAdapter mBluetoothAdapter = null;
    private static final int REQUEST_ENABLE_BT = 1;

    //timer
    public SensorActivity() {
        startTimer = new Runnable() {
            public void run() {
                elapsedTime = System.currentTimeMillis() - startTime;
                updateTimer(elapsedTime);
                mHandler.postDelayed(this,REFRESH_RATE);
            }
        };

        //Sensor Experiment info variables
        //mSenorTypeGroup=new int[]{Sensor.TYPE_GRAVITY,Sensor.TYPE_GYROSCOPE,Sensor.TYPE_LINEAR_ACCELERATION};
        mSenorTypeGroup=new int[]{Sensor.TYPE_GYROSCOPE};
        mDefaultSensor=false;
        mAxes = new int[]{axesX,axesY,axesZ};
        mNamesGroup= new String[]{"4*Angle[rad]","Gyroscope(x)[rad/s]","Linear Accelerometer(z)[m/s^2]"};
        mModify = new boolean[]{MODIFY,TRANSPERENT,TRANSPERENT};
        mTime=WITHTIME;
        sensorDelay=SensorManager.SENSOR_DELAY_FASTEST;

        //Packet variables
        mTotSampNum=100;

        //======================
//SYS MANAGER VARIABLES
//======================
//Sensor Experiment info variables
        mSensorNum=mSenorTypeGroup.length;
//Packet variables
        mSampByteNum=mTime ? 4+4:4; //if time change to 4 change GraphAddData val

        mSensorMaxSamp = new int[mSensorNum]; for(int i=0;i<mSensorNum;i++){mSensorMaxSamp[i]
                =mTotSampNum/mSensorNum;}
//mSensorMaxSamp = new int[]{25,50,25};
        SetPosition();
        mGraphMultiMax=(int)(0.5*mGraphMultiNum*mTotSampNum/mSensorNum);
    }



    //Sensor Experiment info variables
    public int[] mSenorTypeGroup;
    public boolean mTime;
    public int[] mAxes;
    public boolean[] mModify;
    public int mSensorNum;//=mSenorTypeGroup.length;
    private boolean mDefaultSensor;

    //sensor
    private ArrayList<Sensor> mSensorGroup = new ArrayList<Sensor>(mSensorNum);
    private boolean mAcquisitionFlag = false;
    private SensorManager mSensorManager;
    public static int sensorDelay=SensorManager.SENSOR_DELAY_GAME;//default value

    //Packet variables
    public int mTotSampNum;
    public int[] mSensorMaxSamp; // if sensors has different freq
    public int mSampByteNum;
    public ByteBuffer mPacket;   // for each sensor sampnum(int)|time(long)| value(int)
    public int[] mSampCount;
    public int[] mSampCountPos; //the position of the counter of the sensor's samples number
    public int[] mPosition;     // the index(dynamic) of the next sample related to a specific sensor
    public int mCurSensor;      //used for fast ploting at multi-sensor
    public float mCurVal,mCurTime; //  for fast ploting at multi-sensor
    private boolean initPos=true; // for once initilization of mSampCountPos
    public byte[] message;
    public long startTime2;
    //public boolean waitToStart;
    public enum ControlMessage {start};
    public int startMessage=-1;

    public static boolean D_MULTI_SENSOR_SCOPE_VIEW;
    public static boolean D_MULTI_SENSOR_FILE;
    public static boolean D_CONTROLLER_FILE;

    //GRAPH
    //public String[] mAxesName;
    public String[] mNamesGroup;
    public int mGraphMultiNum=0;

    public int mGraphMultiMax;

    //VIEW,FILES
    private PlotDynamic mGraph;
    private Activity mActivity=this;
    private ArrayList<byte[]> Packets;

    //FILES
    public String[] mFileNameMulti,mFileNameControl;
    public ArrayList<File> mFileGroup;
    public ArrayList<FileWriter> mFileWriterGroup;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sensor_screen);
        Button stopButton = (Button)findViewById(R.id.stopButton);
        stopButton.setVisibility(View.GONE);

        //bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth is not available", Toast.LENGTH_LONG).show();
        }

        //Get SensorManager and sensors
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mSensorGroup = getSensors(mSensorManager);
    }


    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);

        }

        if (mBTService==null) {
            mBTService = new BluetoothService(this, bluetoothHandler, mConnectSide);
        }
        connectDevice();

        //Toast.makeText(getApplicationContext(), "Waiting for monitor connection", Toast.LENGTH_LONG).show();
        }
    @Override
    protected void onResume() {
        super.onResume();
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
//        if (mBTService != null) {
//            // Only if the state is STATE_NONE, do we know that we haven't started already
//            if (mBTService.getState() == BluetoothService.STATE_NONE) {
//                // Start the Bluetooth chat services
//                mBTService.set();
//            }
//        }
        if (mAcquisitionFlag) {
            registerSensorListener();
        }

    }

        public void startClick(View view){
            showStopButton();
            if(stopped){
                startTime = System.currentTimeMillis() - elapsedTime;
            }
            else{ startTime = System.currentTimeMillis();
            }
            mHandler.removeCallbacks(startTimer);
            mHandler.postDelayed(startTimer, 0);
            try{
                sendMessage("START_SENSORNUM=" + mSensorNum + "@");
                mSensorManager.unregisterListener((SensorEventListener)mActivity);
                Packets = new ArrayList<byte[]>();
                SetStart();
                mAcquisitionFlag = true;
                registerSensorListener();
                mBTService.write(getControlMessage(ControlMessage.start));
                //sendMessagea(mExperimentManager.startMessage);

            }
            catch (Exception e){}

        }

        public void stopClick(View view){
            try{
                sendMessage("STOP");
                mSensorManager.unregisterListener((SensorEventListener)mActivity);
                if(D_MULTI_SENSOR_FILE){
                    Toast.makeText(mActivity,"Saving files and Leaving pendulum experiment" ,Toast.LENGTH_LONG).show();
                    CreateFile(mFileNameMulti, mActivity);
                    Packets2File(Packets);
                }
                //finish();

            }catch (Exception e){}
            hideStopButton();
            mHandler.removeCallbacks(startTimer);
            ((TextView)findViewById(R.id.counterText)).setText("00:00:00");
            stopped = false;


        }


        private void showStopButton(){
            (findViewById(R.id.startButton)).setVisibility(View.INVISIBLE);
            (findViewById(R.id.stopButton)).setVisibility(View.VISIBLE);
        }
        private void hideStopButton(){
            (findViewById(R.id.startButton)).setVisibility(View.VISIBLE);
            (findViewById(R.id.stopButton)).setVisibility(View.INVISIBLE);
        }

        private void updateTimer (float time){
            secs = (long)(time/1000);
            mins = (long)((time/1000)/60);
            hrs = (long)(((time/1000)/60)/60);
            /* Convert the seconds to String * and format to ensure it has * a leading zero when required */
            secs = secs % 60;
            seconds=String.valueOf(secs);
            if(secs == 0) {
                seconds = "00";
            }
            if(secs <10 && secs > 0){
                seconds = "0"+seconds;
            }
            /* Convert the minutes to String and format the String */
            mins = mins % 60;
            minutes=String.valueOf(mins);
            if(mins == 0){
                minutes = "00";
            }
            if(mins <10 && mins > 0){
                minutes = "0"+minutes;
            }
            /* Convert the hours to String and format the String */
            hours=String.valueOf(hrs);
            if(hrs == 0){
                hours = "00";
            }
            if(hrs <10 && hrs > 0){
                hours = "0"+hours;
            }
            /* Although we are not using milliseconds on the timer in this example * I included the code in the event that you wanted to include it on your own */
            milliseconds = String.valueOf((long)time);
            if(milliseconds.length()==2){
                milliseconds = "0"+milliseconds;
            }
            if(milliseconds.length()<=1){
                milliseconds = "00";
            }
           // milliseconds = milliseconds.substring(milliseconds.length()-3, milliseconds.length()-2);
             /* Setting the timer text to the elapsed time */
            ((TextView)findViewById(R.id.counterText)).setText(hours + ":" + minutes + ":" + seconds);
          //  ((TextView)findViewById(R.id.timerMs)).setText("." + milliseconds);
        }

        public void aboutClick(View view){
            new AlertDialog.Builder(this)
                    .setTitle("About Walking Pattern Detector")
                    .setMessage("This application is research designed to identify walking patterns .\n" +
                            "The app was developed by Technion STEM lab.")
                    .setPositiveButton(android.R.string.ok, null).create().show();
        }

    public void helpClick(View view) {
        new AlertDialog.Builder(this)
                .setTitle("Help")
                .setMessage("You are now in the sensor screen, please make sure you are connected to a monitor device")
                    .setPositiveButton(android.R.string.ok, null).create().show();

        }

    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mBTService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, "Not Connected", Toast.LENGTH_SHORT).show();
            connectDevice();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mBTService.write(send);

        }
    }

    // The Handler that gets information back from the BluetoothService
    private final Handler bluetoothHandler = new Handler() {
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
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    //TextView view = (TextView) findViewById(R.id.multi_sensor_text_view);
                    //view.setText(readMessage);
                        Toast.makeText(getApplicationContext(), readMessage, Toast.LENGTH_SHORT).show();
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


    //bluetooth connection
    private void connectDevice() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                try {   //try to connect to one of the paired devices... problem if more than 1, should be able to choose from list
                    mBTService.connect(device);
                    if (mBTService.getState() == 3) //STATE_CONNECTED
                    {
                        break;
                    }
                }
                catch (Exception e){}
            }
        }
    }



    /**
     * getSensors gets the multi-sensor specific sensor.
     * the sensor type is default or Google.
     */
    public ArrayList<Sensor> getSensors(SensorManager sensorManager){
        ArrayList<Sensor> sensorGroup = new ArrayList<Sensor>(mSensorNum);

        //take the default sensors - HW
        if (mDefaultSensor){
            //Get SensorManager and sensors
            for (int i=0;i<mSensorNum;i++){
                sensorGroup.add(sensorManager.getDefaultSensor(mSenorTypeGroup[i]));
            }
        }
        else{
            //take Android open source sensors
            for (int i=0;i<mSensorNum;i++){
                List<Sensor> sensorList = sensorManager.getSensorList(mSenorTypeGroup[i]);
                for (Sensor sensor:sensorList){
                    if(sensor.getVendor().contains("Google Inc.")){
                        sensorGroup.add(sensor);
                        break;
                    }
                }
            }
        }
        if (sensorGroup.size()==0)
        {
            for (int i=0;i<mSensorNum;i++){
                sensorGroup.add(sensorManager.getDefaultSensor(mSenorTypeGroup[i]));
            }
        }

        return sensorGroup;
    }


    private void registerSensorListener(){
        // register to SensorEventListener
        for (int i=0;i<mSensorNum;i++){
            mSensorManager.registerListener(this, mSensorGroup.get(i), sensorDelay);
        }

    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        // Check that we're actually connected before trying anything
        if (mBTService.getState() != BluetoothService.STATE_CONNECTED) {
            return;
        }

        // Create the message bytes and send via BluetoothChatService
        // Send message and plot at the SAME PHASE
        boolean tosendFlag = PacketAdd(event);
        if(tosendFlag) {
            SetPosition();
            for (int i = 0; i<mSensorNum; i++) {
                String SampCountPosMessage = "SampCountPos[" + i + "]=" + mSampCountPos[i] + "@";
                //mBTService.write(SampCountPosMessage.getBytes());
                sendMessage(SampCountPosMessage);
            }
            mBTService.write(message);
            Packets.add(message);

        }

       //GRAPH
        /*
        if ((D_MULTI_SENSOR_SCOPE_VIEW)){
            boolean toplotDifferFlag = GraphAddData(mGraph);
            //same phase
            if(mGraphMultiPhase && tosendFlag) mGraph.invalidate();
                //different phase
            else if (toplotDifferFlag)
                mGraph.invalidate();
        }
        */
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    //==============
    //PACKET SERVICE
    //==============
    /**
     * Put a specific sensor's sample at the packet.
     * used by the multi-sensor.
     * */
    public boolean PacketAdd(SensorEvent event){
        int i;
        boolean flag = false;
// get sensor index
        for(i=0;i<mSensorNum;i++){
            if(event.sensor.getType()==mSenorTypeGroup[i]) break;
        }
        mCurSensor=i;
// get value to put at the Packet buffer
        float axisX = event.values[0];
        float axisY = event.values[1];
        float axisZ = event.values[2];
        float omegaMagnitude = (float)Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

        mCurVal=omegaMagnitude * 10;
        /*if(mModify[i]){
            mCurVal=ModifySensorVal(mSenorTypeGroup[i],mCurVal);
        }*/
// put sample at the Packet buffer and update buffer.
        mPacket.position(mPosition[i]);
        if(mTime) {
//mPacket.putLong(event.timestamp);
            if(startTime==Long.MIN_VALUE){
                startTime=event.timestamp;
            }
            mCurTime = (float)(1e-9*(event.timestamp-startTime));
            mPacket.putFloat(mCurTime);

        }
        mPacket.putFloat(mCurVal);
        mPosition[i]=mPosition[i]+mSampByteNum;
        mSampCount[i]++;

// if the sub‐buffer is full then sending packet
        if (mSampCount[i]==mSensorMaxSamp[i]){
            for(int j=0;j<mSensorNum;j++){
                mPacket.position(mSampCountPos[j]);
                mPacket.putInt(mSampCount[j]);
            }
            message=mPacket.array();
//allocate new buffer ‐ otherwise data will be override and won't be available for files
            if (D_MULTI_SENSOR_FILE)
                mPacket=ByteBuffer.allocate(4*mSensorNum+mTotSampNum*mSampByteNum);
            //SetPosition();
            flag = true;
            return true;
        }
        if (flag){
            return true;
        }
        else {
            return false;
        }
    }

    private float ModifySensorVal(int SensorType,float val){
        float modval=val;
        if(SensorType==Sensor.TYPE_GRAVITY){
            modval = (float) (4*Math.asin(val/9.8));
        }
        return modval;
    }


    /**
     * Set packet position variables:
     * mPosition is the index(dynamic) of the next sample related to a specific sensor
     * mSampCountPos is the position of the counter of the sensor's samples number
     * */
    private void SetPosition(){
        mPosition=new int[mSensorNum];
        mSampCountPos=new int[mSensorNum];
        mSampCount= new int[mSensorNum];
        for(int i=0;i<mSensorNum;i++){
            mPosition[i]=4*(i+1)+i*mSensorMaxSamp[i]*mSampByteNum; //first 4, second 4+(SensorTotSampNum*SampByteNum)+4
        }
        if (initPos){
            for(int i=0;i<mSensorNum;i++){
                mSampCountPos[i]=i*(4+mSensorMaxSamp[i]*mSampByteNum); //first 0, second (4+(SensorTotSampNum*SampByteNum))
            }
        }
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessagea(String message) { //
        // Check that we're actually connected before trying anything
        if (mBTService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this,"Not connected to the bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mBTService.write(send);

        }
    }
    /**
     * Set the start time of the experiment:
     * set the first event time to be zero and set new packet.
     */
    public void SetStart(){
        //set time
        startTime2=Long.MIN_VALUE;
        // initilize packet
        mPacket=ByteBuffer.allocate(4*mSensorNum+mTotSampNum*mSampByteNum);
        SetPosition();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case REQUEST_ENABLE_BT:
                // When the request for enabling bluetooth returns
                if (resultCode == Activity.RESULT_OK){
                    //Bluetooth is now enabled, so set up bluetoothServie
                    mBTService = new BluetoothService(this, mHandler,mConnectSide);
                }
                else{
                    //User didn't enable bluetooth or error was occurred
                    Toast.makeText(this,"Bluetooth was not enabled", Toast.LENGTH_SHORT).show();
                    finish();

                }
        }
    }





    //=================
    // CONTROL MESSAGES
    //=================
    /**
     * 	The 'control messages' used to transfer control messages.
     *  i.e:
     *  'start' is used when the controller commits start (after the experiment was begun before)
     *          it's used to distinguish between the packets arrived before the multi-senor get the start command and after it got it.
     *          to do that the multi-sensor transmits a start message before it send the new session packets.
     *
     *  *getControlMessage - used for the creation of the control message
     *  *CheckMessageType - used to check if the current message is from the type 'messagetype'
     */
    public byte[] getControlMessage(ControlMessage messagetype){
        byte[] message = new byte[4];

        switch (messagetype){
            case start:
                message=ByteBuffer.allocate(4).putInt(startMessage).array();
        }
        return message;

    }

    public boolean CheckMessageType(byte[] message,ControlMessage messagetype){
        boolean Check = false;

        switch (messagetype){
            case start:
                if(ByteBuffer.wrap(message).getInt()==startMessage)
                    Check=true;
                else
                    Check=false;
        }
        return Check;

    }



    //=====
    //FILES
    //=====
    /**
     * Creates samples files - each sensor has its own file
     * (used by the controller and may be used by the multi-sensor)
     */
    public void CreateFile(String[] filenames,Activity activity){
        //if(D_SAMPLE2FILE){
        String state = Environment.getExternalStorageState();
        if (!(state.equals(Environment.MEDIA_MOUNTED))) {
            Toast.makeText(activity ,"Media is not mounted" ,Toast.LENGTH_SHORT).show();
            activity.finish();
        }
        mFileGroup =new ArrayList<File>();
        mFileWriterGroup= new ArrayList<FileWriter>();

        //create files and it's writers
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        path.mkdirs();
        try{
            for(int i=0;i<mSensorNum;i++){
                File file = new File(path, filenames[i]);
                mFileGroup.add(file);
                if (file.exists()){
                    file.delete();
                }
                file.createNewFile();
                mFileGroup.add(file);
                mFileWriterGroup.add(new FileWriter(file));
            }
            //put file tables titles
            for (int i=0;i<mSensorNum;i++){
                FileWriter filewriter = mFileWriterGroup.get(i);
                if(mTime){
                    filewriter.append("time[sec]");
                    filewriter.append(',');
                }
                filewriter.append("TEST~~~~~~");//mNamesGroup[i]);
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
        for (byte[] p: Packets){
            ByteBuffer Packet = ByteBuffer.wrap(p);//for each packet
            //  at the packet - for each sensor i

            for(int i=0;i<mSensorNum;i++){
                //get appropriate filewriter
                FileWriter filewriter = mFileWriterGroup.get(i);
                //set position to the start of the sensor i message
                Packet.position(mSampCountPos[i]);

                //write to files
                int samplesNum=Packet.getInt();
                try{
                    for (int n=0;n<samplesNum;n++){
                        if(mTime)  {
                            String time = Float.toString(Packet.getFloat());
                            filewriter.append(time);
                            filewriter.append(',');
                        }
                        filewriter.append(Float.toString(Packet.getFloat()));
                        filewriter.append('\n');
                    }
                }
                catch (IOException e) {
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

}