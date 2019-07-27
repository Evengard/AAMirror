package com.github.slashmax.aamirror;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private Button unlockButton;
    private Button relockButton;
    private TextView lockedTextView;
    private TextView unlockedTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        Toolbar toolbar = this.findViewById(R.id.toolbar);
        this.setSupportActionBar(toolbar);

        this.unlockButton = this.findViewById(R.id.unlockButton);
        this.relockButton = this.findViewById(R.id.relockButton);
        this.lockedTextView = MainActivity.this.findViewById(R.id.lockedTextView);
        this.unlockedTextView = MainActivity.this.findViewById(R.id.unlockedTextView);

        this.unlockButton.setOnClickListener(view -> new LockerTask(this.unlockButton, this.relockButton, this.getResources()).execute((IConsoleFunction) Unlocker::unlock));
        this.relockButton.setOnClickListener(view -> new LockerTask(this.unlockButton, this.relockButton, this.getResources()).execute((IConsoleFunction) Unlocker::relock));

        updateLockedStatus();
    }

    private void updateLockedStatus() {
        new IsLockedDetector().execute();
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

    @SuppressLint("StaticFieldLeak")
    private class LockerTask extends AsyncTask<IConsoleFunction, Void, List<String>> {
        private Button unlockButton;
        private Button relockButton;
        private Resources resources;

        LockerTask(Button unlockButton, Button relockButton, Resources resources) {
            super();

            this.unlockButton = unlockButton;
            this.relockButton = relockButton;
            this.resources = resources;
        }

        @Override
        protected void onPreExecute() {
            this.unlockButton.setEnabled(false);
            this.relockButton.setEnabled(false);
        }

        @Override
        protected List<String> doInBackground(IConsoleFunction... consoleFunctions) {
            List<String> result = new ArrayList<>();
            for (IConsoleFunction consoleFunction : consoleFunctions) {
                result.addAll(consoleFunction.call(this.resources));
            }
            return result;
        }

        @Override
        protected void onPostExecute(List<String> result) {
            try {
                StringBuilder sb = new StringBuilder();

                for (String item : result) {
                    sb.append(item);
                    sb.append("\n");
                }

                sb.append(this.resources.getString(R.string.finished));

                ((TextView) findViewById(R.id.operationLog)).setText(sb.toString());
            } finally {
                this.unlockButton.setEnabled(true);
                this.relockButton.setEnabled(true);

                updateLockedStatus();
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class IsLockedDetector extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            if (!Shell.isAvailable()) {
                ((TextView) findViewById(R.id.operationLog)).setText(R.string.no_root_detected);
                MainActivity.this.unlockButton.setEnabled(false);
                MainActivity.this.relockButton.setEnabled(false);
                lockedTextView.setVisibility(View.GONE);
                unlockedTextView.setVisibility(View.GONE);
                return true;
            }

            return Unlocker.isLocked();
        }

        @Override
        protected void onPostExecute(Boolean isLocked) {
            unlockedTextView.setVisibility(isLocked ? View.GONE : View.VISIBLE);
            lockedTextView.setVisibility(isLocked ? View.VISIBLE : View.GONE);
        }
    }

    private interface IConsoleFunction {
        List<String> call(Resources resources);
    }
}
