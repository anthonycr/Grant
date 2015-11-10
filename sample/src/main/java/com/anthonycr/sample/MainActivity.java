package com.anthonycr.sample;

import android.Manifest;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/test.txt";
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Requesting all the permissions in the manifest
        PermissionsManager.getInstance().requestAllManifestPermissionsIfNecessary(this, new PermissionsResultAction() {
            @Override
            public void onGranted() {
                Toast.makeText(MainActivity.this, R.string.message_granted, Toast.LENGTH_SHORT).show();
                writeToStorage();
                readFromStorage();
            }

            @Override
            public void onDenied(String permission) {
                String message = String.format(Locale.getDefault(), getString(R.string.message_denied), permission);
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });

        Button writeStorage = (Button) findViewById(R.id.button_write_storage);
        Button readStorage = (Button) findViewById(R.id.button_read_storage);
        Button readContacts = (Button) findViewById(R.id.button_read_contacts);

        this.textView = (TextView) findViewById(R.id.text);

        writeStorage.setOnClickListener(this);
        readStorage.setOnClickListener(this);
        readContacts.setOnClickListener(this);
    }

    /**
     * Requires Permission: Manifest.permission.WRITE_EXTERNAL_STORAGE
     */
    private void writeToStorage() {
        File file = new File(PATH);
        String test = "Hello, World!";
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            outputStream.write(test.getBytes());
            this.textView.setText(String.format(Locale.getDefault(), getString(R.string.text_write), test));
        } catch (IOException e) {
            Log.e(TAG, "Unable to write to storage", e);
            this.textView.setText(R.string.text_failure_write);
        } finally {
            close(outputStream);
        }
    }

    /**
     * Requires Permission: Manifest.permission.READ_EXTERNAL_STORAGE
     */
    private void readFromStorage() {
        File file = new File(PATH);
        BufferedReader inputStream = null;
        FileInputStream input = null;
        try {
            input = new FileInputStream(file);
            inputStream = new BufferedReader(new InputStreamReader(input));
            String test = inputStream.readLine();
            this.textView.setText(String.format(Locale.getDefault(), getString(R.string.text_read), test));
        } catch (IOException e) {
            Log.e(TAG, "Unable to read from storage", e);
            this.textView.setText(R.string.text_failure_read);
        } finally {
            close(input);
            close(inputStream);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "Activity-onRequestPermissionsResult() PermissionsManager.notifyPermissionsChange()");
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_write_storage:
                PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, new PermissionsResultAction() {

                            @Override
                            public void onGranted() {
                                Log.i(TAG, "onGranted: Write Storage");
                                writeToStorage();
                            }

                            @Override
                            public void onDenied(String permission) {
                                Log.i(TAG, "onDenied: Write Storage");
                                String message = String.format(Locale.getDefault(), getString(R.string.message_denied), permission);
                                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        }
                );
                break;
            case R.id.button_read_storage:
                PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, new PermissionsResultAction() {

                            @Override
                            public void onGranted() {
                                Log.i(TAG, "onGranted: Read Storage");
                                readFromStorage();
                            }

                            @Override
                            public void onDenied(String permission) {
                                Log.i(TAG, "onDenied: Read Storage");
                                String message = String.format(Locale.getDefault(), getString(R.string.message_denied), permission);
                                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        }
                );
                break;
            case R.id.button_read_contacts:
                PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(this,
                        new String[]{Manifest.permission.READ_CONTACTS}, new PermissionsResultAction() {

                            @Override
                            public void onGranted() {
                                Log.i(TAG, "onGranted: Read Contacts");
                                ContactsUtils.readPhoneContacts(MainActivity.this);
                            }

                            @Override
                            public void onDenied(String permission) {
                                Log.i(TAG, "onDenied: Read Contacts");
                                String message = String.format(Locale.getDefault(), getString(R.string.message_denied), permission);
                                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        }
                );
                break;
        }
    }

    private static void close(@Nullable Closeable closeable) {
        if (closeable == null) {return;}
        try {
            closeable.close();
        } catch (IOException ignored) {}
    }


    // I found an issue, which can be seen from the logs, here is the steps:
    // 1. click read contacts button, popup the dialog, select "Allow", the Log "onGranted: Read Contacts" shown.
    // 2. click the read contacts button again, do the read contacts work directly, the Log "onGranted: Read Contacts" shown.
    // 3. click the read storage button, popup the dialog, select "Deny".
    // Bug: Both the Log "onDenied: Read Contacts" and "onDenied: Read Storage" shown.

    // If we repeat the step 2 for many times, we will get many "onDenied: Read Contacts" logs on step 3.

    // Reason: if we request a permission we already have, the onRequestPermissionsResult() is not invoked
    // so the PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults); is not invoked too.
}


