package com.example.app_ans.tasks.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TaskAdvance implements Serializable {
    private int id;

    @SerializedName("content")
    private String content;

    @SerializedName("comment")
    private String comment;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("created_at_date")
    private String createdAtDate;

    @SerializedName("created_at_time")
    private String createdAtTime;

    @SerializedName("created_at_formatted")
    private String createdAtFormatted;

    @SerializedName("user")
    private AssignedUser user;

    @SerializedName("comments")
    private List<Comment> comments;

    @SerializedName("documents")
    private List<AdvanceFile> documents;

    @SerializedName("files")
    private List<AdvanceFile> files;

    @SerializedName("advance_files")
    private List<AdvanceFile> advanceFiles;

    @SerializedName("media")
    private List<AdvanceFile> media;

    public int getId() {
        return id;
    }

    /**
     * Compatibilidad con estructuras viejas.
     */
    public String getContent() {
        return content;
    }

    /**
     * Nuevo campo principal recomendado por backend.
     * Si no viene, usa content para no romper compatibilidad.
     */
    public String getComment() {
        if (comment != null && !comment.trim().isEmpty()) {
            return comment;
        }
        return content;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getCreatedAtDate() {
        return createdAtDate;
    }

    public String getCreatedAtTime() {
        return createdAtTime;
    }

    public String getCreatedAtFormatted() {
        return createdAtFormatted;
    }

    public AssignedUser getUser() {
        return user;
    }

    public List<Comment> getComments() {
        return comments != null ? comments : new ArrayList<>();
    }

    public List<AdvanceFile> getFiles() {
        List<AdvanceFile> allFiles = new ArrayList<>();
        if (documents != null) allFiles.addAll(documents);
        if (files != null) allFiles.addAll(files);
        if (advanceFiles != null) allFiles.addAll(advanceFiles);
        if (media != null) allFiles.addAll(media);
        return allFiles;
    }

    public static class Comment implements Serializable {
        private int id;

        @SerializedName("comment")
        private String comment;

        @SerializedName("created_at")
        private String createdAt;

        @SerializedName("created_at_date")
        private String createdAtDate;

        @SerializedName("created_at_time")
        private String createdAtTime;

        @SerializedName("created_at_formatted")
        private String createdAtFormatted;

        @SerializedName("user")
        private AssignedUser user;

        public int getId() {
            return id;
        }

        public String getComment() {
            return comment;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public String getCreatedAtDate() {
            return createdAtDate;
        }

        public String getCreatedAtTime() {
            return createdAtTime;
        }

        public String getCreatedAtFormatted() {
            return createdAtFormatted;
        }

        public AssignedUser getUser() {
            return user;
        }
    }

    public static class AdvanceFile implements Serializable {
        private int id;

        @SerializedName("url")
        private String url;

        @SerializedName("path")
        private String path;

        @SerializedName("name")
        private String name;

        @SerializedName("mime_type")
        private String mimeType;

        @SerializedName("drive_web_view_link")
        private String driveWebViewLink;

        @SerializedName("drive_thumbnail_link")
        private String driveThumbnailLink;

        public int getId() {
            return id;
        }

        public String getUrl() {
            if (driveWebViewLink != null && !driveWebViewLink.trim().isEmpty()) {
                return driveWebViewLink;
            }
            return url != null ? url : path;
        }

        public String getThumbnailUrl() {
            return driveThumbnailLink;
        }

        public String getName() {
            return name;
        }

        public String getMimeType() {
            return mimeType;
        }
    }
}