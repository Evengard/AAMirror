package com.github.slashmax.aamirror;

import android.content.res.Resources;

import java.util.ArrayList;
import java.util.List;

//import eu.chainfire.libsuperuser.Shell;

class Unlocker {
    private static final String OUR_PACKAGE = Unlocker.class.getPackage().getName();
    private static final String CAR_ROOT_PACKAGE = "com.google.android.gms.car";
    private static final String CAR_PACKAGES_PATTERN = CAR_ROOT_PACKAGE + "%";
    private static final String TRIGGER_NAME = "after_delete_mirror";

    static List<String> unlock(Resources resources) {
        CommandBuilder builder = new CommandBuilder();

        cleanUp(resources, builder);

        builder.print(resources.getString(R.string.adding_to_white_list));
        String unlockSql = String.format("INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) " +
                        "SELECT DISTINCT '%1$s', version, 0, 0, '', 'app_white_list', '%2$s', 1 FROM " +
                        "(SELECT version FROM Packages WHERE packageName = '%1$s' " +
                        "UNION SELECT version FROM ApplicationStates WHERE packageName = '%1$s') AS t",
                CAR_ROOT_PACKAGE, OUR_PACKAGE);
        builder.sql(unlockSql);

        builder.print(resources.getString(R.string.adding_trigger));
        builder.sql("CREATE TRIGGER %1$s AFTER DELETE ON Flags BEGIN %2$s; END", TRIGGER_NAME, unlockSql);

        restartServices(resources, builder);

        return Shell.exec(builder.toArray(), false);
    }

    static List<String> relock(Resources resources) {
        CommandBuilder builder = new CommandBuilder();

        cleanUp(resources, builder);
        restartServices(resources, builder);

        return Shell.exec(builder.toArray(), false);
    }

    static boolean isLocked() {
        CommandBuilder builder = new CommandBuilder();
        builder.sql("SELECT COUNT(DISTINCT version) FROM (SELECT version FROM Packages WHERE packageName = '%1$s' UNION SELECT version FROM ApplicationStates WHERE packageName = '%1$s') AS t", CAR_ROOT_PACKAGE);
        builder.sql("SELECT result FROM (SELECT DISTINCT version, CASE WHEN name = 'app_white_list' THEN 1 ELSE 0 END AS result FROM Flags WHERE (name = 'app_black_list' OR name = 'app_white_list') AND stringVal LIKE '%%%1$s%%' AND packageName = '%2$s') AS t", OUR_PACKAGE, CAR_ROOT_PACKAGE);
        builder.add("echo \"-\"");
        builder.sql("SELECT COUNT() FROM sqlite_master WHERE type = 'trigger' AND tbl_name = 'Flags' AND name = '%1$s'", TRIGGER_NAME);
        builder.sql("SELECT COUNT() FROM sqlite_master WHERE type = 'trigger' AND tbl_name = 'Flags' AND name != '%1$s'", TRIGGER_NAME);

        List<String> results = Shell.exec(builder.toArray(), false);

        if (results.size() < 1) {
            return true;
        }
        int count;
        try {
            count = Integer.parseInt(results.get(0));
        } catch (NumberFormatException e) {
            return true;
        }
        int i;
        for (i = 1 ; i < results.size(); i++) {
            String item = results.get(i);
            if (item.equals("0")) {
                return true;
            }
            if (item.equals("1")) {
                count--;
            } else {
                break;
            }
        }
        return count != 0
                || results.size() <= i + 2
                || !results.get(i + 1).equals("1")
                || !results.get(i + 2).equals("0");
    }

    private static void cleanUp(Resources resources, CommandBuilder builder) {
        dropTriggers(resources, builder, getTriggerList());

        builder.print(resources.getString(R.string.removing_bw_lists));

        builder.sql("DELETE FROM Flags WHERE (name = 'app_black_list' OR name = 'app_white_list') AND packageName LIKE '%1$s'", CAR_PACKAGES_PATTERN);
        builder.sql("DELETE FROM FlagOverrides WHERE (name = 'app_black_list' OR name = 'app_white_list') AND packageName LIKE '%1$s'", CAR_PACKAGES_PATTERN);
    }

    private static List<String> getTriggerList() {
        CommandBuilder builder = new CommandBuilder();
        builder.sql("SELECT name FROM sqlite_master WHERE type = 'trigger' AND tbl_name = 'Flags'");
        return Shell.exec(builder.toArray(), false);
    }

    private static void dropTriggers(Resources resources, CommandBuilder builder, List<String> triggers) {
        for (String trigger : triggers) {
            builder.print(resources.getString(R.string.dropping_trigger), trigger);
            builder.sql("DROP TRIGGER %1$s", trigger);
        }
    }

    private static void restartServices(Resources resources, CommandBuilder builder) {
        builder.print(resources.getString(R.string.restarting_services));

        builder.stopService("com.google.android.gms");
        builder.stopService("com.google.android.projection.gearhead");
    }

    private static class CommandBuilder {
        private List<String> list = new ArrayList<>();

        void add(String command) {
            this.list.add(command);
        }

        void add(String command, Object... args) {
            this.add(String.format(command, args));
        }

        void sql(String sql) {
            this.add(String.format("sqlite3 /data/data/com.google.android.gms/databases/phenotype.db \"%1$s;\"", sql));
        }

        void sql(String sql, Object... args) {
            this.sql(String.format(sql, args));
        }

        void print(String str) {
            this.add("echo \"%1$s\"", str);
        }

        void print(String format, Object... args) {
            this.print(String.format(format, args));
        }

        void stopService(String name) {
            this.add(String.format("am force-stop %1$s", name));
        }

        List<String> toList() {
            return this.list;
        }

        String[] toArray() {
            return this.toList().toArray(new String[0]);
        }
    }
}