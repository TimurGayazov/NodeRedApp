package com.example.virtual_door;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.os.Environment;
import java.io.File;
import java.io.FileInputStream;
import java.nio.channels.FileChannel;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MQTT";
    private MqttClient mqttClient;
    private TextView lockStatusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lockStatusTextView = findViewById(R.id.lockStatusTextView);
        Button unlockButton = findViewById(R.id.unlockButton);
        unlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unlockDoor();
            }
        });

        Button lockButton = findViewById(R.id.lockButton);
        lockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lockDoor();
            }
        });

        Button automodeButton = findViewById(R.id.automodeButton);
        automodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                automode();
            }
        });

        Button downloadButton = findViewById(R.id.downloadButton);
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadLogFile();
            }
        });

        connectToMQTTBroker();
    }

    private void connectToMQTTBroker() {
        try {
            Log.d(TAG, "Connecting to MQTT broker");
            mqttClient = new MqttClient("tcp://192.168.1.129:1883", MqttClient.generateClientId(), null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            mqttClient.connect(options);
            mqttClient.subscribe("door_status");
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e(TAG, "Connection to MQTT broker lost: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    Log.d(TAG, "Message arrived on topic " + topic + ": " + new String(message.getPayload()));
                    String status = new String(message.getPayload());
                    if (topic.equals("door_status")) {
                        updateLockStatus(status);
                    }

                }
                private void updateLockStatus(String status) {
                    runOnUiThread(() -> {
                        lockStatusTextView.setText("Lock Status: " + status);
                        writeStatusToFile(status);
                    });
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d(TAG, "Message delivery complete");
                }

            });
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error connecting to MQTT broker: " + e.getMessage());
        }
    }

    private void writeStatusToFile(String status) {
        try {
            String fileName = "doorlogs.txt";
            FileOutputStream fileOutputStream = openFileOutput(fileName, MODE_APPEND);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            outputStreamWriter.write(timeStamp + ": " + status + "\n");
            outputStreamWriter.close();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error writing status to file: " + e.getMessage());
        }
    }

    private void unlockDoor() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                Log.d(TAG, "Publishing 'unlock' to button_press topic");
                mqttClient.publish("button_press", new MqttMessage("unlock".getBytes()));
            } else {
                Log.e(TAG, "MQTT client is not connected");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error publishing to MQTT broker: " + e.getMessage());
        }
    }

    private void lockDoor() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                Log.d(TAG, "Publishing 'lock' to button_press topic");
                mqttClient.publish("button_press", new MqttMessage("lock".getBytes()));
            } else {
                Log.e(TAG, "MQTT client is not connected");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error publishing to MQTT broker: " + e.getMessage());
        }
    }

    private void automode() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                Log.d(TAG, "Publishing 'lock' to button_press topic");
                mqttClient.publish("button_press", new MqttMessage("auto-mode".getBytes()));
            } else {
                Log.e(TAG, "MQTT client is not connected");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error publishing to MQTT broker: " + e.getMessage());
        }
    }

    private void downloadLogFile() {
        try {
            String fileName = "doorlogs.txt";
            File file = new File(getFilesDir(), fileName);

            if (file.exists()) {
                File downloadDir = new File(Environment.getExternalStorageDirectory(), "Download");
                downloadDir.mkdirs();
                File destinationFile = new File(downloadDir, fileName);
                try (FileInputStream inStream = new FileInputStream(file);
                     FileOutputStream outStream = new FileOutputStream(destinationFile);
                     FileChannel inChannel = inStream.getChannel();
                     FileChannel outChannel = outStream.getChannel()) {
                    inChannel.transferTo(0, inChannel.size(), outChannel);
                }

                Log.d(TAG, "File downloaded to: " + destinationFile.getAbsolutePath());
            } else {
                Log.e(TAG, "File does not exist");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error downloading file: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            Log.d(TAG, "Disconnecting from MQTT broker");
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error disconnecting from MQTT broker: " + e.getMessage());
        }
    }
}
