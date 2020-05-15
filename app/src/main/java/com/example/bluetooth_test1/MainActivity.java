package com.example.bluetooth_test1;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void changeActivity(View View)
    {
        String button_text;
        button_text =((Button)View).getText().toString();
        if(button_text.equals("Bluetooth")) {
            Intent intent = new Intent(this, BluetoothActivity.class);
            startActivity(intent);
        }
    }
}
