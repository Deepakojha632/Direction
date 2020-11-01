package com.iyetechsoft.direction;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ATCommand extends AppCompatActivity {
    private TextView output;
    private Button executeCommand, direction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_a_t_command);

        output = findViewById(R.id.output);
        direction = findViewById(R.id.directionView);
        executeCommand = findViewById(R.id.executeBtn);

        executeCommand.setOnClickListener(v -> {
            try {
                //su previledge required
                Process process = Runtime.getRuntime().exec("ATI");
                BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                output.setText("Output\n\u2500\u2500\u2500\u2500\u2500\u2500\u2500\n\n" + in.readLine());
            } catch (IOException e) {
                output.setText("Output\n\u2500\u2500\u2500\u2500\u2500\u2500\u2500\n\n" + e.getMessage());
            }
        });

        direction.setOnClickListener(v -> {
            startActivity(new Intent(ATCommand.this, MainActivity.class));
            ATCommand.this.finishAffinity();

        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        ATCommand.this.finishAffinity();

    }
}