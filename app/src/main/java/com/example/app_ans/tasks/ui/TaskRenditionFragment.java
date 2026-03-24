package com.example.app_ans.tasks.ui;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AutoCompleteTextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.app_ans.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TaskRenditionFragment extends Fragment {

    public static final String ARG_TASK_ID = "task_id";

    private LinearLayout conceptsContainer;
    private MaterialButton btnNewConcept;
    private MaterialButton btnCancelRendition;
    private MaterialButton btnSaveRendition;
    private TextView tvTotalReported;
    private TextView tvTotalApproved;

    private TaskViewModel viewModel;
    private int taskId = -1;

    private final List<ConceptHolder> conceptHolders = new ArrayList<>();
    private int conceptCounter = 0;
    private ConceptHolder currentFileTarget;

    private final ActivityResultLauncher<String[]> renditionFilePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.OpenMultipleDocuments(),
                    uris -> {
                        if (uris == null || uris.isEmpty() || currentFileTarget == null || getContext() == null) {
                            return;
                        }

                        List<Uri> validUris = new ArrayList<>();
                        for (Uri uri : uris) {
                            if (com.example.app_ans.core.utils.FileStorageUtils.isValidFile(getContext(), uri)) {
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
                            currentFileTarget.files.clear();
                            currentFileTarget.files.addAll(validUris);
                            updateAttachedFilesText(currentFileTarget);
                            renderFilePreviews(currentFileTarget);
                            clearUploadError(currentFileTarget);
                        }

                        currentFileTarget = null;
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
        }

        conceptsContainer = view.findViewById(R.id.concepts_container);
        btnNewConcept = view.findViewById(R.id.btn_new_concept);
        btnCancelRendition = view.findViewById(R.id.btn_cancel_rendition);
        btnSaveRendition = view.findViewById(R.id.btn_save_rendition);
        tvTotalReported = view.findViewById(R.id.tv_total_reported);
        tvTotalApproved = view.findViewById(R.id.tv_total_approved);

        addNewConcept();

        btnNewConcept.setOnClickListener(v -> addNewConcept());
        btnCancelRendition.setOnClickListener(v -> cancelRendition());
        btnSaveRendition.setOnClickListener(v -> validateAndSaveRendition());

        updateTotals();
    }

    private void addNewConcept() {
        if (getContext() == null || conceptsContainer == null) return;

        conceptCounter++;

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View conceptView = inflater.inflate(R.layout.item_rendition_concept, conceptsContainer, false);

        TextView tvConceptTitle = conceptView.findViewById(R.id.tv_concept_title);
        ImageView ivExpandConcept = conceptView.findViewById(R.id.iv_expand_concept);
        LinearLayout conceptContent = conceptView.findViewById(R.id.concept_content);
        LinearLayout conceptHeader = conceptView.findViewById(R.id.concept_header);
        AutoCompleteTextView actvCategory = conceptView.findViewById(R.id.actv_category);
        TextInputEditText etAmount = conceptView.findViewById(R.id.et_amount);
        TextInputEditText etDescription = conceptView.findViewById(R.id.et_description);
        TextInputLayout tilAmount = conceptView.findViewById(R.id.til_amount);
        TextInputLayout tilDescription = conceptView.findViewById(R.id.til_description);
        View uploadBox = conceptView.findViewById(R.id.upload_box);
        TextView tvAttachedFilesCount = conceptView.findViewById(R.id.tv_attached_files_count);
        LinearLayout filesPreviewContainer = conceptView.findViewById(R.id.files_preview_container);

        ConceptHolder holder = new ConceptHolder();
        holder.index = conceptCounter;
        holder.rootView = conceptView;
        holder.conceptHeader = conceptHeader;
        holder.conceptContent = conceptContent;
        holder.ivExpandConcept = ivExpandConcept;
        holder.actvCategory = actvCategory;
        holder.etAmount = etAmount;
        holder.etDescription = etDescription;
        holder.tilAmount = tilAmount;
        holder.tilDescription = tilDescription;
        holder.uploadBox = uploadBox;
        holder.tvAttachedFilesCount = tvAttachedFilesCount;
        holder.filesPreviewContainer = filesPreviewContainer;

        tvConceptTitle.setText("Concepto " + conceptCounter);

        conceptContent.setVisibility(View.VISIBLE);
        ivExpandConcept.setRotation(90f);

        View.OnClickListener toggleListener = v -> toggleConcept(holder);
        conceptHeader.setOnClickListener(toggleListener);
        ivExpandConcept.setOnClickListener(toggleListener);

        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearAmountError(holder);
                updateTotals();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        etDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearDescriptionError(holder);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        uploadBox.setOnClickListener(v -> openFilePickerForConcept(holder));
        updateAttachedFilesText(holder);
        renderFilePreviews(holder);

        conceptHolders.add(holder);
        conceptsContainer.addView(conceptView);

        updateTotals();
    }

    private void toggleConcept(ConceptHolder holder) {
        boolean isVisible = holder.conceptContent.getVisibility() == View.VISIBLE;
        holder.conceptContent.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        holder.ivExpandConcept.setRotation(isVisible ? 0f : 90f);
    }

    private void expandConcept(ConceptHolder holder) {
        holder.conceptContent.setVisibility(View.VISIBLE);
        holder.ivExpandConcept.setRotation(90f);
    }

    private void openFilePickerForConcept(ConceptHolder holder) {
        currentFileTarget = holder;

        String[] mimeTypes = {
                "image/jpeg", "image/png", "image/webp", "application/pdf",
                "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        };

        renditionFilePickerLauncher.launch(mimeTypes);
    }

    private void updateAttachedFilesText(ConceptHolder holder) {
        if (holder.tvAttachedFilesCount == null) return;

        int count = holder.files.size();
        if (count == 0) {
            holder.tvAttachedFilesCount.setText("Imágenes, PDF, documentos");
        } else if (count == 1) {
            holder.tvAttachedFilesCount.setText("1 archivo seleccionado");
        } else {
            holder.tvAttachedFilesCount.setText(count + " archivos seleccionados");
        }
    }

    private void renderFilePreviews(ConceptHolder holder) {
        if (holder.filesPreviewContainer == null || getContext() == null) return;

        holder.filesPreviewContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (int i = 0; i < holder.files.size(); i++) {
            Uri fileUri = holder.files.get(i);
            View itemView = inflater.inflate(R.layout.item_attached_file_preview, holder.filesPreviewContainer, false);

            ImageView ivFileIcon = itemView.findViewById(R.id.iv_file_icon);
            TextView tvFileName = itemView.findViewById(R.id.tv_file_name);
            TextView tvFileMeta = itemView.findViewById(R.id.tv_file_meta);
            ImageView ivRemoveFile = itemView.findViewById(R.id.iv_remove_file);

            String fileName = com.example.app_ans.core.utils.FileStorageUtils.getFileName(getContext(), fileUri);
            String mimeType = requireContext().getContentResolver().getType(fileUri);

            tvFileName.setText(fileName != null ? fileName : "Archivo");
            tvFileMeta.setText(getFileTypeLabel(fileName, mimeType));
            ivFileIcon.setImageResource(getFileIconRes(fileName, mimeType));

            final int index = i;
            ivRemoveFile.setOnClickListener(v -> {
                if (index >= 0 && index < holder.files.size()) {
                    holder.files.remove(index);
                    updateAttachedFilesText(holder);
                    renderFilePreviews(holder);
                }
            });

            holder.filesPreviewContainer.addView(itemView);
        }
    }

    private int getFileIconRes(String fileName, String mimeType) {
        return R.drawable.iconodc;
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

        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || lowerName.endsWith(".webp")) {
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
        if (conceptHolders.isEmpty()) {
            Toast.makeText(getContext(), "Debes agregar al menos un concepto.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (taskId <= 0) {
            Toast.makeText(getContext(), "No se encontró el id de la tarea.", Toast.LENGTH_SHORT).show();
            return;
        }

        clearAllErrors();

        for (ConceptHolder holder : conceptHolders) {
            expandConcept(holder);

            long amount = parseAmount(holder.etAmount != null ? holder.etAmount.getText() : null);
            String description = holder.etDescription != null && holder.etDescription.getText() != null
                    ? holder.etDescription.getText().toString().trim()
                    : "";

            if (amount <= 0) {
                showAmountError(holder);
                Toast.makeText(getContext(), "Falta el monto en el Concepto " + holder.index, Toast.LENGTH_SHORT).show();
                focusView(holder.tilAmount != null ? holder.tilAmount : holder.etAmount);
                return;
            }

            if (description.isEmpty()) {
                showDescriptionError(holder);
                Toast.makeText(getContext(), "Falta la descripción en el Concepto " + holder.index, Toast.LENGTH_SHORT).show();
                focusView(holder.tilDescription != null ? holder.tilDescription : holder.etDescription);
                return;
            }

            if (holder.files.isEmpty()) {
                showUploadError(holder);
                Toast.makeText(getContext(), "Falta adjuntar archivo en el Concepto " + holder.index, Toast.LENGTH_SHORT).show();
                focusView(holder.uploadBox);
                return;
            }
        }

        saveRenditionLocally();
    }

    private void saveRenditionLocally() {
        try {
            List<Map<String, Object>> concepts = new ArrayList<>();
            List<Uri> allFiles = new ArrayList<>();

            for (ConceptHolder holder : conceptHolders) {
                Map<String, Object> conceptMap = new HashMap<>();
                conceptMap.put("index", holder.index);
                conceptMap.put("category", holder.actvCategory != null ? holder.actvCategory.getText().toString().trim() : "");
                conceptMap.put("amount", parseAmount(holder.etAmount != null ? holder.etAmount.getText() : null));
                conceptMap.put("description", holder.etDescription != null && holder.etDescription.getText() != null
                        ? holder.etDescription.getText().toString().trim()
                        : "");
                conceptMap.put("files_count", holder.files.size());

                concepts.add(conceptMap);
                allFiles.addAll(holder.files);
            }

            Map<String, Object> renditionMap = new HashMap<>();
            renditionMap.put("task_id", taskId);
            renditionMap.put("total_reported", calculateTotalReported());
            renditionMap.put("total_approved", 0);
            renditionMap.put("concepts", concepts);
            renditionMap.put("created_at", System.currentTimeMillis());

            Gson gson = new Gson();
            String renditionJson = gson.toJson(renditionMap);

            viewModel.saveRenditionOffline(taskId, renditionJson, allFiles);

            Toast.makeText(getContext(), "Rendición guardada localmente", Toast.LENGTH_SHORT).show();

            if (isAdded()) {
                requireActivity()
                        .getSupportFragmentManager()
                        .popBackStack();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al guardar rendición: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private long calculateTotalReported() {
        long totalReported = 0;

        for (ConceptHolder holder : conceptHolders) {
            totalReported += parseAmount(holder.etAmount != null ? holder.etAmount.getText() : null);
        }

        return totalReported;
    }

    private void showAmountError(ConceptHolder holder) {
        if (holder.tilAmount != null) {
            holder.tilAmount.setError("Ingresa el monto");
            holder.tilAmount.setErrorEnabled(true);
        }
    }

    private void clearAmountError(ConceptHolder holder) {
        if (holder.tilAmount != null) {
            holder.tilAmount.setError(null);
            holder.tilAmount.setErrorEnabled(false);
        }
    }

    private void showDescriptionError(ConceptHolder holder) {
        if (holder.tilDescription != null) {
            holder.tilDescription.setError("Ingresa la descripción");
            holder.tilDescription.setErrorEnabled(true);
        }
    }

    private void clearDescriptionError(ConceptHolder holder) {
        if (holder.tilDescription != null) {
            holder.tilDescription.setError(null);
            holder.tilDescription.setErrorEnabled(false);
        }
    }

    private void showUploadError(ConceptHolder holder) {
        if (holder.uploadBox != null && getContext() != null) {
            holder.uploadBox.setBackgroundResource(R.drawable.bg_upload_box_error);
        }
    }

    private void clearUploadError(ConceptHolder holder) {
        if (holder.uploadBox != null && getContext() != null) {
            holder.uploadBox.setBackgroundResource(R.drawable.bg_upload_box_selector);
        }
    }

    private void clearAllErrors() {
        for (ConceptHolder holder : conceptHolders) {
            clearAmountError(holder);
            clearDescriptionError(holder);
            clearUploadError(holder);
        }
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
        conceptHolders.clear();
        conceptCounter = 0;
        currentFileTarget = null;

        if (conceptsContainer != null) {
            conceptsContainer.removeAllViews();
        }

        if (tvTotalReported != null) {
            tvTotalReported.setText(formatCurrency(0));
        }

        if (tvTotalApproved != null) {
            tvTotalApproved.setText(formatCurrency(0));
        }
    }

    private void updateTotals() {
        long totalReported = calculateTotalReported();

        if (tvTotalReported != null) {
            tvTotalReported.setText(formatCurrency(totalReported));
        }

        if (tvTotalApproved != null) {
            tvTotalApproved.setText(formatCurrency(0));
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

    private static class ConceptHolder {
        int index;
        View rootView;
        View conceptHeader;
        LinearLayout conceptContent;
        ImageView ivExpandConcept;
        AutoCompleteTextView actvCategory;
        TextInputEditText etAmount;
        TextInputEditText etDescription;
        TextInputLayout tilAmount;
        TextInputLayout tilDescription;
        View uploadBox;
        TextView tvAttachedFilesCount;
        LinearLayout filesPreviewContainer;
        List<Uri> files = new ArrayList<>();
    }
}