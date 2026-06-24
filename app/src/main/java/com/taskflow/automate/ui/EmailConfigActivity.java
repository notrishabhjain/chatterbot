package com.taskflow.automate.ui;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.taskflow.automate.R;
import com.taskflow.automate.util.PreferenceManager;
import com.taskflow.automate.worker.EmailDigestWorker;

public class EmailConfigActivity extends AppCompatActivity {

    private TextInputEditText editSmtpHost;
    private TextInputEditText editSmtpPort;
    private TextInputEditText editUsername;
    private TextInputEditText editPassword;
    private TextInputEditText editRecipient;
    private Spinner spinnerFrequency;
    private PreferenceManager preferenceManager;

    private static final String[] FREQUENCY_OPTIONS = {"Daily", "Weekly"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_config);

        preferenceManager = new PreferenceManager(this);

        setupToolbar();
        initViews();
        loadSavedConfig();
        setupSaveButton();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_email_config);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        editSmtpHost = findViewById(R.id.edit_smtp_host);
        editSmtpPort = findViewById(R.id.edit_smtp_port);
        editUsername = findViewById(R.id.edit_email_username);
        editPassword = findViewById(R.id.edit_email_password);
        editRecipient = findViewById(R.id.edit_email_recipient);
        spinnerFrequency = findViewById(R.id.spinner_email_frequency);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, FREQUENCY_OPTIONS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFrequency.setAdapter(adapter);
    }

    private void loadSavedConfig() {
        String host = preferenceManager.getSmtpHost();
        if (host != null) editSmtpHost.setText(host);

        String port = preferenceManager.getSmtpPort();
        if (port != null) editSmtpPort.setText(port);

        String username = preferenceManager.getEmailUsername();
        if (username != null) editUsername.setText(username);

        String password = preferenceManager.getEmailPassword();
        if (password != null) editPassword.setText(password);

        String recipient = preferenceManager.getEmailRecipient();
        if (recipient != null) editRecipient.setText(recipient);

        String frequency = preferenceManager.getEmailFrequency();
        if ("weekly".equalsIgnoreCase(frequency)) {
            spinnerFrequency.setSelection(1);
        } else {
            spinnerFrequency.setSelection(0);
        }
    }

    private void setupSaveButton() {
        MaterialButton btnSave = findViewById(R.id.btn_save_email_config);
        btnSave.setOnClickListener(v -> saveConfig());
    }

    private void saveConfig() {
        String host = editSmtpHost.getText() != null ? editSmtpHost.getText().toString().trim() : "";
        String port = editSmtpPort.getText() != null ? editSmtpPort.getText().toString().trim() : "";
        String username = editUsername.getText() != null ? editUsername.getText().toString().trim() : "";
        String password = editPassword.getText() != null ? editPassword.getText().toString().trim() : "";
        String recipient = editRecipient.getText() != null ? editRecipient.getText().toString().trim() : "";
        String frequency = FREQUENCY_OPTIONS[spinnerFrequency.getSelectedItemPosition()];

        if (host.isEmpty() || username.isEmpty() || recipient.isEmpty()) {
            Toast.makeText(this, R.string.email_config_required_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        preferenceManager.setSmtpHost(host);
        preferenceManager.setSmtpPort(port.isEmpty() ? "587" : port);
        preferenceManager.setEmailUsername(username);
        preferenceManager.setEmailPassword(password);
        preferenceManager.setEmailRecipient(recipient);
        preferenceManager.setEmailFrequency(frequency);

        // Schedule the work
        EmailDigestWorker.scheduleEmailDigest(this, frequency);

        Toast.makeText(this, R.string.email_config_saved, Toast.LENGTH_SHORT).show();
        finish();
    }
}
