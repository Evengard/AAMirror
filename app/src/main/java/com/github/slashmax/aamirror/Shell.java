package com.github.slashmax.aamirror;

import android.os.AsyncTask;
import android.util.Log;

import java.util.concurrent.Executor;

public class Shell {
    private static final String TAG = "Shell";

    private static eu.chainfire.libsuperuser.Shell.Interactive shell = null;
    private static ShellDirectExecutor shellExecutor = new ShellDirectExecutor();

    static {
        if (eu.chainfire.libsuperuser.Shell.SU.available()) {
            shell = new eu.chainfire.libsuperuser.Shell.Builder().useSU().open();
        }
    }

    private Shell() {
    }

    private static class ShellDirectExecutor implements Executor {
        public void execute(Runnable r) {
            new Thread(r).start();
        }
    }

    private static class ShellAsyncTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            Log.d(TAG, "doInBackground");
            if (Shell.shell != null) {
                Shell.shell.addCommand(params[0]);
            }
            return null;
        }
    }

    public static void exec(String cmd) {
        new ShellAsyncTask().executeOnExecutor(Shell.shellExecutor, "setenforce 0; " + cmd + "; setenforce 1");
    }

    public static void close() {
        if (shell != null) {
            shell.close();
        }
    }
}
