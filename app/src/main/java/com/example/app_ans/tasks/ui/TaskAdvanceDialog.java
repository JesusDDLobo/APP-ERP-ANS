package com.example.app_ans.tasks.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.net.Uri;

import com.example.app_ans.R;
import java.util.ArrayList;
import java.util.List;

public class TaskAdvanceDialog {
    private final Context context;
    private final OnAdvanceSubmitListener listener;
    private final List<Uri> selectedFiles = new ArrayList<>();
    private LinearLayout filesContainer;
    private String initialContent = "";
    private boolean isEditMode = false;
    private OnDraftChangedListener draftListener;
    private Runnable onDismissListener;

    public interface OnAdvanceSubmitListener {
        void onSubmit(String content, List<Uri> files);
        void onPickFiles();
    }

    public interface OnDraftChangedListener {
        void onDraftChanged(String content);
    }

    public TaskAdvanceDialog(Context context, OnAdvanceSubmitListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setOnDraftChangedListener(OnDraftChangedListener draftListener) {
        this.draftListener = draftListener;
    }

    public void setOnDismissListener(Runnable onDismissListener) {
        this.onDismissListener = onDismissListener;
    }

    public void setInitialContent(String content) {
        this.initialContent = content;
        this.isEditMode = true;
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_task_advance, null);

        TextView title = view.findViewById(R.id.dialog_title);
        if (title != null && isEditMode) title.setText("Editar Avance");

        EditText contentInput = view.findViewById(R.id.advance_content);
        if (isEditMode) {
            contentInput.setText(initialContent);
        } else if (initialContent != null && !initialContent.isEmpty()) {
            contentInput.setText(initialContent);
        }

        contentInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (draftListener != null && !isEditMode) {
                    draftListener.onDraftChanged(s.toString());
                }
            }
        });

        filesContainer = view.findViewById(R.id.files_container);
        Button btnAddFiles = view.findViewById(R.id.btn_add_files);
        if (isEditMode) btnAddFiles.setVisibility(View.GONE);

        Button btnCancel = view.findViewById(R.id.btn_cancel_advance);
        Button btnSubmit = view.findViewById(R.id.btn_submit_advance);
        if (isEditMode) btnSubmit.setText("Actualizar");

        builder.setView(view);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnAddFiles.setOnClickListener(v -> listener.onPickFiles());
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.setOnDismissListener(d -> {
            if (onDismissListener != null) {
                onDismissListener.run();
            }
        });

        btnSubmit.setOnClickListener(v -> {
            String content = contentInput.getText().toString();
            if (!content.isEmpty()) {
                listener.onSubmit(content, selectedFiles);
                dialog.dismiss();
            } else {
                contentInput.setError("Campo requerido");
            }
        });

        dialog.show();
    }

    public void addFiles(List<Uri> uris) {
        for (Uri uri : uris) {
            if (!selectedFiles.contains(uri)) {
                selectedFiles.add(uri);
                addFileView(uri);
            }
        }
    }

    private void addFileView(Uri uri) {
        if (filesContainer == null) return;
        
        View fileView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, filesContainer, false);
        TextView text = fileView.findViewById(android.R.id.text1);
        text.setText(com.example.app_ans.core.utils.FileStorageUtils.getFileName(context, uri));
        text.setTextSize(12);
        text.setPadding(0, 8, 0, 8);
        
        fileView.setOnClickListener(v -> {
            selectedFiles.remove(uri);
            filesContainer.removeView(fileView);
        });
        
        filesContainer.addView(fileView);
    }
}
