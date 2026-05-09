package com.taskflow.automate.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class PreferenceManager {

    private static final String PREF_NAME = "taskflow_prefs";

    private static final String KEY_BLOCKED_APPS = "blocked_apps";
    private static final String KEY_KNOWN_APPS = "known_apps";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_REMINDER_INTERVAL_HIGH = "reminder_interval_high";
    private static final String KEY_REMINDER_INTERVAL_MEDIUM = "reminder_interval_medium";
    private static final String KEY_REMINDER_INTERVAL_LOW = "reminder_interval_low";

    private static final int DEFAULT_HIGH_INTERVAL = 30;
    private static final int DEFAULT_MEDIUM_INTERVAL = 60;
    private static final int DEFAULT_LOW_INTERVAL = 120;

    private final SharedPreferences prefs;

    public PreferenceManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public Set<String> getBlockedApps() {
        return new HashSet<>(prefs.getStringSet(KEY_BLOCKED_APPS, new HashSet<>()));
    }

    public void setBlockedApps(Set<String> blockedApps) {
        prefs.edit().putStringSet(KEY_BLOCKED_APPS, blockedApps).apply();
    }

    public void addKnownApp(String packageName) {
        Set<String> knownApps = getKnownApps();
        knownApps.add(packageName);
        prefs.edit().putStringSet(KEY_KNOWN_APPS, knownApps).apply();
    }

    public Set<String> getKnownApps() {
        return new HashSet<>(prefs.getStringSet(KEY_KNOWN_APPS, new HashSet<>()));
    }

    public int getReminderInterval(int priority) {
        switch (priority) {
            case 1:
                return prefs.getInt(KEY_REMINDER_INTERVAL_HIGH, DEFAULT_HIGH_INTERVAL);
            case 2:
                return prefs.getInt(KEY_REMINDER_INTERVAL_MEDIUM, DEFAULT_MEDIUM_INTERVAL);
            case 3:
            default:
                return prefs.getInt(KEY_REMINDER_INTERVAL_LOW, DEFAULT_LOW_INTERVAL);
        }
    }

    public void setReminderInterval(int priority, int minutes) {
        String key;
        switch (priority) {
            case 1:
                key = KEY_REMINDER_INTERVAL_HIGH;
                break;
            case 2:
                key = KEY_REMINDER_INTERVAL_MEDIUM;
                break;
            case 3:
            default:
                key = KEY_REMINDER_INTERVAL_LOW;
                break;
        }
        prefs.edit().putInt(key, minutes).apply();
    }

    public boolean isFirstLaunch() {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true);
    }

    public void setFirstLaunchDone() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
    }
}
