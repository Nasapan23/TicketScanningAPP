package com.zero_ideas.qrscanner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.Toast;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private Set<String> targetTexts;
    private Set<String> scannedTexts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        targetTexts = readTargetTexts();
        scannedTexts = new HashSet<>();
        Button scanButton = findViewById(R.id.scan_button);
        scanButton.setOnClickListener(v -> startScan());
    }

    @SuppressLint("MutatingSharedPrefs")
    private Set<String> readTargetTexts() {
        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        Set<String> targetTexts = preferences.getStringSet("target_texts", null);
        if (targetTexts == null) {
            targetTexts = new HashSet<>();
            try {
                InputStream inputStream = getAssets().open("target_texts.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    targetTexts.add(line);
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            SharedPreferences.Editor editor = preferences.edit();
            editor.putStringSet("target_texts", targetTexts);
            editor.apply();
        }
        return targetTexts;
    }

    private void startScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Scan a QR code");
        integrator.setOrientationLocked(false);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setBeepEnabled(false);
        integrator.initiateScan();
    }

    private void analyzeCode(String scannedText) {
        if (scannedTexts.contains(scannedText)) {
            Toast.makeText(getApplicationContext(), "Code has already been scanned!", Toast.LENGTH_LONG).show();
            return;
        }
        scannedTexts.add(scannedText);
        String decodedText = decodeBase64(scannedText);
        if (decodedText == null) {
            Toast.makeText(getApplicationContext(), "Error decoding QR code!", Toast.LENGTH_LONG).show();
            return;
        }
        if (targetTexts.contains(decodedText)) {
            Toast.makeText(getApplicationContext(), "Code belongs to target texts!", Toast.LENGTH_LONG).show();
            // Display green screen
            getWindow().getDecorView().setBackgroundColor(Color.GREEN);
            // Remove code from target texts so it can't be scanned again
            targetTexts.remove(decodedText);
            SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putStringSet("target_texts", targetTexts);
            editor.apply();
        } else {
            Toast.makeText(getApplicationContext(), "Code does not belong to target texts.", Toast.LENGTH_LONG).show();
            // Display red screen
            getWindow().getDecorView().setBackgroundColor(Color.RED);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(getApplicationContext(), "Scan cancelled.", Toast.LENGTH_LONG).show();
            } else {
                String scannedText = result.getContents();
                analyzeCode(scannedText);
            }
        }
    }
    private String decodeBase64(String encodedText) {
        try {
            byte[] decodedBytes = Base64.decode(encodedText, Base64.DEFAULT);
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Reset screen color to default
        getWindow().getDecorView().setBackgroundColor(Color.BLACK);
    }
}