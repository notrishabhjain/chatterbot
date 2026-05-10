package com.taskflow.automate.ui;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.taskflow.automate.R;
import com.taskflow.automate.service.NotificationCaptureService;
import com.taskflow.automate.ui.fragment.ImportFragment;
import com.taskflow.automate.ui.fragment.MoreFragment;
import com.taskflow.automate.ui.fragment.TasksFragment;
import com.taskflow.automate.ui.fragment.TeamFragment;
import com.taskflow.automate.ui.fragment.TodayFragment;

public class MainActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNavigation;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                // Permission result handled; no further action needed
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupToolbar();
        setupBottomNavigation();
        checkNotificationListenerPermission();
        requestPostNotificationPermission();

        if (savedInstanceState == null) {
            loadFragment(new TodayFragment());
            bottomNavigation.setSelectedItemId(R.id.nav_today);
        }
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_today) {
                selectedFragment = new TodayFragment();
                toolbar.setTitle(R.string.tab_today);
            } else if (itemId == R.id.nav_tasks) {
                selectedFragment = new TasksFragment();
                toolbar.setTitle(R.string.tab_tasks);
            } else if (itemId == R.id.nav_import) {
                selectedFragment = new ImportFragment();
                toolbar.setTitle(R.string.tab_import);
            } else if (itemId == R.id.nav_team) {
                selectedFragment = new TeamFragment();
                toolbar.setTitle(R.string.tab_team);
            } else if (itemId == R.id.nav_more) {
                selectedFragment = new MoreFragment();
                toolbar.setTitle(R.string.tab_more);
            } else {
                return false;
            }

            loadFragment(selectedFragment);
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void checkNotificationListenerPermission() {
        if (!isNotificationListenerEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.notification_permission_title)
                    .setMessage(R.string.notification_permission_message)
                    .setPositiveButton(R.string.enable, (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                        startActivity(intent);
                    })
                    .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                    .setCancelable(true)
                    .show();
        }
    }

    private boolean isNotificationListenerEnabled() {
        ComponentName componentName = new ComponentName(this, NotificationCaptureService.class);
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(componentName.flattenToString());
    }

    private void requestPostNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}
