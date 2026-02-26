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
import com.example.app_ans.tasks.model.TaskQuestion;
import com.example.app_ans.tasks.network.dto.TaskCompletionAnswer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskCompleteDialog {
    private final Context context;
    private final List<TaskQuestion> questions;
    private final OnTaskCompleteSubmitListener listener;
    private final Map<Integer, Uri> selectedFiles = new HashMap<>();
    private OnDraftChangedListener draftListener;
    private Runnable onDismissListener;
    private String initialDraftJson;

    public interface OnTaskCompleteSubmitListener {
        void onSubmit(List<TaskCompletionAnswer> answers, Map<Integer, Uri> files);
    }

    public interface OnDraftChangedListener {
        void onDraftChanged(String draftJson);
    }

    public interface FilePickerLauncher {
        void launch(int questionId);
    }

    private FilePickerLauncher filePickerLauncher;

    public TaskCompleteDialog(Context context, List<TaskQuestion> questions, OnTaskCompleteSubmitListener listener) {
        this.context = context;
        this.questions = questions;
        this.listener = listener;
    }

    public void setFilePickerLauncher(FilePickerLauncher launcher) {
        this.filePickerLauncher = launcher;
    }

    public void setOnDraftChangedListener(OnDraftChangedListener listener) {
        this.draftListener = listener;
    }

    public void setOnDismissListener(Runnable listener) {
        this.onDismissListener = listener;
    }

    public void setInitialDraft(String draftJson) {
        this.initialDraftJson = draftJson;
    }

    public void onFileSelected(int questionId, Uri uri, String fileName) {
        selectedFiles.put(questionId, uri);
        // fileName could be used to update UI if we had references to the specific view
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        
        View containerView = inflater.inflate(R.layout.dialog_task_complete, null);
        LinearLayout questionsContainer = containerView.findViewById(R.id.questions_container);
        Button btnSubmit = containerView.findViewById(R.id.btn_submit_complete);
        Button btnCancel = containerView.findViewById(R.id.btn_cancel_complete);

        List<AnswerInput> inputs = new ArrayList<>();
        Map<Integer, String> recoveredAnswers = new HashMap<>();
        if (initialDraftJson != null && !initialDraftJson.isEmpty()) {
            try {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<Integer, String>>(){}.getType();
                recoveredAnswers = gson.fromJson(initialDraftJson, type);
            } catch (Exception ignored) {}
        }

        for (TaskQuestion q : questions) {
            View questionView = inflater.inflate(R.layout.item_task_question, questionsContainer, false);
            TextView questionText = questionView.findViewById(R.id.question_text);
            EditText answerInput = questionView.findViewById(R.id.answer_input);
            Button btnSelectFile = questionView.findViewById(R.id.btn_select_file);
            TextView selectedFileName = questionView.findViewById(R.id.selected_file_name);
            
            questionText.setText(q.getText() + (q.isRequired() ? " *" : ""));
            
            if ("file".equalsIgnoreCase(q.getType())) {
                answerInput.setVisibility(View.GONE);
                btnSelectFile.setVisibility(View.VISIBLE);
                btnSelectFile.setOnClickListener(v -> {
                    if (filePickerLauncher != null) {
                        filePickerLauncher.launch(q.getId());
                    }
                });
            } else {
                if (recoveredAnswers.containsKey(q.getId())) {
                    answerInput.setText(recoveredAnswers.get(q.getId()));
                }

                answerInput.addTextChangedListener(new android.text.TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(android.text.Editable s) {
                        notifyDraftChanged(inputs);
                    }
                });
            }

            questionsContainer.addView(questionView);
            inputs.add(new AnswerInput(q, answerInput, selectedFileName));
        }

        builder.setView(containerView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.setOnDismissListener(d -> {
            if (onDismissListener != null) onDismissListener.run();
        });

        btnSubmit.setOnClickListener(v -> {
            List<TaskCompletionAnswer> answers = new ArrayList<>();
            boolean isValid = true;
            int fileIndex = 0;

            for (AnswerInput input : inputs) {
                if ("file".equalsIgnoreCase(input.question.getType())) {
                    Uri fileUri = selectedFiles.get(input.question.getId());
                    if (input.question.isRequired() && fileUri == null) {
                        isValid = false;
                        // Show error somehow
                    } else if (fileUri != null) {
                        answers.add(new TaskCompletionAnswer(input.question.getId(), "Archivo adjunto", fileIndex));
                        fileIndex++;
                    }
                } else {
                    String text = input.editText.getText().toString();
                    if (input.question.isRequired() && text.isEmpty()) {
                        input.editText.setError("Requerido");
                        isValid = false;
                    } else {
                        answers.add(new TaskCompletionAnswer(input.question.getId(), text));
                    }
                }
            }

            if (isValid) {
                listener.onSubmit(answers, selectedFiles);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void notifyDraftChanged(List<AnswerInput> inputs) {
        if (draftListener == null) return;
        Map<Integer, String> draftMap = new HashMap<>();
        for (AnswerInput input : inputs) {
            if (!"file".equalsIgnoreCase(input.question.getType())) {
                draftMap.put(input.question.getId(), input.editText.getText().toString());
            }
        }
        com.google.gson.Gson gson = new com.google.gson.Gson();
        draftListener.onDraftChanged(gson.toJson(draftMap));
    }

    private static class AnswerInput {
        TaskQuestion question;
        EditText editText;
        TextView fileNameText;

        AnswerInput(TaskQuestion question, EditText editText, TextView fileNameText) {
            this.question = question;
            this.editText = editText;
            this.fileNameText = fileNameText;
        }
    }
}
