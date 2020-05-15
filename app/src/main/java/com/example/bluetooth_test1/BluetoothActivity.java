package com.example.bluetooth_test1;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class BluetoothActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1; // To enable the Bluetooth
    BluetoothAdapter myBluetoothAdapter;
    BluetoothDevice[] btArray;

    // objects from the layout
    Button buttonON, buttonOFF, buttonListen, buttonSend, buttonListDevices;
    ListView listDevices;
    TextView msg_box, status;
    EditText writeMsg;
    ArrayAdapter<String> arrayAdapter;

    // constants for the handler
    static final int STATE_LISTENING         = 1;
    static final int STATE_CONNECTING        = 2;
    static final int STATE_CONNECTED         = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECEIVED  = 5;

    private static final  String APP_NAME = "BTChat";
    private static final UUID MY_UUID = UUID.fromString("");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_view);

        myFindViewById();

        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothONMethod();
        bluetoothOFFMethod();

        implementListeners(); // paired devices
    }

    private void implementListeners() {
        buttonListDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get the devices and put them in a list
                Set<BluetoothDevice> btDevices = myBluetoothAdapter.getBondedDevices();
                String[] strings = new String[btDevices.size()];
                int index = 0;
                if(btDevices.size() > 0){
                    for(BluetoothDevice device:btDevices){
                        // save the devices in the array
                        btArray[index] = device;
                        // save the name of the devices in the array
                        strings[index] = device.getName();
                        index++;
                    }
                    arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1,strings);
                    listDevices.setAdapter(arrayAdapter);
                }
            }
        });
        buttonListen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ServerClass serverClass = new ServerClass();
                serverClass.start();
            }
        });
    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            // check the type of message
            switch (msg.what)
            {
                case STATE_LISTENING:
                    status.setText("Listening..");
                    break;
                case STATE_CONNECTING:
                    status.setText("Connectiong..");
                    break;
                case STATE_CONNECTED:
                    status.setText("Connected..");
                    break;
                case STATE_CONNECTION_FAILED:
                    status.setText("Connection failed");
                    break;
                case STATE_MESSAGE_RECEIVED:
                    break;
            }
            return false;
        }
    });

    private void myFindViewById() {
        buttonON = findViewById(R.id.btON);
        buttonOFF = findViewById(R.id.btOFF);
        buttonListen = findViewById(R.id.listen);
        buttonSend = findViewById(R.id.send);
        buttonListDevices = findViewById(R.id.listDevices);
        listDevices = findViewById(R.id.listViewDev);
        msg_box = findViewById(R.id.msg);
        status = findViewById(R.id.status);
        writeMsg = findViewById(R.id.writeMsg);
    }

    private void bluetoothOFFMethod() {
        buttonOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myBluetoothAdapter.isEnabled()){
                    myBluetoothAdapter.disable();
                    Toast.makeText(getApplicationContext(), "Bluetooth is disabled", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void bluetoothONMethod() {
        buttonON.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // is bluetooth supported?
                if(myBluetoothAdapter == null){
                    Toast.makeText(getApplicationContext(), "Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
                }
                else {
                    // is bluetooth enabled or not?
                    if(!myBluetoothAdapter.isEnabled()){
                        // it's not, so enable bluetooth
                        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(intent, REQUEST_ENABLE_BT); // it returns teh result of the activity
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data ) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), "Bluetooth is Enabled", Toast.LENGTH_SHORT).show();
            }
            else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(),"Bluetooth Enabling Canceled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ============================= CLASS FOR SERVER =============================
    private class ServerClass extends Thread{
        private BluetoothServerSocket serverSocket;

        public ServerClass(){
            try {
                serverSocket = myBluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){
            BluetoothSocket socket = null;

            while (socket == null){
                try {
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTING;
                    handler.sendMessage(message);

                    socket = serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();

                    Message message = Message.obtain();
                    message.what = STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }

                if (socket != null){
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTED;
                    handler.sendMessage(message);

                    //write the send/receive
                    break;
                }

            }
        }
    }

}







