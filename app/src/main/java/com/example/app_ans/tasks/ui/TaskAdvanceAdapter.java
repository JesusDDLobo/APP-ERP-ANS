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

        List<TaskAdvance> sorted = new ArrayList<>(advances);
        sorted.sort((a, b) -> {
            String dateA = a.getCreatedAt();
            String dateB = b.getCreatedAt();

            if (dateA == null && dateB == null) return 0;
            if (dateA == null) return 1;
            if (dateB == null) return -1;

            try {
                java.time.OffsetDateTime dtA = parseOffsetDate(dateA);
                java.time.OffsetDateTime dtB = parseOffsetDate(dateB);
                return dtB.compareTo(dtA);
            } catch (Exception e) {
                return dateB.compareTo(dateA);
            }
        });

        this.advances = sorted;
        applyFilter();
    }

    public void filter(String query) {
        this.currentQuery = query != null ? query.toLowerCase().trim() : "";
        applyFilter();
    }

    private void applyFilter() {
        if (currentQuery.isEmpty()) {
            filteredAdvances = new ArrayList<>(advances);
        } else {
            filteredAdvances = new ArrayList<>();
            for (TaskAdvance advance : advances) {
                boolean matches = false;

                String comment = safeLower(advance.getComment());
                String author = advance.getUser() != null ? safeLower(advance.getUser().getName()) : "";
                String createdAt = safeLower(advance.getCreatedAt());
                String createdAtDate = safeLower(advance.getCreatedAtDate());
                String createdAtTime = safeLower(advance.getCreatedAtTime());
                String createdAtFormatted = safeLower(advance.getCreatedAtFormatted());

                if (comment.contains(currentQuery)
                        || author.contains(currentQuery)
                        || createdAt.contains(currentQuery)
                        || createdAtDate.contains(currentQuery)
                        || createdAtTime.contains(currentQuery)
                        || createdAtFormatted.contains(currentQuery)) {
                    matches = true;
                }

                if (matches) {
                    filteredAdvances.add(advance);
                }
            }
        }
        notifyDataSetChanged();
    }

    private String safeLower(String value) {
        return value != null ? value.toLowerCase() : "";
    }

    private java.time.OffsetDateTime parseOffsetDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return java.time.OffsetDateTime.MIN;
        }

        try {
            return java.time.OffsetDateTime.parse(dateStr);
        } catch (Exception ignored) {
        }

        try {
            String normalized = dateStr.replace(" ", "T");
            if (normalized.length() == 19) {
                normalized += "Z";
            }
            return java.time.OffsetDateTime.parse(normalized);
        } catch (Exception ignored) {
        }

        return java.time.OffsetDateTime.MIN;
    }

    public void setPendingAdvances(List<PendingAdvance> pendingAdvances) {
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
                holder.tvDate.setTextColor(holder.itemView.getContext().getColor(R.color.ans_gray_3));
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

            String authorName = (advance.getUser() != null && advance.getUser().getName() != null)
                    ? advance.getUser().getName()
                    : "Técnico";
            holder.tvAuthor.setText(authorName);

            String dateText = advance.getCreatedAtDate();
            String timeText = advance.getCreatedAtTime();

            if (dateText == null || dateText.trim().isEmpty()) {
                dateText = DateUtils.formatDateOnly(advance.getCreatedAt());
            }
            if (timeText == null || timeText.trim().isEmpty()) {
                timeText = DateUtils.formatTimeOnly(advance.getCreatedAt());
            }

            holder.tvDate.setText(dateText != null ? dateText : "");
            holder.tvTime.setText(timeText != null ? timeText : "");

            String comment = advance.getComment() != null ? advance.getComment() : "";
            holder.tvSnippet.setText(comment);
            holder.tvDescriptionFull.setText(comment);
            holder.itemView.setAlpha(1.0f);

            holder.tvSnippet.setVisibility(isExpanded ? View.GONE : View.VISIBLE);

            if (isWithinTenMinutes(advance.getCreatedAt())) {
                holder.btnDelete.setVisibility(View.VISIBLE);
                holder.btnDelete.setOnClickListener(v -> {
                    if (listener != null) listener.onDelete(advance);
                });
            } else {
                holder.btnDelete.setVisibility(View.GONE);
            }

            holder.imagesContainer.removeAllViews();
            if (advance.getFiles() != null && !advance.getFiles().isEmpty()) {
                holder.imagesScroll.setVisibility(View.VISIBLE);
                holder.docsLabel.setVisibility(View.VISIBLE);

                for (TaskAdvance.AdvanceFile file : advance.getFiles()) {
                    addFileThumbnail(
                            holder.imagesContainer,
                            file.getThumbnailUrl() != null ? file.getThumbnailUrl() : file.getUrl(),
                            file.getMimeType(),
                            file.getUrl()
                    );
                }
            } else {
                holder.imagesScroll.setVisibility(View.GONE);
                holder.docsLabel.setVisibility(View.GONE);
            }

            holder.commentsContainer.removeAllViews();
            if (advance.getComments() != null && !advance.getComments().isEmpty()) {
                holder.commentsLabel.setVisibility(View.VISIBLE);
                for (TaskAdvance.Comment commentItem : advance.getComments()) {
                    addCommentView(holder.commentsContainer, commentItem);
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
        if (createdAt == null || createdAt.trim().isEmpty()) return false;

        try {
            java.time.OffsetDateTime created = parseOffsetDate(createdAt);
            if (created.equals(java.time.OffsetDateTime.MIN)) return false;

            java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
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

        String effectiveMimeType = mimeType;
        if (effectiveMimeType == null && thumbnailOrUrl != null) {
            String lowercaseUrl = thumbnailOrUrl.toLowerCase();
            if (lowercaseUrl.contains(".jpg") || lowercaseUrl.contains(".jpeg")) effectiveMimeType = "image/jpeg";
            else if (lowercaseUrl.contains(".png")) effectiveMimeType = "image/png";
            else if (lowercaseUrl.contains(".pdf")) effectiveMimeType = "application/pdf";
        }

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
            int iconRes = android.R.drawable.ic_menu_save;
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

        String formattedDate = comment.getCreatedAtFormatted();
        if (formattedDate == null || formattedDate.trim().isEmpty()) {
            if (comment.getCreatedAtDate() != null && comment.getCreatedAtTime() != null) {
                formattedDate = comment.getCreatedAtDate() + " " + comment.getCreatedAtTime();
            } else {
                formattedDate = DateUtils.formatDateTime(comment.getCreatedAt());
            }
        }

        date.setText(formattedDate != null ? formattedDate : "");
        text.setText(comment.getComment());

        container.addView(commentView);
    }

    @Override
    public int getItemCount() {
        return (filteredAdvances != null ? filteredAdvances.size() : 0)
                + (pendingAdvances != null ? pendingAdvances.size() : 0);
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