package com.example.app_ans.tasks.model;

public class Rendition {

    private final int id;
    private final int taskId;
    private final String taskCode;
    private final String taskTitle;
    private final String date;
    private final String amount;
    private final String status;

    public Rendition(int id,
                     int taskId,
                     String taskCode,
                     String taskTitle,
                     String date,
                     String amount,
                     String status) {
        this.id = id;
        this.taskId = taskId;
        this.taskCode = taskCode;
        this.taskTitle = taskTitle;
        this.date = date;
        this.amount = amount;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public int getTaskId() {
        return taskId;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public String getDate() {
        return date;
    }

    public String getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }
}