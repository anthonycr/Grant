package com.anthonycr.sample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
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
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/test.txt";
    private TextView textView;

    @SuppressLint("InlinedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Requesting all the permissions in the manifest
        PermissionsManager.getInstance().requestAllManifestPermissionsIfNecessary(this, new PermissionsResultAction() {
            @Override
            public void onGranted() {
                Toast.makeText(MainActivity.this, R.string.message_granted, Toast.LENGTH_SHORT).show();
                writeToStorage("Hello, World!");
                readFromStorage();
            }

            @Override
            public void onDenied(String permission) {
                String message = String.format(Locale.getDefault(), getString(R.string.message_denied), permission);
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });

        boolean hasPermission = PermissionsManager.getInstance().hasPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        Log.d(TAG, "Has " + Manifest.permission.READ_EXTERNAL_STORAGE + " permission: " + hasPermission);

        Button writeStorage = (Button) findViewById(R.id.button_write_storage);
        Button readStorage = (Button) findViewById(R.id.button_read_storage);
        Button readContacts = (Button) findViewById(R.id.button_read_contacts);
        Button getLocation = (Button) findViewById(R.id.button_get_location);

        this.textView = (TextView) findViewById(R.id.text);

        writeStorage.setOnClickListener(this);
        readStorage.setOnClickListener(this);
        readContacts.setOnClickListener(this);
        getLocation.setOnClickListener(this);
    }

    /**
     * Requires Permission: Manifest.permission.WRITE_EXTERNAL_STORAGE
     */
    private void writeToStorage(String text) {
        File file = new File(PATH);
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
            outputStream.write(text.getBytes());
            this.textView.setText(String.format(Locale.getDefault(), getString(R.string.text_write), text));
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

    @SuppressLint("InlinedApi")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_write_storage:
                PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, new PermissionsResultAction() {

                            @Override
                            public void onGranted() {
                                Log.i(TAG, "onGranted: Write Storage");
                                writeToStorage("Hello, World!");
                            }

                            @Override
                            public void onDenied(String permission) {
                                Log.i(TAG, "onDenied: Write Storage: " + permission);
                                String message = String.format(Locale.getDefault(), getString(R.string.message_denied), permission);
                                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        }
                );
                break;
            case R.id.button_read_storage:
                PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, new PermissionsResultAction() {

                            @Override
                            public void onGranted() {
                                Log.i(TAG, "onGranted: Read Storage");
                                readFromStorage();
                            }

                            @Override
                            public void onDenied(String permission) {
                                Log.i(TAG, "onDenied: Read Storage: " + permission);
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
            case R.id.button_get_location:
                PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        new PermissionsResultAction() {
                            @Override
                            public void onGranted() {
                                double[] location = getCoordinates();
                                for (double coord : location) {
                                    Log.d(TAG, "Coordinate: " + coord);
                                    writeToStorage("Coordinate: " + coord);
                                }
                            }

                            @Override
                            public void onDenied(String permission) {
                                Log.e(TAG, "Unable to get location without permission");
                            }
                        });
                break;
        }
    }

    private static void close(@Nullable Closeable closeable) {
        if (closeable == null) {return;}
        try {
            closeable.close();
        } catch (IOException ignored) {}
    }

    private double[] getCoordinates() {
        LocationManager lm = (LocationManager) getSystemService(
                Context.LOCATION_SERVICE);
        List<String> providers = lm.getProviders(true);

        Location l = null;

        for (int i = providers.size() - 1; i >= 0; i--) {
            //noinspection ResourceType
            l = lm.getLastKnownLocation(providers.get(i));
            if (l != null) break;
        }

        double[] gps = new double[2];
        if (l != null) {
            gps[0] = l.getLatitude();
            gps[1] = l.getLongitude();
        }

        return gps;
    }

}


