package com.jacopomii.googledialermod;

import static com.jacopomii.googledialermod.Utils.execPhenotypeQuery;
import static com.jacopomii.googledialermod.Utils.killDialerAndDeletePhenotypeCache;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class DBFlagsSingleton {
    private static DBFlagsSingleton mUniqueInstance = null;
    private Map<String, Boolean> mDBBooleanFlags = new TreeMap<>();
    private Map<String, String> mDBStringFlags = new TreeMap<>();
    ArrayList<String> mDBUsers = new ArrayList<>();
    private Context mContext;

    private DBFlagsSingleton(Context context) {
        mContext = context;
        reloadDB();
    }

    public static DBFlagsSingleton getInstance(Context context) {
        if (mUniqueInstance == null)
            mUniqueInstance = new DBFlagsSingleton(context);
        return mUniqueInstance;
    }

    public Boolean getDBBooleanFlag(String flag) {
        return mDBBooleanFlags.get(flag);
    }

    public String getDBStringFlag(String flag) {
        return mDBStringFlags.get(flag);
    }

    public Map<String, Boolean> getDBBooleanFlags() {
        return mDBBooleanFlags;
    }

    public Map<String, String> getDBStringFlags() {
        return mDBStringFlags;
    }

    public void reloadDB() {
        reloadDBUsers();
        reloadDBBooleanFlags();
        reloadDBStringFlags();
    }

    private void reloadDBUsers() {
        mDBUsers.clear();
        JSONArray users = execPhenotypeQuery(mContext, "SELECT DISTINCT user FROM Flags WHERE packageName = 'com.google.android.dialer'");
        for (int i=0; i < users.length(); i++) {
            try {
                String user = users.getJSONObject(i).getString("user");
                mDBUsers.add(user);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void reloadDBBooleanFlags() {
        mDBBooleanFlags.clear();
        String[] tables = {"Flags", "FlagOverrides"};
        for (String table : tables) {
            JSONArray query_result = execPhenotypeQuery(mContext, "SELECT DISTINCT name, boolVal FROM " + table + " WHERE packageName = 'com.google.android.dialer' AND user = '' AND boolVal != 'NULL'");
            for (int i=0; i < query_result.length(); i++) {
                try {
                    JSONObject flag = query_result.getJSONObject(i);
                    mDBBooleanFlags.put(flag.getString("name"), flag.getInt("boolVal")!=0);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void reloadDBStringFlags() {
        mDBStringFlags.clear();
        String[] tables = {"Flags", "FlagOverrides"};
        for (String table : tables) {
            JSONArray query_result = execPhenotypeQuery(mContext, "SELECT DISTINCT name, stringVal FROM " + table + " WHERE packageName = 'com.google.android.dialer' AND user = '' AND stringVal != 'NULL'");
            for (int i=0; i < query_result.length(); i++) {
                try {
                    JSONObject flag = query_result.getJSONObject(i);
                    mDBStringFlags.put(flag.getString("name"), flag.getString("stringVal"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void updateDBFlag(String flag, boolean value) {
        mDBBooleanFlags.put(flag, value);
        killDialerAndDeletePhenotypeCache();
        execPhenotypeQuery(mContext, "DELETE FROM FlagOverrides WHERE packageName = 'com.google.android.dialer' AND name = '" + flag.replace("'", "\\'") + "'");
        for (String user : mDBUsers)
            execPhenotypeQuery(mContext, "INSERT OR REPLACE INTO FlagOverrides (packageName, flagType, name, user, boolVal, committed) VALUES ('com.google.android.dialer', 0, '" + flag.replace("'", "\\'") + "', '" + user.replace("'", "\\'") + "', " + (value ? 1 : 0) + ", 0)");
    }

    public void updateDBFlag(String flag, String value) {
        mDBStringFlags.put(flag, value);
        killDialerAndDeletePhenotypeCache();
        execPhenotypeQuery(mContext, "DELETE FROM FlagOverrides WHERE packageName = 'com.google.android.dialer' AND name = '" + flag.replace("'", "\\'") + "'");
        for (String user : mDBUsers)
            execPhenotypeQuery(mContext, "INSERT OR REPLACE INTO FlagOverrides (packageName, flagType, name, user, stringVal, committed) VALUES ('com.google.android.dialer', 0, '" + flag.replace("'", "\\'") + "', '" + user.replace("'", "\\'") + "', '" + value.replace("'", "\\'") + "', 0)");
    }

    public boolean areAllBooleanFlagsTrue(String... flags) {
        for (String flag : flags) {
            Boolean flagValue = getDBBooleanFlag(flag);
            if (flagValue == null || !flagValue)
                return false;
        }
        return true;
    }

    public boolean areAllStringFlagsEmpty(String... flags) {
        for (String flag : flags) {
            String flagValue = getDBStringFlag(flag);
            if (flagValue == null || !flagValue.isEmpty())
                return false;
        }
        return true;
    }

    public void deleteAllFlagOverrides() {
        killDialerAndDeletePhenotypeCache();
        execPhenotypeQuery(mContext, "DELETE FROM FlagOverrides WHERE packageName = 'com.google.android.dialer'");
        reloadDB();
    }
}