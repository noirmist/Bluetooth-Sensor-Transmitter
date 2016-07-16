package me.jungwoo.noirmist.publisher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity implements View.OnClickListener, SensorEventListener {

    /*
     * Set up for Sensor
     */
    private SensorManager mSensorManager;
    private Sensor mGyroscope;
    private Sensor accSensor;

    int accelXValue;
    int accelYValue;
    int accelZValue;

    int gyroX;
    int gyroY;
    int gyroZ;

    // Debugging
    private static final String TAG = "Main";

    // Intent request code
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Layout
    private Button btn_Connect;
    private Button btn_Run;
    private Button btn_Stop;

    private BluetoothService btService = null;

    //private Handler mHandler = null;
    private SendingThread mSendThread = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate");

        setContentView(R.layout.activity_main);
        // Get SensorManager
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        // Gyroscope sensor
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        // Accelerometer Sensor
        accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        /** Main Layout **/
        btn_Connect = (Button) findViewById(R.id.btn_connect);
        btn_Run = (Button) findViewById(R.id.btn_run);
        btn_Stop = (Button) findViewById(R.id.btn_stop);

        // Set Click Listener
        btn_Connect.setOnClickListener(this);
        btn_Run.setOnClickListener(this);
        btn_Stop.setOnClickListener(this);

        // Set BluetoothService class
        if(btService == null) {
            btService = new BluetoothService(this); //,mHandler);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_connect:
                if(btService.getDeviceState()) {
                    // When device support Bluetooth
                    btService.enableBluetooth();
                } else {
                    finish();
                }
                break;

            case R.id.btn_run:
                mSendThread = new SendingThread();
                mSendThread.start();
                break;

            case R.id.btn_stop:
                mSendThread.stopThread();
                break;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult " + resultCode);

        switch (requestCode) {

            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    btService.scanDevice();
                } else {

                    Log.d(TAG, "Bluetooth is not enabled");
                }
                break;

            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    btService.getDeviceInfo(data);
                }
                break;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor localsensor = event.sensor;
        switch (localsensor.getType()){
            case Sensor.TYPE_LINEAR_ACCELERATION:
                accelXValue = (int) event.values[0];
                accelYValue = (int) event.values[1];
                accelZValue = (int) event.values[2];
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyroX = Math.round(event.values[0] * 1000);
                gyroY = Math.round(event.values[1] * 1000);
                gyroZ = Math.round(event.values[2] * 1000);
                break;
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
    // Register Listener
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mGyroscope,SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, accSensor,SensorManager.SENSOR_DELAY_GAME);
    }

    // Unregister Listener
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    // Set sending thread for sending data to another device
    class SendingThread extends Thread implements Runnable {
        private boolean isPlay = false;

        public SendingThread() {
            isPlay = true;
        }

        public void isThreadState(boolean isPlay) {
            this.isPlay = isPlay;
        }

        public void stopThread() {
            isPlay = !isPlay;
        }

        public void run(){
            super.run();
            while(isPlay){
                //Merge sensor's data
                String data = String.valueOf(gyroX)+":"
                        +String.valueOf(gyroY)+":"
                        +String.valueOf(gyroZ)+":"
                        +String.valueOf(accelXValue)+":"
                        +String.valueOf(accelYValue)+":"
                        +String.valueOf(accelZValue);
                byte[] send = data.getBytes();
                //System.out.println(send.toString());
                btService.write(send);

                try { Thread.sleep(250); }
                catch (InterruptedException e) { e.printStackTrace(); }
            }
        }
    }

}
