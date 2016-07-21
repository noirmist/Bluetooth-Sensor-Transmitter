# Bluetooth-Sensor-Transmitter
This application send Gyroscope sensor and Accelerometer's value to the other phone through bluetooth.

# Before you start
You need two Android smartphones which have built in bluetooth.

# Environment

Target system : Android Kitkat
Build system  : Android Studio

# How to build
1. git clone https://github.com/noirmist/Bluetooth-Sensor-Transmitter.git
2. Import each project at Android Studio.
3. Build Pulisher and install one phone.
4. Build Receiver and install the other phone.

# How to Use each application
## Receiver
1. Run application and wait for connecting from the other phone

## Publisher
0. Before Run Publisher Please run Receiver at the other phone.
1. Press connect button
2. Choose device you want to connect
3. Press Run
4. You can check sensor value on Receiver application.
5. Press Stop to stop sending.

# Tips
If it is not working well, please restart application.

## Reference
Google sample (Android-Bluetooth-chat)
https://github.com/googlesamples/android-BluetoothChat

