package com.example.app_ans.tasks.ui;

import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.app_ans.R;
import com.example.app_ans.tasks.model.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskRenditionFragment extends Fragment {

    public static final String ARG_TASK_ID = "task_id";
    public static final String ARG_RENDITION_JSON = "rendition_json";
    public static final String ARG_RENDITION_CREATED_AT = "rendition_created_at";
    public static final String ARG_OPENED_FROM_TASKS = "opened_from_tasks";

    private MaterialButton btnCancelRendition;
    private MaterialButton btnSaveRendition;
    private TextView tvTotalReported;
    private TextView tvRenditionTaskTitle;
    private TextView tvRenditionDate;
    private TextView tvRenditionSubtitle;

    private AutoCompleteTextView actvCategory;
    private TextInputEditText etAmount;
    private TextInputEditText etDescription;
    private TextInputLayout tilAmount;
    private TextInputLayout tilDescription;
    private View uploadBox;
    private TextView tvAttachedFilesCount;
    private LinearLayout filesPreviewContainer;

    private TaskViewModel viewModel;
    private int taskId = -1;

    private String openedRenditionJson;
    private long openedRenditionCreatedAt = 0L;
    private boolean openedFromTasks = false;

    private final List<Uri> selectedFiles = new ArrayList<>();
    private final List<StoredAttachment> storedAttachments = new ArrayList<>();

    private final ActivityResultLauncher<String[]> renditionFilePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.OpenMultipleDocuments(),
                    uris -> {
                        if (uris == null || uris.isEmpty() || getContext() == null) {
                            return;
                        }

                        List<Uri> validUris = new ArrayList<>();
                        for (Uri uri : uris) {
                            if (com.example.app_ans.core.utils.FileStorageUtils.isValidFile(getContext(), uri)) {
                                persistReadPermission(uri);
                                validUris.add(uri);
                            } else {
                                Toast.makeText(
                                        getContext(),
                                        "Archivo no permitido: " +
                                                com.example.app_ans.core.utils.FileStorageUtils.getFileName(getContext(), uri),
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }

                        if (!validUris.isEmpty()) {
                            selectedFiles.clear();
                            storedAttachments.clear();
                            selectedFiles.addAll(validUris);
                            updateAttachedFilesText();
                            renderFilePreviews();
                            clearUploadError();
                            updateTotals();
                        }
                    }
            );

    public TaskRenditionFragment() {
    }

    public static TaskRenditionFragment newInstance(int taskId) {
        TaskRenditionFragment fragment = new TaskRenditionFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TASK_ID, taskId);
        fragment.setArguments(args);
        return fragment;
    }

    public static TaskRenditionFragment newDetailInstance(int taskId, String renditionJson, long createdAt) {
        TaskRenditionFragment fragment = new TaskRenditionFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TASK_ID, taskId);
        args.putString(ARG_RENDITION_JSON, renditionJson);
        args.putLong(ARG_RENDITION_CREATED_AT, createdAt);
        args.putBoolean(ARG_OPENED_FROM_TASKS, true);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task_rendition, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);

        if (getArguments() != null) {
            taskId = getArguments().getInt(ARG_TASK_ID, -1);
            openedRenditionJson = getArguments().getString(ARG_RENDITION_JSON, null);
            openedRenditionCreatedAt = getArguments().getLong(ARG_RENDITION_CREATED_AT, 0L);
            openedFromTasks = getArguments().getBoolean(ARG_OPENED_FROM_TASKS, false);
        }

        tvRenditionTaskTitle = view.findViewById(R.id.tv_rendition_task_title);
        tvRenditionDate = view.findViewById(R.id.tv_rendition_date);
        tvTotalReported = view.findViewById(R.id.tv_total_reported);
        tvRenditionSubtitle = view.findViewById(R.id.tv_rendition_subtitle);

        btnCancelRendition = view.findViewById(R.id.btn_cancel_rendition);
        btnSaveRendition = view.findViewById(R.id.btn_save_rendition);

        setupSingleExpenseForm(view);
        setupHeader();

        if (openedFromTasks && openedRenditionJson != null && !openedRenditionJson.trim().isEmpty()) {
            fillFormWithExistingRendition();
        }

        btnCancelRendition.setOnClickListener(v -> cancelRendition());

        btnSaveRendition.setOnClickListener(v -> {
            if (openedFromTasks) {
                Toast.makeText(
                        getContext(),
                        "Este detalle pertenece al módulo de Rendiciones.",
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                validateAndSaveRendition();
            }
        });

        if (openedFromTasks) {
            if (tvRenditionSubtitle != null) {
                tvRenditionSubtitle.setText("Detalle del gasto reportado");
            }
            if (btnSaveRendition != null) {
                btnSaveRendition.setText("Detalle cargado");
                btnSaveRendition.setEnabled(false);
                btnSaveRendition.setAlpha(0.7f);
            }
        }

        updateTotals();
    }

    private void setupHeader() {
        Task currentTask = null;

        if (viewModel.getTaskDetail().getValue() != null
                && viewModel.getTaskDetail().getValue().getId() == taskId) {
            currentTask = viewModel.getTaskDetail().getValue();
        }

        if (currentTask == null && viewModel.getTasks().getValue() != null) {
            for (Task item : viewModel.getTasks().getValue()) {
                if (item != null && item.getId() == taskId) {
                    currentTask = item;
                    break;
                }
            }
        }

        if (currentTask != null && tvRenditionTaskTitle != null) {
            tvRenditionTaskTitle.setText(
                    currentTask.getName() != null ? currentTask.getName() : "Tarea"
            );
        }

        if (tvRenditionDate != null) {
            if (openedFromTasks && openedRenditionCreatedAt > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                tvRenditionDate.setText(sdf.format(new Date(openedRenditionCreatedAt)));
            } else if (currentTask != null && currentTask.getStartTime() != null) {
                tvRenditionDate.setText(
                        com.example.app_ans.core.utils.DateUtils.formatDateOnly(currentTask.getStartTime())
                );
            }
        }
    }

    private void setupSingleExpenseForm(View rootView) {
        if (getContext() == null) return;

        ViewGroup rootLinear = findMainLinearLayout(rootView);
        if (rootLinear == null) return;

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View formView = inflater.inflate(R.layout.item_rendition_concept, rootLinear, false);

        actvCategory = formView.findViewById(R.id.actv_category);
        etAmount = formView.findViewById(R.id.et_amount);
        etDescription = formView.findViewById(R.id.et_description);
        tilAmount = formView.findViewById(R.id.til_amount);
        tilDescription = formView.findViewById(R.id.til_description);
        uploadBox = formView.findViewById(R.id.upload_box);
        tvAttachedFilesCount = formView.findViewById(R.id.tv_attached_files_count);
        filesPreviewContainer = formView.findViewById(R.id.files_preview_container);

        if (etAmount != null) {
            etAmount.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    clearAmountError();
                    updateTotals();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        if (etDescription != null) {
            etDescription.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    clearDescriptionError();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        if (uploadBox != null) {
            uploadBox.setOnClickListener(v -> {
                if (openedFromTasks) {
                    Toast.makeText(
                            getContext(),
                            "Toca el archivo listado abajo para abrirlo.",
                            Toast.LENGTH_SHORT
                    ).show();
                } else {
                    openFilePicker();
                }
            });
        }

        updateAttachedFilesText();
        renderFilePreviews();

        int insertIndex = Math.max(rootLinear.getChildCount() - 1, 0);
        rootLinear.addView(formView, insertIndex);
    }

    private void fillFormWithExistingRendition() {
        try {
            JsonObject jsonObject = JsonParser.parseString(openedRenditionJson).getAsJsonObject();

            String category = getStringField(jsonObject, "category");
            long amount = getLongField(jsonObject, "amount",
                    getLongField(jsonObject, "total_reported", 0L));
            String description = getStringField(jsonObject, "description");

            if (actvCategory != null) {
                actvCategory.setText(category, false);
                actvCategory.setEnabled(false);
                actvCategory.setFocusable(false);
                actvCategory.setClickable(false);
            }

            if (etAmount != null) {
                etAmount.setText(String.valueOf(amount));
                etAmount.setEnabled(false);
                etAmount.setFocusable(false);
                etAmount.setClickable(false);
            }

            if (etDescription != null) {
                etDescription.setText(description);
                etDescription.setEnabled(false);
                etDescription.setFocusable(false);
                etDescription.setClickable(false);
            }

            if (tilAmount != null) {
                tilAmount.setEnabled(false);
            }

            if (tilDescription != null) {
                tilDescription.setEnabled(false);
            }

            if (uploadBox != null) {
                uploadBox.setAlpha(0.7f);
            }

            selectedFiles.clear();
            storedAttachments.clear();

            if (jsonObject.has("files") && jsonObject.get("files").isJsonArray()) {
                JsonArray filesArray = jsonObject.getAsJsonArray("files");
                for (int i = 0; i < filesArray.size(); i++) {
                    if (!filesArray.get(i).isJsonObject()) continue;

                    JsonObject fileObj = filesArray.get(i).getAsJsonObject();
                    StoredAttachment attachment = new StoredAttachment();
                    attachment.name = getStringField(fileObj, "name");
                    attachment.uri = getStringField(fileObj, "uri");
                    attachment.mimeType = getStringField(fileObj, "mime_type");

                    if (attachment.name == null || attachment.name.trim().isEmpty()) {
                        attachment.name = "Archivo adjunto " + (i + 1);
                    }

                    storedAttachments.add(attachment);
                }
            } else {
                int filesCount = getIntField(jsonObject, "files_count", 0);
                for (int i = 1; i <= filesCount; i++) {
                    StoredAttachment attachment = new StoredAttachment();
                    attachment.name = "Archivo adjunto " + i;
                    attachment.uri = "";
                    attachment.mimeType = "";
                    storedAttachments.add(attachment);
                }
            }

            updateAttachedFilesText();
            renderFilePreviews();
            updateTotals();

        } catch (Exception e) {
            Toast.makeText(
                    getContext(),
                    "No se pudo cargar el detalle del reporte.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private String getStringField(JsonObject json, String key) {
        try {
            return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private long getLongField(JsonObject json, String key, long defaultValue) {
        try {
            return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsLong() : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private int getIntField(JsonObject json, String key, int defaultValue) {
        try {
            return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsInt() : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private ViewGroup findMainLinearLayout(View rootView) {
        if (rootView instanceof ViewGroup) {
            ViewGroup rootGroup = (ViewGroup) rootView;
            if (rootGroup.getChildCount() > 0 && rootGroup.getChildAt(0) instanceof ViewGroup) {
                return (ViewGroup) rootGroup.getChildAt(0);
            }
        }
        return null;
    }

    private void openFilePicker() {
        String[] mimeTypes = {
                "image/jpeg",
                "image/png",
                "image/webp",
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        };

        renditionFilePickerLauncher.launch(mimeTypes);
    }

    private void persistReadPermission(Uri uri) {
        if (getContext() == null || uri == null) return;

        try {
            getContext().getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (Exception ignored) {
        }
    }

    private void updateAttachedFilesText() {
        if (tvAttachedFilesCount == null) return;

        int count = openedFromTasks ? storedAttachments.size() : selectedFiles.size();

        if (count == 0) {
            tvAttachedFilesCount.setText("Imágenes, PDF, documentos");
        } else if (count == 1) {
            tvAttachedFilesCount.setText("1 archivo adjunto");
        } else {
            tvAttachedFilesCount.setText(count + " archivos adjuntos");
        }
    }

    private void renderFilePreviews() {
        if (filesPreviewContainer == null || getContext() == null) return;

        filesPreviewContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        if (openedFromTasks) {
            for (int i = 0; i < storedAttachments.size(); i++) {
                StoredAttachment attachment = storedAttachments.get(i);

                View itemView = inflater.inflate(R.layout.item_attached_file_preview, filesPreviewContainer, false);

                ImageView ivFileIcon = itemView.findViewById(R.id.iv_file_icon);
                TextView tvFileName = itemView.findViewById(R.id.tv_file_name);
                TextView tvFileMeta = itemView.findViewById(R.id.tv_file_meta);
                ImageView ivRemoveFile = itemView.findViewById(R.id.iv_remove_file);

                tvFileName.setText(
                        attachment.name != null && !attachment.name.trim().isEmpty()
                                ? attachment.name
                                : "Archivo adjunto"
                );

                tvFileMeta.setText(
                        attachment.mimeType != null && !attachment.mimeType.trim().isEmpty()
                                ? attachment.mimeType
                                : "Archivo adjunto"
                );

                ivFileIcon.setImageResource(R.drawable.iconodc);
                ivRemoveFile.setVisibility(View.GONE);

                itemView.setOnClickListener(v -> openStoredAttachment(attachment));

                filesPreviewContainer.addView(itemView);
            }
            return;
        }

        for (int i = 0; i < selectedFiles.size(); i++) {
            Uri fileUri = selectedFiles.get(i);
            View itemView = inflater.inflate(R.layout.item_attached_file_preview, filesPreviewContainer, false);

            ImageView ivFileIcon = itemView.findViewById(R.id.iv_file_icon);
            TextView tvFileName = itemView.findViewById(R.id.tv_file_name);
            TextView tvFileMeta = itemView.findViewById(R.id.tv_file_meta);
            ImageView ivRemoveFile = itemView.findViewById(R.id.iv_remove_file);

            String fileName = com.example.app_ans.core.utils.FileStorageUtils.getFileName(getContext(), fileUri);
            String mimeType = requireContext().getContentResolver().getType(fileUri);

            tvFileName.setText(fileName != null ? fileName : "Archivo");
            tvFileMeta.setText(getFileTypeLabel(fileName, mimeType));
            ivFileIcon.setImageResource(R.drawable.iconodc);

            final int index = i;
            ivRemoveFile.setOnClickListener(v -> {
                if (index >= 0 && index < selectedFiles.size()) {
                    selectedFiles.remove(index);
                    updateAttachedFilesText();
                    renderFilePreviews();
                    updateTotals();
                }
            });

            filesPreviewContainer.addView(itemView);
        }
    }

    private void openStoredAttachment(StoredAttachment attachment) {
        if (getContext() == null || attachment == null) return;

        if (attachment.uri == null || attachment.uri.trim().isEmpty()) {
            Toast.makeText(
                    getContext(),
                    "Este reporte viejo no guarda la ruta real del archivo.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        try {
            Uri uri = Uri.parse(attachment.uri);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(
                    uri,
                    attachment.mimeType != null && !attachment.mimeType.trim().isEmpty()
                            ? attachment.mimeType
                            : "*/*"
            );
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(attachment.uri));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(
                        getContext(),
                        "No se pudo abrir el archivo adjunto.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private String getFileTypeLabel(String fileName, String mimeType) {
        String lowerName = fileName != null ? fileName.toLowerCase() : "";
        String lowerMime = mimeType != null ? mimeType.toLowerCase() : "";

        if (lowerMime.contains("pdf") || lowerName.endsWith(".pdf")) {
            return "PDF";
        }

        if (lowerMime.startsWith("image/")) {
            return "Imagen";
        }

        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")
                || lowerName.endsWith(".png") || lowerName.endsWith(".webp")) {
            return "Imagen";
        }

        if (lowerName.endsWith(".doc") || lowerName.endsWith(".docx")) {
            return "Documento Word";
        }

        if (lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx")) {
            return "Hoja de cálculo";
        }

        if (lowerName.endsWith(".ppt") || lowerName.endsWith(".pptx")) {
            return "Presentación";
        }

        return "Archivo adjunto";
    }

    private void validateAndSaveRendition() {
        if (taskId <= 0) {
            Toast.makeText(getContext(), "No se encontró el id de la tarea.", Toast.LENGTH_SHORT).show();
            return;
        }

        clearAllErrors();

        long amount = parseAmount(etAmount != null ? etAmount.getText() : null);
        String description = etDescription != null && etDescription.getText() != null
                ? etDescription.getText().toString().trim()
                : "";
        String category = actvCategory != null
                ? actvCategory.getText().toString().trim()
                : "";

        if (amount <= 0) {
            showAmountError();
            Toast.makeText(getContext(), "Falta el monto.", Toast.LENGTH_SHORT).show();
            focusView(tilAmount != null ? tilAmount : etAmount);
            return;
        }

        if (description.isEmpty()) {
            showDescriptionError();
            Toast.makeText(getContext(), "Falta la descripción.", Toast.LENGTH_SHORT).show();
            focusView(tilDescription != null ? tilDescription : etDescription);
            return;
        }

        if (selectedFiles.isEmpty()) {
            showUploadError();
            Toast.makeText(getContext(), "Falta adjuntar al menos un archivo.", Toast.LENGTH_SHORT).show();
            focusView(uploadBox);
            return;
        }

        saveRenditionLocally(category, amount, description);
    }

    private void saveRenditionLocally(String category, long amount, String description) {
        try {
            JsonObject expenseMap = new JsonObject();
            expenseMap.addProperty("task_id", taskId);
            expenseMap.addProperty("category", category);
            expenseMap.addProperty("amount", amount);
            expenseMap.addProperty("description", description);
            expenseMap.addProperty("files_count", selectedFiles.size());
            expenseMap.addProperty("total_reported", amount);
            expenseMap.addProperty("created_at", System.currentTimeMillis());

            JsonArray filesArray = new JsonArray();

            for (Uri uri : selectedFiles) {
                JsonObject fileObj = new JsonObject();
                String fileName = com.example.app_ans.core.utils.FileStorageUtils.getFileName(getContext(), uri);
                String mimeType = getContext() != null
                        ? getContext().getContentResolver().getType(uri)
                        : "";

                fileObj.addProperty("name", fileName != null ? fileName : "Archivo");
                fileObj.addProperty("uri", uri.toString());
                fileObj.addProperty("mime_type", mimeType != null ? mimeType : "");

                filesArray.add(fileObj);
            }

            expenseMap.add("files", filesArray);

            String renditionJson = new Gson().toJson(expenseMap);

            viewModel.saveRenditionOffline(taskId, renditionJson, new ArrayList<>(selectedFiles));

            Toast.makeText(getContext(), "Gasto reportado localmente", Toast.LENGTH_SHORT).show();

            if (isAdded()) {
                requireActivity()
                        .getSupportFragmentManager()
                        .popBackStack();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al guardar gasto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private long calculateTotalReported() {
        return parseAmount(etAmount != null ? etAmount.getText() : null);
    }

    private void showAmountError() {
        if (tilAmount != null) {
            tilAmount.setError("Ingresa el monto");
            tilAmount.setErrorEnabled(true);
        }
    }

    private void clearAmountError() {
        if (tilAmount != null) {
            tilAmount.setError(null);
            tilAmount.setErrorEnabled(false);
        }
    }

    private void showDescriptionError() {
        if (tilDescription != null) {
            tilDescription.setError("Ingresa la descripción");
            tilDescription.setErrorEnabled(true);
        }
    }

    private void clearDescriptionError() {
        if (tilDescription != null) {
            tilDescription.setError(null);
            tilDescription.setErrorEnabled(false);
        }
    }

    private void showUploadError() {
        if (uploadBox != null) {
            uploadBox.setBackgroundResource(R.drawable.bg_upload_box_error);
        }
    }

    private void clearUploadError() {
        if (uploadBox != null) {
            uploadBox.setBackgroundResource(R.drawable.bg_upload_box_selector);
        }
    }

    private void clearAllErrors() {
        clearAmountError();
        clearDescriptionError();
        clearUploadError();
    }

    private void focusView(View view) {
        if (view == null) return;

        view.requestFocus();
        view.post(() -> {
            ViewParent parent = view.getParent();
            if (parent != null) {
                parent.requestChildFocus(view, view);
            }
        });
    }

    private void cancelRendition() {
        clearRenditionForm();

        if (isAdded()) {
            requireActivity()
                    .getSupportFragmentManager()
                    .popBackStack();
        }
    }

    private void clearRenditionForm() {
        selectedFiles.clear();
        storedAttachments.clear();

        if (actvCategory != null) actvCategory.setText("");
        if (etAmount != null) etAmount.setText("");
        if (etDescription != null) etDescription.setText("");

        updateAttachedFilesText();
        renderFilePreviews();
        updateTotals();
        clearAllErrors();
    }

    private void updateTotals() {
        long totalReported;

        if (openedFromTasks && openedRenditionJson != null) {
            totalReported = extractTotalFromJson(openedRenditionJson);
        } else {
            totalReported = calculateTotalReported();
        }

        if (tvTotalReported != null) {
            tvTotalReported.setText(formatCurrency(totalReported));
        }
    }

    private long extractTotalFromJson(String renditionJson) {
        try {
            JsonObject jsonObject = JsonParser.parseString(renditionJson).getAsJsonObject();

            if (jsonObject.has("total_reported") && !jsonObject.get("total_reported").isJsonNull()) {
                return jsonObject.get("total_reported").getAsLong();
            }

            if (jsonObject.has("amount") && !jsonObject.get("amount").isJsonNull()) {
                return jsonObject.get("amount").getAsLong();
            }

            return 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private long parseAmount(Editable editable) {
        if (editable == null) return 0;

        String raw = editable.toString().trim();
        if (raw.isEmpty()) return 0;

        raw = raw.replaceAll("[^\\d]", "");
        if (raw.isEmpty()) return 0;

        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String formatCurrency(long value) {
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("es", "CO"));
        return "$" + format.format(value);
    }

    private static class StoredAttachment {
        String name;
        String uri;
        String mimeType;
    }
}