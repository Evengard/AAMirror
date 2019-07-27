package com.github.slashmax.aamirror;

import eu.chainfire.libsuperuser.Shell.OnCommandResultListener2;
import eu.chainfire.libsuperuser.Shell.OnResult;

import java.util.ArrayList;
import java.util.List;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

public class Shell {
    private static final String TAG = "Shell";

    private static eu.chainfire.libsuperuser.Shell.Threaded shell = null;

    /*static {
        InitShell();
    }*/

    private synchronized static void InitShell()
    {
        if (shell == null && eu.chainfire.libsuperuser.Shell.SU.available()) {
            shell = new eu.chainfire.libsuperuser.Shell.Builder().useSU().openThreaded();
        }
    }

    private Shell() {
    }

    public static boolean isAvailable()
    {
        InitShell();
        return shell != null;
    }

    public static List<String> exec(String cmd)
    {
        return exec(cmd, true);
    }

    private static Entry<String, OnResult> getCommandEntry(String cmd, OnResult callback)
    {
        return new SimpleEntry<String, OnResult>(cmd, callback);
    }

    public static List<String> exec(String cmd, boolean withSelinuxOverride)
    {
        return exec(new String[] {cmd}, withSelinuxOverride);
    }

    public static List<String> exec(String[] cmds, boolean withSelinuxOverride)
    {
        List<Entry<String, OnResult>> commands = new ArrayList<Entry<String, OnResult>>();
        @SuppressWarnings("unchecked")
        final List<String>[] results = new List[]{new ArrayList<String>(), new ArrayList<String>()};
        OnResult callback = (OnCommandResultListener2) (commandCode, exitCode, STDOUT, STDERR) -> {
            results[0].addAll(STDOUT);
            results[1].addAll(STDERR);
        };
        for(String cmd : cmds)
        {
            commands.add(getCommandEntry(cmd, callback));
        }
        if (withSelinuxOverride)
        {
            commands.add(0, getCommandEntry("setenforce 0", null));
            commands.add(getCommandEntry("setenforce 1", null));
        }
        exec(commands);
        List<String> merged = new ArrayList<String>();
        merged.addAll(results[0]);
        merged.addAll(results[1]);
        return merged;
    }

    public static synchronized void exec(List<Entry<String, OnResult>> cmds)
    {
        InitShell();
        shell.waitForIdle();
        for(Entry<String, OnResult> entry : cmds)
        {
            String cmd = entry.getKey();
            OnResult callback = entry.getValue();
            shell.addCommand(cmd, 0, callback);
        }
        shell.waitForIdle();
    }

    public static synchronized void close()
    {
        if (shell != null) {
            shell.close();
        }
    }
}
