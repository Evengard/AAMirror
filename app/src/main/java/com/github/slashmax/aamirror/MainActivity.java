package com.github.slashmax.aamirror;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        this.unlock();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    private void unlock() {
        TextView operationLog = findViewById(R.id.operationLog);

        if (!Shell.SU.available()) {
            operationLog.setText(R.string.no_root_detected);
            return;
        }

        if (!Unlocker.isLocked()) {
            operationLog.setText(R.string.already_unlocked);
            return;
        }

        List<String> log = Unlocker.unlock(/*true, true*/);

        StringBuilder sb = new StringBuilder();

        for (String item : log) {
            sb.append(item);
            sb.append("\n");
        }

        sb.append("FINISHED");

        operationLog.setText(sb.toString());
    }
}
