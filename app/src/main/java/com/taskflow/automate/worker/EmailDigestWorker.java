package com.taskflow.automate.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.taskflow.automate.database.AppDatabase;
import com.taskflow.automate.model.Task;
import com.taskflow.automate.util.PreferenceManager;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailDigestWorker extends Worker {

    public EmailDigestWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        PreferenceManager prefs = new PreferenceManager(context);

        String host = prefs.getSmtpHost();
        String port = prefs.getSmtpPort();
        String username = prefs.getEmailUsername();
        String password = prefs.getEmailPassword();
        String recipient = prefs.getEmailRecipient();

        if (host == null || host.isEmpty() || recipient == null || recipient.isEmpty()) {
            return Result.failure();
        }

        try {
            List<Task> pendingTasks = AppDatabase.getInstance(context).taskDao().getPendingTasks();
            String htmlBody = buildEmailBody(pendingTasks);

            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port != null ? port : "587");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");

            Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                @Override
                protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                    return new javax.mail.PasswordAuthentication(username, password);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject("TaskFlow Digest - Pending Tasks");
            message.setContent(htmlBody, "text/html; charset=utf-8");

            Transport.send(message);
            return Result.success();
        } catch (Exception e) {
            return Result.failure();
        }
    }

    private String buildEmailBody(List<Task> tasks) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h2>TaskFlow - Task Digest</h2>");

        // Group by priority
        sb.append("<h3 style='color:#F44336;'>High Priority</h3><ul>");
        for (Task task : tasks) {
            if (task.getPriority() == 1) {
                sb.append("<li>").append(escapeHtml(task.getTitle()));
                if (task.getDescription() != null && !task.getDescription().isEmpty()) {
                    sb.append(" - ").append(escapeHtml(task.getDescription()));
                }
                sb.append("</li>");
            }
        }
        sb.append("</ul>");

        sb.append("<h3 style='color:#FF9800;'>Medium Priority</h3><ul>");
        for (Task task : tasks) {
            if (task.getPriority() == 2) {
                sb.append("<li>").append(escapeHtml(task.getTitle()));
                if (task.getDescription() != null && !task.getDescription().isEmpty()) {
                    sb.append(" - ").append(escapeHtml(task.getDescription()));
                }
                sb.append("</li>");
            }
        }
        sb.append("</ul>");

        sb.append("<h3 style='color:#4CAF50;'>Low Priority</h3><ul>");
        for (Task task : tasks) {
            if (task.getPriority() == 3) {
                sb.append("<li>").append(escapeHtml(task.getTitle()));
                if (task.getDescription() != null && !task.getDescription().isEmpty()) {
                    sb.append(" - ").append(escapeHtml(task.getDescription()));
                }
                sb.append("</li>");
            }
        }
        sb.append("</ul>");

        sb.append("<p>Total pending tasks: ").append(tasks.size()).append("</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public static void scheduleEmailDigest(Context context, String frequency) {
        long interval = "daily".equalsIgnoreCase(frequency) ? 24 : 7 * 24;
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                EmailDigestWorker.class, interval, TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "email_digest", ExistingPeriodicWorkPolicy.REPLACE, request);
    }
}
