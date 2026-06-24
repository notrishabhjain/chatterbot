package com.taskflow.automate.util;

import com.taskflow.automate.model.Task;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ExportManagerTest {

    private ExportManager exportManager;

    @Before
    public void setUp() {
        exportManager = new ExportManager();
    }

    @Test
    public void jsonExport_singleTask_containsTitle() {
        Task task = new Task();
        task.setTitle("Test Task");
        task.setStatus("pending");
        task.setPriority(2);

        String json = exportManager.exportToJson(Arrays.asList(task));
        assertTrue(json.contains("Test Task"));
    }

    @Test
    public void jsonExport_containsValidJsonStructure() {
        Task task = new Task();
        task.setTitle("Sample");
        task.setStatus("pending");

        String json = exportManager.exportToJson(Arrays.asList(task));
        assertTrue(json.trim().startsWith("["));
        assertTrue(json.trim().endsWith("]"));
    }

    @Test
    public void jsonExport_emptyList_returnsEmptyArray() {
        String json = exportManager.exportToJson(new ArrayList<Task>());
        assertTrue(json.trim().startsWith("["));
        assertTrue(json.trim().endsWith("]"));
        // Should be empty array: either "[]" or "[\n]" depending on formatting
        assertFalse(json.contains("title"));
    }

    @Test
    public void csvExport_hasHeaderRow() {
        String csv = exportManager.exportToCsv(new ArrayList<Task>());
        String firstLine = csv.split("\n")[0];
        assertTrue(firstLine.contains("ID"));
        assertTrue(firstLine.contains("Title"));
        assertTrue(firstLine.contains("Description"));
        assertTrue(firstLine.contains("Priority"));
        assertTrue(firstLine.contains("Status"));
    }

    @Test
    public void csvExport_singleTask_hasTwoLines() {
        Task task = new Task();
        task.setTitle("My Task");
        task.setStatus("pending");
        task.setPriority(2);

        String csv = exportManager.exportToCsv(Arrays.asList(task));
        String[] lines = csv.split("\n");
        assertEquals(2, lines.length);
    }

    @Test
    public void csvExport_emptyList_hasOnlyHeader() {
        String csv = exportManager.exportToCsv(new ArrayList<Task>());
        String[] lines = csv.split("\n");
        assertEquals(1, lines.length);
    }

    @Test
    public void csvExport_specialCharacters_escaped() {
        Task task = new Task();
        task.setTitle("Task with, comma");
        task.setStatus("pending");
        task.setPriority(3);

        String csv = exportManager.exportToCsv(Arrays.asList(task));
        // Title with comma should be quoted
        assertTrue(csv.contains("\"Task with, comma\""));
    }

    @Test
    public void csvExport_nullFields_handledGracefully() {
        Task task = new Task();
        task.setTitle("Minimal Task");
        task.setStatus("pending");
        task.setPriority(3);
        // description, sourceApp, assignee, dueDate, completedAt, recurrenceRule are all null

        String csv = exportManager.exportToCsv(Arrays.asList(task));
        assertNotNull(csv);
        // Should not throw NPE - just have empty fields
        String[] lines = csv.split("\n");
        assertEquals(2, lines.length);
    }

    @Test
    public void jsonExport_multipleTasksContainsAll() {
        Task task1 = new Task();
        task1.setTitle("First Task");
        task1.setStatus("pending");

        Task task2 = new Task();
        task2.setTitle("Second Task");
        task2.setStatus("completed");

        String json = exportManager.exportToJson(Arrays.asList(task1, task2));
        assertTrue(json.contains("First Task"));
        assertTrue(json.contains("Second Task"));
    }

    @Test
    public void csvExport_multipleTasks_correctLineCount() {
        Task task1 = new Task();
        task1.setTitle("Task A");
        task1.setStatus("pending");
        task1.setPriority(1);

        Task task2 = new Task();
        task2.setTitle("Task B");
        task2.setStatus("completed");
        task2.setPriority(2);

        String csv = exportManager.exportToCsv(Arrays.asList(task1, task2));
        String[] lines = csv.split("\n");
        assertEquals(3, lines.length); // header + 2 data rows
    }
}
