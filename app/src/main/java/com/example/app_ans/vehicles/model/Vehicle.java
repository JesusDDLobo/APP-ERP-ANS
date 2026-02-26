package com.example.app_ans.vehicles.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Vehicle {
    private int id;
    private String plate;
    private String brand;
    private String model;
    private int year;
    private double odometer;
    private VehicleType type;
    private VehicleProvider provider;
    private VehicleState state;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("guardian_device_id")
    private String guardianDeviceId;

    private List<Document> documents;

    @SerializedName("imageDocuments")
    private List<Document> imageDocuments;

    @SerializedName("activeImage")
    private ActiveImage activeImage;

    @SerializedName("maintenance_status")
    private MaintenanceStatus maintenanceStatus;

    private List<Maintenance> maintenances;

    // Getters
    public int getId() { return id; }
    public String getPlate() { return plate; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public int getYear() { return year; }
    public double getOdometer() { return odometer; }
    public VehicleType getType() { return type; }
    public VehicleProvider getProvider() { return provider; }
    public VehicleState getState() { return state; }
    public String getCreatedAt() { return createdAt; }
    public String getGuardianDeviceId() { return guardianDeviceId; }
    public List<Document> getDocuments() { return documents; }
    public List<Document> getImageDocuments() { return imageDocuments; }
    public ActiveImage getActiveImage() { return activeImage; }
    public MaintenanceStatus getMaintenanceStatus() { return maintenanceStatus; }
    public List<Maintenance> getMaintenances() { return maintenances; }

    public static class MaintenanceStatus {
        @SerializedName("is_due")
        private boolean isDue;
        @SerializedName("can_fill")
        private boolean canFill;
        @SerializedName("next_due_kms")
        private double nextDueKms;
        @SerializedName("remaining_kms")
        private double remainingKms;
        @SerializedName("last_maintenance_kms")
        private double lastMaintenanceKms;
        @SerializedName("interval_kms")
        private int intervalKms;
        @SerializedName("pre_alert_kms")
        private int preAlertKms;

        public boolean isDue() { return isDue; }
        public boolean isCanFill() { return canFill; }
        public double getNextDueKms() { return nextDueKms; }
        public double getRemainingKms() { return remainingKms; }
        public double getLastMaintenanceKms() { return lastMaintenanceKms; }
        public int getIntervalKms() { return intervalKms; }
        public int getPreAlertKms() { return preAlertKms; }
    }

    public static class VehicleState {
        private int id;
        private String name;
        public int getId() { return id; }
        public String getName() { return name; }
    }

    public static class VehicleType {
        private int id;
        private String name;
        public int getId() { return id; }
        public String getName() { return name; }
    }

    public static class VehicleProvider {
        private int id;
        private String name;
        public int getId() { return id; }
        public String getName() { return name; }
    }

    public static class Document {
        private int id;
        private String name;
        @SerializedName("file_path")
        private String filePath;
        @SerializedName("previewUrl")
        private String previewUrl;
        @SerializedName("drive_web_view_link")
        private String driveWebViewLink;

        public int getId() { return id; }
        public String getName() { return name; }
        public String getFilePath() { return filePath; }
        public String getPreviewUrl() { return previewUrl; }
        public String getDriveWebViewLink() { return driveWebViewLink; }
    }

    public static class ActiveImage {
        @SerializedName("pivotId")
        private Integer pivotId;
        @SerializedName("documentId")
        private Integer documentId;
        @SerializedName("previewUrl")
        private String previewUrl;
        public Integer getPivotId() { return pivotId; }
        public Integer getDocumentId() { return documentId; }
        public String getPreviewUrl() { return previewUrl; }
    }

    public static class Maintenance {
        private int id;
        private String type;
        @SerializedName("created_at")
        private String createdAt;
        public int getId() { return id; }
        public String getType() { return type; }
        public String getCreatedAt() { return createdAt; }
    }
}
