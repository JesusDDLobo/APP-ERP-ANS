package com.example.app_ans.tasks.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_ans.R;
import com.example.app_ans.core.persistence.PendingAdvance;
import com.example.app_ans.core.utils.DateUtils;
import com.example.app_ans.tasks.model.TaskAdvance;

import java.util.ArrayList;
import java.util.List;

public class TaskAdvanceAdapter extends RecyclerView.Adapter<TaskAdvanceAdapter.ViewHolder> {
    private List<TaskAdvance> advances = new ArrayList<>();
    private List<TaskAdvance> filteredAdvances = new ArrayList<>();
    private List<PendingAdvance> pendingAdvances = new ArrayList<>();
    private OnAdvanceActionListener listener;
    private int expandedPosition = -1;
    private String currentQuery = "";

    public interface OnAdvanceActionListener {
        void onEdit(TaskAdvance advance);
        void onDelete(TaskAdvance advance);
        void onFileClick(String fileUrl, String mimeType);
    }

    public void setOnAdvanceActionListener(OnAdvanceActionListener listener) {
        this.listener = listener;
    }

    public void setAdvances(List<TaskAdvance> advances) {
        if (advances == null) {
            this.advances = new ArrayList<>();
            this.filteredAdvances = new ArrayList<>();
            notifyDataSetChanged();
            return;
        }

        // Sort server advances descending (newest first)
        List<TaskAdvance> sorted = new ArrayList<>(advances);
        sorted.sort((a, b) -> {
            String dateA = a.getCreatedAt();
            String dateB = b.getCreatedAt();

            if (dateA == null && dateB == null) return 0;
            if (dateA == null) return 1;
            if (dateB == null) return -1;

            try {
                java.time.LocalDateTime dtA = parseDate(dateA);
                java.time.LocalDateTime dtB = parseDate(dateB);
                return dtB.compareTo(dtA); // Descending (newest first)
            } catch (Exception e) {
                return dateB.compareTo(dateA);
            }
        });
        this.advances = sorted;
        applyFilter();
    }

    public void filter(String query) {
        this.currentQuery = query.toLowerCase().trim();
        applyFilter();
    }

    private void applyFilter() {
        if (currentQuery.isEmpty()) {
            filteredAdvances = new ArrayList<>(advances);
        } else {
            filteredAdvances = new ArrayList<>();
            for (TaskAdvance advance : advances) {
                boolean matches = false;
                if (advance.getContent() != null && advance.getContent().toLowerCase().contains(currentQuery)) {
                    matches = true;
                } else if (advance.getUser() != null && advance.getUser().getName().toLowerCase().contains(currentQuery)) {
                    matches = true;
                } else if (advance.getCreatedAt() != null && advance.getCreatedAt().toLowerCase().contains(currentQuery)) {
                    matches = true;
                }

                if (matches) {
                    filteredAdvances.add(advance);
                }
            }
        }
        notifyDataSetChanged();
    }

    private java.time.LocalDateTime parseDate(String dateStr) {
        if (dateStr == null) return java.time.LocalDateTime.MIN;
        String normalized = dateStr.replace(" ", "T");
        if (normalized.contains(".")) {
            normalized = normalized.substring(0, normalized.indexOf("."));
        }

        try {
            // Case 1: YYYY-MM-DD...
            if (normalized.contains("-") && normalized.indexOf("-") == 4) {
                 if (normalized.length() == 16) normalized += ":00";
                 return java.time.LocalDateTime.parse(normalized);
            }
            // Case 2: DD-MM-YYYY...
            String pattern = "dd-MM-yyyy'T'HH:mm";
            if (normalized.length() >= 19) pattern = "dd-MM-yyyy'T'HH:mm:ss";
            else if (normalized.length() == 16) pattern = "dd-MM-yyyy'T'HH:mm";
            else if (normalized.length() == 10) normalized += "T00:00";

            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(pattern);
            return java.time.LocalDateTime.parse(normalized, formatter);
        } catch (Exception e) {
            android.util.Log.e("TaskAdvanceAdapter", "Error parsing date: " + dateStr, e);
            return java.time.LocalDateTime.MIN;
        }
    }

    public void setPendingAdvances(List<PendingAdvance> pendingAdvances) {
        // Sort pending descending (newest first)
        List<PendingAdvance> sorted = new ArrayList<>(pendingAdvances);
        sorted.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        this.pendingAdvances = sorted;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_advance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        boolean isExpanded = position == expandedPosition;
        holder.expandedBody.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.ivArrow.setRotation(isExpanded ? 90 : 0);

        if (position < pendingAdvances.size()) {
            PendingAdvance pending = pendingAdvances.get(position);
            holder.tvAuthor.setText("Sincronizando...");

            if ("ERROR".equals(pending.getStatus())) {
                holder.tvDate.setText("Error");
                holder.tvDate.setTextColor(android.graphics.Color.RED);
                holder.tvTime.setText("Subida");
            } else {
                holder.tvDate.setText("Pendiente");
                holder.tvTime.setText("---");
            }

            holder.tvSnippet.setText(pending.getContent());
            holder.tvDescriptionFull.setText(pending.getContent());
            holder.itemView.setAlpha(0.6f);
            holder.btnDelete.setVisibility(View.GONE);
            holder.imagesContainer.removeAllViews();
            holder.docsLabel.setVisibility(View.GONE);
            holder.commentsLabel.setVisibility(View.GONE);
            holder.commentsContainer.removeAllViews();
            holder.imagesScroll.setVisibility(View.GONE);

            if (pending.getFilePaths() != null && !pending.getFilePaths().isEmpty()) {
                holder.imagesScroll.setVisibility(View.VISIBLE);
                holder.docsLabel.setVisibility(View.VISIBLE);
                for (String path : pending.getFilePaths()) {
                    addFileThumbnail(holder.imagesContainer, path, null);
                }
            }
        } else {
            int advanceIndex = position - pendingAdvances.size();
            TaskAdvance advance = filteredAdvances.get(advanceIndex);

            String authorName = (advance.getUser() != null) ? advance.getUser().getName() : "Técnico";
            holder.tvAuthor.setText(authorName);

            String dateStr = advance.getCreatedAt();
            holder.tvDate.setText(DateUtils.formatDateOnly(dateStr));
            holder.tvTime.setText(DateUtils.formatTimeOnly(dateStr));

            holder.tvSnippet.setText(advance.getContent());
            holder.tvDescriptionFull.setText(advance.getContent());
            holder.itemView.setAlpha(1.0f);

            // Snippet only visible when NOT expanded
            holder.tvSnippet.setVisibility(isExpanded ? View.GONE : View.VISIBLE);

            // Check 10-minute window for delete
            if (isWithinTenMinutes(advance.getCreatedAt())) {
                holder.btnDelete.setVisibility(View.VISIBLE);
                holder.btnDelete.setOnClickListener(v -> { if (listener != null) listener.onDelete(advance); });
            } else {
                holder.btnDelete.setVisibility(View.GONE);
            }

            // Show remote files
            holder.imagesContainer.removeAllViews();
            if (advance.getFiles() != null && !advance.getFiles().isEmpty()) {
                holder.imagesScroll.setVisibility(View.VISIBLE);
                holder.docsLabel.setVisibility(View.VISIBLE);
                for (TaskAdvance.AdvanceFile file : advance.getFiles()) {
                    addFileThumbnail(holder.imagesContainer, file.getThumbnailUrl() != null ? file.getThumbnailUrl() : file.getUrl(), file.getMimeType(), file.getUrl());
                }
            } else {
                holder.imagesScroll.setVisibility(View.GONE);
                holder.docsLabel.setVisibility(View.GONE);
            }

            // Show comments
            holder.commentsContainer.removeAllViews();
            if (advance.getComments() != null && !advance.getComments().isEmpty()) {
                holder.commentsLabel.setVisibility(View.VISIBLE);
                for (TaskAdvance.Comment comment : advance.getComments()) {
                    addCommentView(holder.commentsContainer, comment);
                }
            } else {
                holder.commentsLabel.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;

            int prevExpanded = expandedPosition;
            expandedPosition = (currentPos == expandedPosition) ? -1 : currentPos;

            if (prevExpanded != -1) notifyItemChanged(prevExpanded);
            notifyItemChanged(currentPos);
        });
    }

    private boolean isWithinTenMinutes(String createdAt) {
        if (createdAt == null) return false;
        try {
            // Handle both T and space separators
            String normalized = createdAt.replace(" ", "T");
            if (normalized.contains(".")) {
                normalized = normalized.substring(0, normalized.indexOf("."));
            }
            
            java.time.OffsetDateTime created = java.time.ZonedDateTime.parse(normalized + "Z", 
                java.time.format.DateTimeFormatter.ISO_DATE_TIME).toOffsetDateTime();
            java.time.OffsetDateTime now = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC);
            
            return java.time.Duration.between(created, now).toMinutes() <= 10;
        } catch (Exception e) {
            return false;
        }
    }

    private int dpToPx(int dp, android.content.Context context) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    private String getAbsoluteUrl(String url) {
        if (url == null || url.isEmpty() || url.startsWith("http") || url.startsWith("content") || url.startsWith("file")) {
            return url;
        }
        String baseUrl = com.example.app_ans.BuildConfig.API_BASE_URL;
        if (baseUrl == null || baseUrl.isEmpty() || baseUrl.equals("null")) {
            // Fallback just in case
            return url;
        }
        if (url.startsWith("/")) {
            return baseUrl + url;
        }
        return baseUrl + "/" + url;
    }

    private void addFileThumbnail(LinearLayout container, String thumbnailOrUrl, String mimeType) {
        addFileThumbnail(container, thumbnailOrUrl, mimeType, thumbnailOrUrl);
    }

    private void addFileThumbnail(LinearLayout container, String thumbnailOrUrl, String mimeType, String fullUrl) {
        if (thumbnailOrUrl == null || thumbnailOrUrl.isEmpty()) return;

        android.content.Context context = container.getContext();
        String absoluteThumbnail = getAbsoluteUrl(thumbnailOrUrl);
        String absoluteFull = getAbsoluteUrl(fullUrl);

        ImageView imageView = new ImageView(context);
        int size = dpToPx(32, context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMargins(0, 0, dpToPx(8, context), 0);
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setClipToOutline(true);
        imageView.setBackgroundResource(R.drawable.bg_status_chip);
        
        // Better MimeType detection for thumbnails
        String effectiveMimeType = mimeType;
        if (effectiveMimeType == null && thumbnailOrUrl != null) {
            String lowercaseUrl = thumbnailOrUrl.toLowerCase();
            if (lowercaseUrl.contains(".jpg") || lowercaseUrl.contains(".jpeg")) effectiveMimeType = "image/jpeg";
            else if (lowercaseUrl.contains(".png")) effectiveMimeType = "image/png";
            else if (lowercaseUrl.contains(".pdf")) effectiveMimeType = "application/pdf";
        }

        // If it's a Drive thumbnail, it might not have an extension but it's likely an image
        boolean isLikelyImage = (effectiveMimeType != null && effectiveMimeType.startsWith("image/"))
                               || (thumbnailOrUrl.contains("googleusercontent.com") && thumbnailOrUrl.contains("drive-storage"));

        if (isLikelyImage) {
            com.bumptech.glide.Glide.with(context)
                .load(absoluteThumbnail)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(imageView);
        } else {
            // Show icon for non-image files
            int iconRes = android.R.drawable.ic_menu_save; // Default
            if (mimeType != null) {
                if (mimeType.contains("pdf")) iconRes = android.R.drawable.ic_menu_view;
                else if (mimeType.contains("word") || mimeType.contains("officedocument.wordprocessingml")) iconRes = android.R.drawable.ic_menu_edit;
                else if (mimeType.contains("excel") || mimeType.contains("spreadsheetml")) iconRes = android.R.drawable.ic_menu_sort_by_size;
            }
            imageView.setImageResource(iconRes);
            imageView.setPadding(dpToPx(8, context), dpToPx(8, context), dpToPx(8, context), dpToPx(8, context));
            imageView.setColorFilter(context.getColor(R.color.ans_secondary));
        }

        final String finalMimeType = effectiveMimeType;
        imageView.setOnClickListener(v -> {
            if (listener != null) listener.onFileClick(absoluteFull, finalMimeType);
        });
        
        container.addView(imageView);
    }

    private void addCommentView(LinearLayout container, TaskAdvance.Comment comment) {
        View commentView = LayoutInflater.from(container.getContext()).inflate(R.layout.item_advance_comment, container, false);

        TextView author = commentView.findViewById(R.id.tv_comment_author);
        TextView date = commentView.findViewById(R.id.tv_comment_date);
        TextView text = commentView.findViewById(R.id.tv_comment_content);

        author.setText(comment.getUser() != null ? comment.getUser().getName() : "Usuario");
        date.setText(DateUtils.formatDateTime(comment.getCreatedAt()));
        text.setText(comment.getComment());

        container.addView(commentView);
    }

    @Override
    public int getItemCount() {
        return (filteredAdvances != null ? filteredAdvances.size() : 0) + (pendingAdvances != null ? pendingAdvances.size() : 0);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthor, tvDate, tvTime, tvSnippet, tvDescriptionFull;
        TextView docsLabel, commentsLabel;
        LinearLayout imagesContainer, commentsContainer;
        View expandedBody, imagesScroll;
        ImageView ivArrow;
        View btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthor = itemView.findViewById(R.id.tv_advance_author);
            tvDate = itemView.findViewById(R.id.tv_advance_date);
            tvTime = itemView.findViewById(R.id.tv_advance_time);
            tvSnippet = itemView.findViewById(R.id.tv_advance_snippet);
            tvDescriptionFull = itemView.findViewById(R.id.tv_advance_description_full);
            docsLabel = itemView.findViewById(R.id.tv_label_documents);
            commentsLabel = itemView.findViewById(R.id.tv_label_comments);
            imagesContainer = itemView.findViewById(R.id.advance_images_container);
            commentsContainer = itemView.findViewById(R.id.comments_container);
            imagesScroll = itemView.findViewById(R.id.advance_images_scroll);
            expandedBody = itemView.findViewById(R.id.expanded_body);
            ivArrow = itemView.findViewById(R.id.iv_expand_arrow);
            btnDelete = itemView.findViewById(R.id.btn_delete_advance);
        }
    }
}
