package com.example.app_ans.tasks.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SubWorkOrderTasksResponse {

    @SerializedName("sub_work_order")
    private CoordinatorSubWorkOrder subWorkOrder;

    @SerializedName("tasks")
    private List<Task> tasks;

    public CoordinatorSubWorkOrder getSubWorkOrder() {
        return subWorkOrder;
    }

    public List<Task> getTasks() {
        return tasks;
    }
}