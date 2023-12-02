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
import android.os.Bundle;
import android.os.Environment;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MQTT";
    private MqttClient mqttClient;
    private TextView lockStatusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация TextView
        lockStatusTextView = findViewById(R.id.lockStatusTextView);

        // Инициализация кнопок
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


        // Подключение к MQTT-брокеру
        connectToMQTTBroker();
    }

    private void connectToMQTTBroker() {
        try {
            Log.d(TAG, "Connecting to MQTT broker");
            mqttClient = new MqttClient("tcp://192.168.1.129:1883", MqttClient.generateClientId(), null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            mqttClient.connect(options);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e(TAG, "Connection to MQTT broker lost: " + cause.getMessage());
                    // Handle connection lost
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // Handle incoming messages if needed
                    Log.d(TAG, "Message arrived on topic " + topic + ": " + new String(message.getPayload()));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d(TAG, "Message delivery complete");
                    // Handle message delivery completion if needed
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error connecting to MQTT broker: " + e.getMessage());
        }
    }

    private void updateLockStatus(String status) {
        // Обновите текст в TextView
        lockStatusTextView.setText("Lock Status: " + status);

        // Запишите статус в файл
        writeStatusToFile(status);
    }

    private void writeStatusToFile(String status) {
        try {
            // Имя файла, в который будут записываться статусы
            String fileName = "lock_status_log.txt";

            // Открываем или создаем файл для записи (MODE_APPEND для добавления в конец файла)
            FileOutputStream fileOutputStream = openFileOutput(fileName, MODE_APPEND);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);

            // Форматируем текущую дату и время
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            // Записываем статус и временную метку в файл
            outputStreamWriter.write(timeStamp + ": " + status + "\n");

            // Закрываем потоки
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

                // Обновите статус замка при открытии
                updateLockStatus("Unlocked");
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

                // Обновите статус замка при закрытии
                updateLockStatus("Locked");
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

                // Обновите статус замка при переключении в автоматический режим
                updateLockStatus("Auto Mode");
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
            String fileName = "lock_status_log.txt";

            // Получаем путь к файлу
            File file = new File(getFilesDir(), fileName);

            if (file.exists()) {
                // Создаем временную директорию для сохранения файла
                File downloadDir = new File(Environment.getExternalStorageDirectory(), "Download");
                downloadDir.mkdirs();

                // Создаем файл в директории Download
                File destinationFile = new File(downloadDir, fileName);

                // Копируем файл из внутренней директории в директорию Download
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
