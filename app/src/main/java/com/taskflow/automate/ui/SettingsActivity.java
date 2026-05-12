package com.taskflow.automate.ui;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.taskflow.automate.R;
import com.taskflow.automate.util.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    private PreferenceManager preferenceManager;
    private LinearLayout containerAppToggles;
    private Spinner spinnerHighPriority;
    private Spinner spinnerMediumPriority;
    private Spinner spinnerLowPriority;
    private Spinner spinnerThemeMode;

    private static final String[] THEME_LABELS = {"Light", "Dark", "System Default"};
    private static final String[] THEME_VALUES = {"light", "dark", "system"};

    private static final String[] INTERVAL_LABELS = {
            "15 minutes", "30 minutes", "1 hour", "2 hours", "4 hours"
    };
    private static final int[] INTERVAL_VALUES = {15, 30, 60, 120, 240};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferenceManager = new PreferenceManager(this);

        setupToolbar();
        setupThemeMode();
        setupAppToggles();
        setupReminderIntervals();
        setupEmailDigest();
        setupWhatsAppSettings();
        setupWhatsAppMonitor();
        setupTaskActionButton();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_settings);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupThemeMode() {
        spinnerThemeMode = findViewById(R.id.spinner_theme_mode);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, THEME_LABELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerThemeMode.setAdapter(adapter);

        String currentMode = preferenceManager.getThemeMode();
        int selectedIndex = 2; // default system
        for (int i = 0; i < THEME_VALUES.length; i++) {
            if (THEME_VALUES[i].equals(currentMode)) {
                selectedIndex = i;
                break;
            }
        }
        spinnerThemeMode.setSelection(selectedIndex);

        spinnerThemeMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean isInitialSelection = true;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isInitialSelection) {
                    isInitialSelection = false;
                    return;
                }
                String mode = THEME_VALUES[position];
                preferenceManager.setThemeMode(mode);
                switch (mode) {
                    case "light":
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        break;
                    case "dark":
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        break;
                    default:
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });
    }

    private void setupAppToggles() {
        containerAppToggles = findViewById(R.id.container_app_toggles);
        Set<String> blockedApps = preferenceManager.getBlockedApps();

        PackageManager pm = getPackageManager();
        List<ApplicationInfo> allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        // Filter to all user-visible apps (launcher intent), user-installed, or updated system apps
        List<ApplicationInfo> relevantApps = new ArrayList<>();
        for (ApplicationInfo appInfo : allApps) {
            // Skip our own app
            if ("com.taskflow.automate".equals(appInfo.packageName)) {
                continue;
            }

            boolean isUserApp = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0;
            boolean isUpdatedSystemApp = (appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
            boolean hasLauncher = pm.getLaunchIntentForPackage(appInfo.packageName) != null;

            if (hasLauncher || isUserApp || isUpdatedSystemApp) {
                relevantApps.add(appInfo);
            }
        }

        // Sort alphabetically by app label
        Collections.sort(relevantApps, (a, b) -> {
            String labelA = pm.getApplicationLabel(a).toString();
            String labelB = pm.getApplicationLabel(b).toString();
            return labelA.compareToIgnoreCase(labelB);
        });

        if (relevantApps.isEmpty()) {
            TextView noApps = new TextView(this);
            noApps.setText("No apps found.");
            noApps.setTextColor(getResources().getColor(R.color.textSecondary, getTheme()));
            containerAppToggles.addView(noApps);
            return;
        }

        for (ApplicationInfo appInfo : relevantApps) {
            String packageName = appInfo.packageName;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 16, 0, 16);

            TextView label = new TextView(this);
            label.setText(pm.getApplicationLabel(appInfo).toString());
            label.setTextSize(16);
            label.setTextColor(getResources().getColor(R.color.textPrimary, getTheme()));
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            label.setLayoutParams(labelParams);

            SwitchCompat toggle = new SwitchCompat(this);
            toggle.setChecked(!blockedApps.contains(packageName));
            toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Set<String> currentBlocked = preferenceManager.getBlockedApps();
                if (isChecked) {
                    currentBlocked.remove(packageName);
                } else {
                    currentBlocked.add(packageName);
                }
                preferenceManager.setBlockedApps(currentBlocked);
            });

            row.addView(label);
            row.addView(toggle);
            containerAppToggles.addView(row);
        }
    }

    private void setupReminderIntervals() {
        spinnerHighPriority = findViewById(R.id.spinner_high_priority);
        spinnerMediumPriority = findViewById(R.id.spinner_medium_priority);
        spinnerLowPriority = findViewById(R.id.spinner_low_priority);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, INTERVAL_LABELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerHighPriority.setAdapter(adapter);
        spinnerMediumPriority.setAdapter(adapter);
        spinnerLowPriority.setAdapter(adapter);

        // Set saved selections
        spinnerHighPriority.setSelection(getIntervalIndex(preferenceManager.getReminderInterval(1)));
        spinnerMediumPriority.setSelection(getIntervalIndex(preferenceManager.getReminderInterval(2)));
        spinnerLowPriority.setSelection(getIntervalIndex(preferenceManager.getReminderInterval(3)));

        // Set listeners
        spinnerHighPriority.setOnItemSelectedListener(new IntervalSelectionListener(1));
        spinnerMediumPriority.setOnItemSelectedListener(new IntervalSelectionListener(2));
        spinnerLowPriority.setOnItemSelectedListener(new IntervalSelectionListener(3));
    }

    private int getIntervalIndex(int minutes) {
        for (int i = 0; i < INTERVAL_VALUES.length; i++) {
            if (INTERVAL_VALUES[i] == minutes) {
                return i;
            }
        }
        return 1; // Default to 30 minutes
    }

    private class IntervalSelectionListener implements AdapterView.OnItemSelectedListener {
        private final int priority;
        private boolean isInitialSelection = true;

        IntervalSelectionListener(int priority) {
            this.priority = priority;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (isInitialSelection) {
                isInitialSelection = false;
                return;
            }
            preferenceManager.setReminderInterval(priority, INTERVAL_VALUES[position]);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // No action needed
        }
    }

    private void setupEmailDigest() {
        MaterialCardView cardEmailDigest = findViewById(R.id.card_email_digest_settings);
        if (cardEmailDigest != null) {
            cardEmailDigest.setOnClickListener(v -> {
                startActivity(new Intent(this, EmailConfigActivity.class));
            });
        }
    }

    private void setupWhatsAppSettings() {
        TextInputLayout whatsappNameLayout = findViewById(R.id.layout_whatsapp_self_name);
        TextInputEditText whatsappNameInput = findViewById(R.id.input_whatsapp_self_name);

        if (whatsappNameLayout == null || whatsappNameInput == null) {
            return;
        }

        String savedName = preferenceManager.getWhatsAppSelfName();
        if (savedName != null) {
            whatsappNameInput.setText(savedName);
        }

        whatsappNameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String name = s.toString().trim();
                if (!name.isEmpty()) {
                    preferenceManager.setWhatsAppSelfName(name);
                } else {
                    preferenceManager.setWhatsAppSelfName(null);
                }
            }
        });
    }

    private void setupTaskActionButton() {
        SwitchCompat toggleTaskAction = findViewById(R.id.switch_task_action_button);
        if (toggleTaskAction == null) {
            return;
        }

        toggleTaskAction.setChecked(preferenceManager.isTaskActionButtonEnabled());
        toggleTaskAction.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferenceManager.setTaskActionButtonEnabled(isChecked);
        });
    }

    private void setupWhatsAppMonitor() {
        SwitchCompat switchMonitor = findViewById(R.id.switch_whatsapp_monitor);
        TextInputLayout layoutMonitoredChat = findViewById(R.id.layout_whatsapp_monitored_chat);
        TextInputEditText inputMonitoredChat = findViewById(R.id.input_whatsapp_monitored_chat);

        if (switchMonitor == null || layoutMonitoredChat == null || inputMonitoredChat == null) {
            return;
        }

        boolean enabled = preferenceManager.isWhatsAppMonitorEnabled();
        switchMonitor.setChecked(enabled);
        layoutMonitoredChat.setVisibility(enabled ? View.VISIBLE : View.GONE);

        String savedChat = preferenceManager.getWhatsAppMonitoredChat();
        if (savedChat != null) {
            inputMonitoredChat.setText(savedChat);
        }

        switchMonitor.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferenceManager.setWhatsAppMonitorEnabled(isChecked);
            layoutMonitoredChat.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        inputMonitoredChat.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String name = s.toString().trim();
                if (!name.isEmpty()) {
                    preferenceManager.setWhatsAppMonitoredChat(name);
                } else {
                    preferenceManager.setWhatsAppMonitoredChat(null);
                }
            }
        });
    }
}
