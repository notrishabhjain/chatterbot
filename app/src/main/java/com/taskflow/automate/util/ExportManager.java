package com.taskflow.automate.util;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.taskflow.automate.model.Task;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ExportManager {

    public String exportToJson(List<Task> tasks) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(tasks);
    }

    public String exportToCsv(List<Task> tasks) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Title,Description,Priority,Status,Source App,Due Date,Created At,Completed At,Assignee,Recurrence\n");
        for (Task task : tasks) {
            sb.append(task.getId()).append(",");
            sb.append(escapeCsv(task.getTitle())).append(",");
            sb.append(escapeCsv(task.getDescription())).append(",");
            sb.append(task.getPriorityLabel()).append(",");
            sb.append(task.getStatus()).append(",");
            sb.append(escapeCsv(task.getSourceApp())).append(",");
            sb.append(task.getDueDate() != null ? task.getDueDate() : "").append(",");
            sb.append(task.getCreatedAt()).append(",");
            sb.append(task.getCompletedAt() != null ? task.getCompletedAt() : "").append(",");
            sb.append(escapeCsv(task.getAssignee())).append(",");
            sb.append(task.getRecurrenceRule() != null ? task.getRecurrenceRule() : "").append("\n");
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public File writeToFile(Context context, String content, String filename) throws IOException {
        File dir = new File(context.getExternalFilesDir(null), "exports");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, filename);
        FileWriter writer = new FileWriter(file);
        writer.write(content);
        writer.close();
        return file;
    }
}
