package com.github.slashmax.aamirror;

import android.util.Log;

import eu.chainfire.libsuperuser.Shell.*;

import java.util.ArrayList;
import java.util.List;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

public class Shell {
    private static final String TAG = "Shell";

    private static PoolWrapper shellPool = null;
    private static Threaded lastShell = null;

    private static boolean isSelinuxEnforcing = false;
    private static boolean available = false;
    /*static {

    }*/

    private static void InitShell()
    {
        available = available || SU.available();
        if (shellPool == null && available) {
            isSelinuxEnforcing = SU.isSELinuxEnforcing();
            Pool.setPoolSize(5);
            shellPool = Pool.SU;
        }
    }

    private Shell() {
    }

    private static synchronized Threaded getShell() throws ShellDiedException
    {
        InitShell();
        if (lastShell != null && lastShell.isIdle())
        {
            return lastShell;
        }
        if (lastShell != null)
        {
            lastShell.closeWhenIdle();
            lastShell = null;
        }
        try {
            lastShell = shellPool.get();
        } catch (ShellDiedException e) {
            Log.d(TAG, "Get shell exception: ", e);
            available = false;
            throw e;
        }
        return lastShell;
    }

    public static synchronized boolean isAvailable()
    {
        InitShell();
        return available;
    }

    public static List<String> exec(String cmd)
    {
        return exec(cmd, true);
    }

    private static Entry<String, Boolean> getCommandEntry(String cmd, boolean needsOutput)
    {
        return new SimpleEntry<String, Boolean>(cmd, needsOutput);
    }

    public static List<String> exec(String cmd, boolean withSelinuxOverride)
    {
        return exec(new String[] {cmd}, withSelinuxOverride);
    }

    public static List<String> exec(String[] cmds, boolean withSelinuxOverride)
    {
        List<Entry<String, Boolean>> commands = new ArrayList<Entry<String, Boolean>>();
        @SuppressWarnings("unchecked")
        final List<String>[] results = new List[]{new ArrayList<String>(), new ArrayList<String>()};
        for(String cmd : cmds)
        {
            commands.add(getCommandEntry(cmd, true));
        }
        if (withSelinuxOverride && isSelinuxEnforcing)
        {
            commands.add(0, getCommandEntry("setenforce 0", false));
            commands.add(getCommandEntry("setenforce 1", false));
        }
        exec(commands);
        List<String> merged = new ArrayList<String>();
        merged.addAll(results[0]);
        merged.addAll(results[1]);
        return merged;
    }

    public static List<Entry<List<String>, List<String>>> exec(List<Entry<String, Boolean>> cmds)
    {
        List<Entry<List<String>, List<String>>> results = new ArrayList<Entry<List<String>, List<String>>>();
        try {
            Threaded shell = getShell();
            shell.waitForIdle();
            for (Entry<String, Boolean> entry : cmds) {
                int exit = -1;
                String cmd = entry.getKey();
                boolean needsOutput = entry.getValue();
                List<String> stdout = needsOutput ? new ArrayList<String>() : null;
                List<String> stderr = needsOutput ? new ArrayList<String>() : null;
                exit = shell.run(cmd, stdout, stderr, true);
                Log.d(TAG, "Shell result: code " + exit + ", cmdline: " + cmd);
                if (needsOutput) {
                    Entry<List<String>, List<String>> result = new SimpleEntry<List<String>, List<String>>(stdout, stderr);
                    results.add(result);
                }
            }
            shell.closeWhenIdle();
        }
        catch (Exception e)
        {
            Log.d(TAG, "Shell execution exception: ", e);
        }
        return results;
    }
}
