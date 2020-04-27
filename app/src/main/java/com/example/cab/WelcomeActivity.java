package com.example.cab;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class WelcomeActivity extends AppCompatActivity {
    private Button WelcomeDriverButton;
    private Button WelcomeCustomerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        WelcomeCustomerButton = (Button) findViewById(R.id.welcome_customer_btn);
        WelcomeDriverButton = (Button) findViewById(R.id.welcome_driver_btn);

        WelcomeCustomerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent LoginRegisterCustomerIntent = new Intent(WelcomeActivity.this, CustomerLoginActivity.class);
                startActivity(LoginRegisterCustomerIntent);
            }
        });
        WelcomeDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent LoginRegisterDriverIntent = new Intent(WelcomeActivity.this, DriverLoginActivity.class);
                startActivity(LoginRegisterDriverIntent);
            }
        });
    }
}
