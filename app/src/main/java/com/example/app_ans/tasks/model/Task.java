package com.example.app_ans.tasks.model;

import com.example.app_ans.vehicles.model.Vehicle;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

/** Represents a Task (Ticket) assigned to a user. */
public class Task implements Serializable {
    private int id;

    @SerializedName("public_id")
    private String publicId;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("created_at_formatted")
    private String createdAtFormatted;

    @SerializedName("content")
    private TaskContent content;

    @SerializedName("state")
    private TaskState state;

    @SerializedName("assigned_users")
    private List<AssignedUser> assignedUsers;

    @SerializedName("technicians")
    private List<AssignedUser> technicians;

    @SerializedName("advances")
    private List<TaskAdvance> advances;

    @SerializedName("previous_ticket")
    private PreviousTask previousTicket;

    @SerializedName("questions")
    private List<TaskQuestion> questions;

    @SerializedName("advances_count")
    private int advancesCount;

    @SerializedName("time_logs")
    private List<TaskTimeLog> timeLogs;

    @SerializedName("is_running")
    private boolean isRunning;

    @SerializedName("total_time_spent")
    private String totalTimeSpent;

    @SerializedName("vehicle_plate")
    private String vehiclePlate;

    @SerializedName("vehicle")
    private Vehicle vehicle;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("type")
    private String type;

    @SerializedName("start_time")
    private String startTime;

    @SerializedName("end_time")
    private String endTime;

    @SerializedName("latitude")
    private Double latitude;

    @SerializedName("longitude")
    private Double longitude;

    @SerializedName("sub_work_order_id")
    private Integer subWorkOrderId;

    @SerializedName("state_id")
    private Integer stateId;

    @SerializedName("process_id")
    private Integer processId;

    public int getId() {
        return id;
    }

    public String getPublicId() {
        return publicId;
    }

    public TaskState getState() {
        return state;
    }

    public String getStatus() {
        return state != null ? state.getValue() : "Desconocido";
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getCreatedAtFormatted() {
        return createdAtFormatted;
    }

    public TaskContent getContent() {
        return content;
    }

    public String getName() {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        return content != null ? content.getName() : "";
    }

    public String getDescription() {
        if (description != null && !description.trim().isEmpty()) {
            return description;
        }
        return content != null ? content.getDescription() : "";
    }

    public String getType() {
        if (type != null && !type.trim().isEmpty()) {
            return type;
        }
        return content != null ? content.getType() : null;
    }

    public String getStartTime() {
        if (startTime != null && !startTime.trim().isEmpty()) {
            return startTime;
        }
        return content != null ? content.getStartTime() : null;
    }

    public String getEndTime() {
        if (endTime != null && !endTime.trim().isEmpty()) {
            return endTime;
        }
        return content != null ? content.getEndTime() : null;
    }

    public Integer getVehicleId() {
        if (content != null) {
            return content.getVehicleId();
        }
        return null;
    }

    public Double getLatitude() {
        if (latitude != null) {
            return latitude;
        }
        return content != null ? content.getLatitude() : null;
    }

    public Double getLongitude() {
        if (longitude != null) {
            return longitude;
        }
        return content != null ? content.getLongitude() : null;
    }

    public Integer getSubWorkOrderId() {
        if (subWorkOrderId != null) {
            return subWorkOrderId;
        }
        return content != null ? content.getSubWorkOrderId() : null;
    }

    public Integer getStateId() {
        return stateId;
    }

    public Integer getProcessId() {
        return processId;
    }

    public List<AssignedUser> getAssignedUsers() {
        return assignedUsers != null ? assignedUsers : technicians;
    }

    public List<TaskAdvance> getAdvances() {
        return advances;
    }

    public PreviousTask getPreviousTicket() {
        return previousTicket;
    }

    public List<TaskQuestion> getQuestions() {
        return questions;
    }

    public int getAdvancesCount() {
        return advancesCount;
    }

    public List<TaskTimeLog> getTimeLogs() {
        return timeLogs;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        this.isRunning = running;
    }

    public String getTotalTimeSpent() {
        return totalTimeSpent;
    }

    public void setTotalTimeSpent(String totalTimeSpent) {
        this.totalTimeSpent = totalTimeSpent;
    }

    public String getVehiclePlate() {
        return vehiclePlate;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public static class TaskTimeLog implements Serializable {
        private int id;

        @SerializedName("start_time")
        private String startTime;

        @SerializedName("end_time")
        private String endTime;

        private String duration;

        private TimeLogUser user;

        public int getId() {
            return id;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public String getDuration() {
            return duration;
        }

        public TimeLogUser getUser() {
            return user;
        }
    }

    public static class TimeLogUser implements Serializable {
        private int id;
        private String name;

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    public static class PreviousTask implements Serializable {
        private int id;

        @SerializedName("public_id")
        private String publicId;

        public int getId() {
            return id;
        }

        public String getPublicId() {
            return publicId;
        }
    }

    public static class TaskState implements Serializable {
        private int id;
        private String value;

        public int getId() {
            return id;
        }

        public String getValue() {
            return value;
        }
    }

    public static class TaskContent implements Serializable {
        private String name;
        private String description;
        private String type;

        @SerializedName("start_time")
        private String startTime;

        @SerializedName("end_time")
        private String endTime;

        @SerializedName("vehicle_id")
        private Integer vehicleId;

        private Double latitude;
        private Double longitude;

        @SerializedName("sub_work_order_id")
        private Integer subWorkOrderId;

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getType() {
            return type;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public Integer getVehicleId() {
            return vehicleId;
        }

        public Double getLatitude() {
            return latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public Integer getSubWorkOrderId() {
            return subWorkOrderId;
        }
    }
}