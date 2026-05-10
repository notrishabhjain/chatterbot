package com.taskflow.automate.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tags")
public class Tag {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "color")
    private String color;

    public Tag() {
    }

    // Getters
    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    // Setters
    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
