package com.taskflow.automate.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.taskflow.automate.R;
import com.taskflow.automate.util.PreferenceManager;

import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    private PreferenceManager preferenceManager;
    private LinearLayout containerAppToggles;
    private Spinner spinnerHighPriority;
    private Spinner spinnerMediumPriority;
    private Spinner spinnerLowPriority;

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
        setupAppToggles();
        setupReminderIntervals();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_settings);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupAppToggles() {
        containerAppToggles = findViewById(R.id.container_app_toggles);
        Set<String> knownApps = preferenceManager.getKnownApps();
        Set<String> blockedApps = preferenceManager.getBlockedApps();

        if (knownApps.isEmpty()) {
            TextView noApps = new TextView(this);
            noApps.setText("No notification sources detected yet.");
            noApps.setTextColor(getResources().getColor(R.color.textSecondary, getTheme()));
            containerAppToggles.addView(noApps);
            return;
        }

        for (String packageName : knownApps) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 16, 0, 16);

            TextView label = new TextView(this);
            label.setText(getAppLabel(packageName));
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

    private String getAppLabel(String packageName) {
        try {
            return getPackageManager()
                    .getApplicationLabel(
                            getPackageManager().getApplicationInfo(packageName, 0))
                    .toString();
        } catch (Exception e) {
            return packageName;
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
}
