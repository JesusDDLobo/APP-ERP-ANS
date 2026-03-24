package com.example.app_ans.tasks.model;

import java.util.List;

public class RenditionTaskGroup {

    private final int taskId;
    private final String taskCode;
    private final String taskTitle;
    private final String totalAmount;
    private final int renditionCount;
    private final List<Rendition> renditions;

    private boolean expanded;

    public RenditionTaskGroup(int taskId,
                              String taskCode,
                              String taskTitle,
                              String totalAmount,
                              int renditionCount,
                              List<Rendition> renditions,
                              boolean expanded) {
        this.taskId = taskId;
        this.taskCode = taskCode;
        this.taskTitle = taskTitle;
        this.totalAmount = totalAmount;
        this.renditionCount = renditionCount;
        this.renditions = renditions;
        this.expanded = expanded;
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

    public String getTotalAmount() {
        return totalAmount;
    }

    public int getRenditionCount() {
        return renditionCount;
    }

    public List<Rendition> getRenditions() {
        return renditions;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }
}