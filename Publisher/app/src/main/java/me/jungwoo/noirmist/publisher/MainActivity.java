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

    // Debugging
    private static final String TAG = "Main";

    // Intent request code
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Class for Data sending
    private BluetoothService btService = null;
    private SendingThread sendThread = null;

    // Set up for Sensor
    private SensorManager sensorManager;
    private Sensor gyroSensor;
    private Sensor accSensor;

    int accelX;
    int accelY;
    int accelZ;

    int gyroX;
    int gyroY;
    int gyroZ;

    // Layout
    private Button btnConnect;
    private Button btnRunStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set SensorManager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        // Main Layout
        btnConnect = (Button) findViewById(R.id.btn_connect);
        btnRunStop = (Button) findViewById(R.id.btn_run_stop);

        // Set Click Listener
        btnConnect.setOnClickListener(this);
        btnRunStop.setOnClickListener(this);

        // Set BluetoothService

        if(btService == null) {
            btService = new BluetoothService(this);
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
            case R.id.btn_run_stop:
                if(sendThread == null){
                    sendThread = new SendingThread();
                    sendThread.start();
                } else {
                    sendThread.stopThread();
                    sendThread = null;
                }
                break;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
        Sensor sensor = event.sensor;
        switch (sensor.getType()){
            case Sensor.TYPE_LINEAR_ACCELERATION:
                accelX = (int) event.values[0];
                accelY = (int) event.values[1];
                accelZ = (int) event.values[2];
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyroX = Math.round(event.values[0] * 1000);
                gyroY = Math.round(event.values[1] * 1000);
                gyroZ = Math.round(event.values[2] * 1000);
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}
    // Register Listener
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, gyroSensor,SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, accSensor,SensorManager.SENSOR_DELAY_GAME);
    }

    // Unregister Listener
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    // Set sending thread for sending data to another device
    class SendingThread extends Thread implements Runnable {
        private boolean isPlay = false;

        public SendingThread() {
            isPlay = true;
        }
        public void stopThread() {
            isPlay = false;
        }

        public void run(){
            super.run();
            while(isPlay){
                //Merge sensor's data
                String data = String.valueOf(gyroX)+":"
                        +String.valueOf(gyroY)+":"
                        +String.valueOf(gyroZ)+":"
                        +String.valueOf(accelX)+":"
                        +String.valueOf(accelY)+":"
                        +String.valueOf(accelZ);
                byte[] send = data.getBytes();
                //System.out.println(send.toString());
                btService.write(send);

                try { Thread.sleep(250); }
                catch (InterruptedException e) { e.printStackTrace(); }
            }
        }
    }

}
