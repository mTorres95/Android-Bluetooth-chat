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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1; // To enable the Bluetooth
    BluetoothAdapter myBluetoothAdapter;
    BluetoothDevice[] btArray;

    SendReceive sendReceive;

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
    private static final UUID MY_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");


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
        // BUTTON LIST DEVICES
        buttonListDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get the devices and put them in a list
                Set<BluetoothDevice> btDevices = myBluetoothAdapter.getBondedDevices();
                String[] strings = new String[btDevices.size()];
                // initialize btArray
                btArray = new BluetoothDevice[btDevices.size()];
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

        // BUTTON LISTEN
        buttonListen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ServerClass serverClass = new ServerClass();
                serverClass.start();
            }
        });

        // LIST VIEW FOR THE DEVICES
        listDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ClientClass clientClass = new ClientClass(btArray[position]);
                clientClass.start();

                status.setText("Connecting..");
            }
        });

        // BUTTON SEND
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String string = String.valueOf(writeMsg.getText());
                sendReceive.write(string.getBytes());
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
                    status.setText("Connecting..");
                    break;
                case STATE_CONNECTED:
                    status.setText("Connected..");
                    break;
                case STATE_CONNECTION_FAILED:
                    status.setText("Connection failed");
                    break;
                case STATE_MESSAGE_RECEIVED:
                    // write/receive
                    byte[] readBuffer = (byte[]) msg.obj;
                    String tempMsg = new String(readBuffer, 0, msg.arg1);
                    msg_box.setText(tempMsg);
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
                    sendReceive = new SendReceive(socket);
                    sendReceive.start();
                    break;
                }

            }
        }
    }

    // ============================= CLASS FOR CLIENT =============================
    private class ClientClass extends Thread {
        private BluetoothSocket socket;
        private BluetoothDevice device;

        // constructor
        public ClientClass (BluetoothDevice device1){
            device = device1;
            try {
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        public void run(){
            try {
                socket.connect();
                Message message = Message.obtain();
                message.what = STATE_CONNECTED;
                handler.sendMessage(message);

                // write/receive
                sendReceive = new SendReceive(socket);
                sendReceive.start();

            } catch (IOException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
    }

    // ============================= CLASS FOR SEND/RECEIVE =============================
    private class SendReceive extends Thread
    {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive (BluetoothSocket socket){
            bluetoothSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try {
                tempIn = bluetoothSocket.getInputStream();
                tempOut = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream = tempIn;
            outputStream = tempOut;
        }

        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;

            while(true){
                try {
                    bytes = inputStream.read(buffer);
                    // second arg: number of bytes; third: not used, so -1; fourth: object
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes){
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}







