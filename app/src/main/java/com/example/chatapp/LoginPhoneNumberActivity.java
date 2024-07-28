package com.example.chatapp;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.hbb20.CountryCodePicker;

public class LoginPhoneNumberActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_phone_number); // This should be the only call to setContentView

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Declare your UI components
        CountryCodePicker countryCodePicker;
        EditText phoneInput;
        Button sendOtpButton;
        ProgressBar progressBar;

        progressBar = findViewById(R.id.login_progress_bar);
        progressBar.setVisibility(View.GONE);

        countryCodePicker = findViewById(R.id.login_countrycode);
        phoneInput = findViewById(R.id.login_mobile_number);
        sendOtpButton = findViewById(R.id.send_otp_btn);

        countryCodePicker.registerCarrierNumberEditText(phoneInput);
        sendOtpButton.setOnClickListener(v -> {
            if (!countryCodePicker.isValidFullNumber()) {
                phoneInput.setError("Phone number is not valid");
                return;
            }
            Intent intent = new Intent(LoginPhoneNumberActivity.this, LoginOtpActivity.class);
            intent.putExtra("phone", countryCodePicker.getFullNumberWithPlus());
            startActivity(intent);
        });
    }
}

