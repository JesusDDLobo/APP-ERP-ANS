package com.example.app_ans.tasks.model;

import com.google.gson.annotations.SerializedName;

/** Represents a question that must be answered to complete a task. */
public class TaskQuestion {
    private int id;
    
    @SerializedName("text")
    private String text;

    @SerializedName("question")
    private String question;

    @SerializedName("type")
    private String type; // "text" or "file"

    @SerializedName("required")
    private boolean required;

    public int getId() { return id; }
    public String getText() { 
        return text != null ? text : question; 
    }
    public String getType() { return type; }
    public boolean isRequired() { return required; }
}
