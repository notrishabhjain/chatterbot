package com.taskflow.automate.model;

public enum TaskType {
    MEETING,
    DEADLINE,
    FOLLOW_UP,
    REQUEST,
    APPROVAL,
    REMINDER,
    GENERAL;

    public static TaskType fromString(String value) {
        if (value == null || value.isEmpty()) {
            return GENERAL;
        }
        try {
            return TaskType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return GENERAL;
        }
    }
}
