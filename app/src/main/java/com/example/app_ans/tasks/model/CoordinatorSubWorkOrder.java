package com.example.app_ans.tasks.model;

import com.google.gson.annotations.SerializedName;

public class CoordinatorSubWorkOrder {

    private int id;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("public_id")
    private String publicId;

    @SerializedName("work_order_public_id")
    private String workOrderPublicId;

    private String name;

    @SerializedName("coordinator_name")
    private String coordinatorName;

    private String status;

    public int getId() {
        return id;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getPublicId() {
        return publicId;
    }

    public String getWorkOrderPublicId() {
        return workOrderPublicId;
    }

    public String getName() {
        return name;
    }

    public String getCoordinatorName() {
        return coordinatorName;
    }

    public String getStatus() {
        return status;
    }
}