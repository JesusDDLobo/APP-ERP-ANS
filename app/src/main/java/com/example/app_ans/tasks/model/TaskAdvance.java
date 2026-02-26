package com.example.app_ans.tasks.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class TaskAdvance implements Serializable {
    private int id;
    
    @SerializedName("content")
    private String content;
    
    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("user")
    private AssignedUser user;

    @SerializedName("comments")
    private java.util.List<Comment> comments;

    @SerializedName("documents")
    private java.util.List<AdvanceFile> documents;

    @SerializedName("files")
    private java.util.List<AdvanceFile> files;

    @SerializedName("advance_files")
    private java.util.List<AdvanceFile> advanceFiles;

    @SerializedName("media")
    private java.util.List<AdvanceFile> media;

    public int getId() { return id; }
    public String getContent() { return content; }
    public String getCreatedAt() { return createdAt; }
    public AssignedUser getUser() { return user; }
    public java.util.List<Comment> getComments() { return comments; }
    
    public java.util.List<AdvanceFile> getFiles() { 
        java.util.List<AdvanceFile> allFiles = new java.util.ArrayList<>();
        if (documents != null) allFiles.addAll(documents);
        if (files != null) allFiles.addAll(files);
        if (advanceFiles != null) allFiles.addAll(advanceFiles);
        if (media != null) allFiles.addAll(media);
        return allFiles;
    }

    public static class Comment implements Serializable {
        private int id;
        private String comment;
        @SerializedName("created_at")
        private String createdAt;
        private AssignedUser user;

        public int getId() { return id; }
        public String getComment() { return comment; }
        public String getCreatedAt() { return createdAt; }
        public AssignedUser getUser() { return user; }
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

        public int getId() { return id; }
        public String getUrl() { 
            if (driveWebViewLink != null) return driveWebViewLink;
            return url != null ? url : path; 
        }
        public String getThumbnailUrl() { 
            return driveThumbnailLink; 
        }
        public String getName() { return name; }
        public String getMimeType() { return mimeType; }
    }
}
