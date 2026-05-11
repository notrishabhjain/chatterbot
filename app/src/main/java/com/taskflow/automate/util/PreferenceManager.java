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
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_REMINDER_INTERVAL_HIGH = "reminder_interval_high";
    private static final String KEY_REMINDER_INTERVAL_MEDIUM = "reminder_interval_medium";
    private static final String KEY_REMINDER_INTERVAL_LOW = "reminder_interval_low";
    private static final String KEY_SMTP_HOST = "smtp_host";
    private static final String KEY_SMTP_PORT = "smtp_port";
    private static final String KEY_EMAIL_USERNAME = "email_username";
    private static final String KEY_EMAIL_PASSWORD = "email_password";
    private static final String KEY_EMAIL_RECIPIENT = "email_recipient";
    private static final String KEY_EMAIL_FREQUENCY = "email_frequency";
    private static final String KEY_QUICK_ADD_ENABLED = "quick_add_enabled";

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

    public String getThemeMode() {
        return prefs.getString(KEY_THEME_MODE, "system");
    }

    public void setThemeMode(String mode) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply();
    }

    public boolean isFirstLaunch() {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true);
    }

    public void setFirstLaunchDone() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
    }

    // Email Digest Config
    public String getSmtpHost() {
        return prefs.getString(KEY_SMTP_HOST, null);
    }

    public void setSmtpHost(String host) {
        prefs.edit().putString(KEY_SMTP_HOST, host).apply();
    }

    public String getSmtpPort() {
        return prefs.getString(KEY_SMTP_PORT, "587");
    }

    public void setSmtpPort(String port) {
        prefs.edit().putString(KEY_SMTP_PORT, port).apply();
    }

    public String getEmailUsername() {
        return prefs.getString(KEY_EMAIL_USERNAME, null);
    }

    public void setEmailUsername(String username) {
        prefs.edit().putString(KEY_EMAIL_USERNAME, username).apply();
    }

    public String getEmailPassword() {
        return prefs.getString(KEY_EMAIL_PASSWORD, null);
    }

    public void setEmailPassword(String password) {
        prefs.edit().putString(KEY_EMAIL_PASSWORD, password).apply();
    }

    public String getEmailRecipient() {
        return prefs.getString(KEY_EMAIL_RECIPIENT, null);
    }

    public void setEmailRecipient(String recipient) {
        prefs.edit().putString(KEY_EMAIL_RECIPIENT, recipient).apply();
    }

    public String getEmailFrequency() {
        return prefs.getString(KEY_EMAIL_FREQUENCY, "daily");
    }

    public void setEmailFrequency(String frequency) {
        prefs.edit().putString(KEY_EMAIL_FREQUENCY, frequency).apply();
    }

    public boolean isQuickAddEnabled() {
        return prefs.getBoolean(KEY_QUICK_ADD_ENABLED, true);
    }

    public void setQuickAddEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_QUICK_ADD_ENABLED, enabled).apply();
    }
}
