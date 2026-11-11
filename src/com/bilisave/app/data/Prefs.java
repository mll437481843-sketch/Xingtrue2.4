package com.bilisave.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Local-only preferences. Cookie never leaves this device and is never logged. */
public final class Prefs {
    private static final String FILE = "bilisave";
    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String cookie(Context c) { return sp(c).getString("cookie", ""); }
    public static void setCookie(Context c, String v) { sp(c).edit().putString("cookie", v == null ? "" : v).apply(); }

    public static String ua(Context c) { return sp(c).getString("ua", ""); }
    public static void setUa(Context c, String v) { sp(c).edit().putString("ua", v == null ? "" : v).apply(); }

    public static boolean dynamic(Context c) { return sp(c).getBoolean("dynamic", true); }
    public static void setDynamic(Context c, boolean v) { sp(c).edit().putBoolean("dynamic", v).apply(); }

    /** 0 follow system, 1 light, 2 dark */
    public static int themeMode(Context c) { return sp(c).getInt("themeMode", 0); }
    public static void setThemeMode(Context c, int v) { sp(c).edit().putInt("themeMode", v).apply(); }

    public static String dirUri(Context c) { return sp(c).getString("dirUri", ""); }
    public static void setDirUri(Context c, String v) { sp(c).edit().putString("dirUri", v == null ? "" : v).apply(); }

    public static String tasksJson(Context c) { return sp(c).getString("tasks", "[]"); }
    public static void setTasksJson(Context c, String v) { sp(c).edit().putString("tasks", v).apply(); }

    // ---- accounts ----
    public static class Account {
        public String id = "";
        public String name = "";
        public String face = "";
        public String cookie = "";
    }

    public static List<Account> accounts(Context c) {
        List<Account> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(sp(c).getString("accounts", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Account ac = new Account();
                ac.id = o.optString("id");
                ac.name = o.optString("name");
                ac.face = o.optString("face");
                ac.cookie = o.optString("cookie");
                out.add(ac);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static void saveAccounts(Context c, List<Account> list) {
        JSONArray a = new JSONArray();
        for (Account ac : list) {
            JSONObject o = new JSONObject();
            try {
                o.put("id", ac.id);
                o.put("name", ac.name);
                o.put("face", ac.face);
                o.put("cookie", ac.cookie);
                a.put(o);
            } catch (Exception ignored) {
            }
        }
        sp(c).edit().putString("accounts", a.toString()).apply();
    }

    public static String currentAccountId(Context c) { return sp(c).getString("accId", ""); }
    public static void setCurrentAccountId(Context c, String v) { sp(c).edit().putString("accId", v).apply(); }
}

