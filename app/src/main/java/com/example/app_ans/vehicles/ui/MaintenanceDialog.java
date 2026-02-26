package com.example.app_ans.vehicles.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.app_ans.R;
import com.example.app_ans.vehicles.ui.MaintenanceDialog.OnMaintenanceSubmitListener;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MaintenanceDialog {
    private final Context context;
    private final Map<String, Object> config;
    private final OnMaintenanceSubmitListener listener;
    private final Map<String, Uri> selectedImages = new HashMap<>();
    private final Map<String, MaintenanceFieldInput> viewMap = new HashMap<>();

    public interface OnMaintenanceSubmitListener {
        void onSubmit(String dataJson, Map<String, Uri> images);
    }

    public interface ImagePickerLauncher {
        void launch(String fieldName);
    }

    private ImagePickerLauncher imagePickerLauncher;

    public MaintenanceDialog(Context context, Map<String, Object> config, OnMaintenanceSubmitListener listener) {
        this.context = context;
        this.config = config;
        this.listener = listener;
    }

    public void setImagePickerLauncher(ImagePickerLauncher launcher) {
        this.imagePickerLauncher = launcher;
    }

    public void onImageSelected(String fieldName, Uri uri, String fileName) {
        selectedImages.put(fieldName, uri);
        MaintenanceFieldInput input = viewMap.get(fieldName);
        if (input != null && input.imageNameText != null) {
            String statusText = "✓ " + fileName;
            input.imageNameText.setText(statusText);
            input.imageNameText.setTextColor(ContextCompat.getColor(context, R.color.ans_primary));
        }
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);

        View containerView = inflater.inflate(R.layout.dialog_maintenance, null);
        LinearLayout fieldsContainer = containerView.findViewById(R.id.fields_container);
        Button btnSubmit = containerView.findViewById(R.id.btn_submit_maintenance);
        Button btnCancel = containerView.findViewById(R.id.btn_cancel_maintenance);

        List<Map<String, Object>> schema = (List<Map<String, Object>>) config.get("schema");
        android.util.Log.d("MaintenanceDialog", "Full config: " + config.toString());

        if (schema != null) {
            for (Map<String, Object> field : schema) {
                // Determine final values before using them in lambdas
                String nameValue = "field_" + schema.indexOf(field);
                if (field.containsKey("name") && field.get("name") != null) nameValue = String.valueOf(field.get("name"));
                else if (field.containsKey("id") && field.get("id") != null) nameValue = String.valueOf(field.get("id"));

                final String currentFieldName = nameValue;

                // SKIP odometer if it's in the dynamic schema, as it's usually handled separately
                if ("odometer".equalsIgnoreCase(currentFieldName)) {
                    continue;
                }

                View fieldView = inflater.inflate(R.layout.item_maintenance_field, fieldsContainer, false);
                TextView titleView = fieldView.findViewById(R.id.field_title);
                TextView descriptionView = fieldView.findViewById(R.id.field_label);
                View inputLayout = fieldView.findViewById(R.id.field_input_layout);
                EditText inputText = fieldView.findViewById(R.id.field_input_text);
                Spinner inputSelect = fieldView.findViewById(R.id.field_input_select);
                LinearLayout imageContainer = fieldView.findViewById(R.id.field_input_image_container);
                Button btnSelectImage = fieldView.findViewById(R.id.btn_select_image);
                TextView selectedImageName = fieldView.findViewById(R.id.selected_image_name);

                // Title strategy: Use 'label' or 'title' if present, otherwise format 'name'
                String titleValue = "";
                if (field.containsKey("label") && field.get("label") != null) titleValue = String.valueOf(field.get("label"));
                else if (field.containsKey("title") && field.get("title") != null) titleValue = String.valueOf(field.get("title"));
                else titleValue = formatFieldNameAsTitle(currentFieldName);

                final String currentTitleText = titleValue;

                // Description strategy: Use 'description' or 'description_text'
                String descValue = "";
                if (field.containsKey("description") && field.get("description") != null) descValue = String.valueOf(field.get("description"));
                else if (field.containsKey("description_text") && field.get("description_text") != null) descValue = String.valueOf(field.get("description_text"));
                // If label was NOT used as title, we could use it here, but typically label is the title.

                final String currentDescriptionText = descValue;
                final String currentFieldType = field.containsKey("type") ? String.valueOf(field.get("type")) : "text";

                Object allowImgObj = field.get("allow_images");
                boolean allows = false;
                if (allowImgObj instanceof Boolean) allows = (Boolean) allowImgObj;
                else if (allowImgObj != null) allows = Boolean.parseBoolean(String.valueOf(allowImgObj));
                final boolean currentAllowImages = allows;

                android.util.Log.d("MaintenanceDialog", "Rendering Field: " + currentFieldName + " | Title: " + currentTitleText);

                titleView.setText(currentTitleText);
                titleView.setVisibility(View.VISIBLE);

                if (!currentDescriptionText.isEmpty()) {
                    descriptionView.setText(currentDescriptionText);
                    descriptionView.setVisibility(View.VISIBLE);
                }

                if ("selector".equals(currentFieldType) || "select".equals(currentFieldType)) {
                    inputSelect.setVisibility(View.VISIBLE);
                    List<String> options = (List<String>) field.get("options");
                    if (options != null) {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, options);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        inputSelect.setAdapter(adapter);
                    }
                } else {
                    inputLayout.setVisibility(View.VISIBLE);
                    if ("number".equals(currentFieldType)) {
                        inputText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                        inputText.setHint("0.0");
                    } else {
                        inputText.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
                        inputText.setHint("Respuesta...");
                    }
                }

                if (currentAllowImages) {
                    imageContainer.setVisibility(View.VISIBLE);
                    btnSelectImage.setOnClickListener(v -> {
                        if (imagePickerLauncher != null) {
                            imagePickerLauncher.launch(currentFieldName);
                        }
                    });

                    // Add a long click listener to clear image, making it more like "task advancements" file list
                    selectedImageName.setOnLongClickListener(v -> {
                        selectedImages.remove(currentFieldName);
                        selectedImageName.setText("No seleccionado");
                        selectedImageName.setTextColor(ContextCompat.getColor(context, R.color.ans_gray_3));
                        return true;
                    });
                }

                fieldsContainer.addView(fieldView);
                viewMap.put(currentFieldName, new MaintenanceFieldInput(field, inputText, inputSelect, selectedImageName));
            }
        }

        builder.setView(containerView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            Map<String, Object> data = new HashMap<>();
            for (Map.Entry<String, MaintenanceFieldInput> entry : viewMap.entrySet()) {
                String name = entry.getKey();
                MaintenanceFieldInput input = entry.getValue();

                String type = "text";
                if (input.field.containsKey("type") && input.field.get("type") != null) {
                    type = String.valueOf(input.field.get("type"));
                }

                if ("selector".equals(type) || "select".equals(type)) {
                    Object selected = input.spinner.getSelectedItem();
                    data.put(name, selected != null ? selected.toString() : "");
                } else {
                    data.put(name, input.editText.getText().toString());
                }
            }

            Gson gson = new Gson();
            String dataJson = gson.toJson(data);
            android.util.Log.d("MaintenanceDialog", "Submitting form data: " + dataJson);
            listener.onSubmit(dataJson, selectedImages);
            dialog.dismiss();
        });

        dialog.show();
    }

    private String formatFieldNameAsTitle(String name) {
        if (name == null || name.isEmpty()) return "";
        // Replace underscores with spaces and capitalize
        String[] words = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                sb.append(word.substring(1).toLowerCase());
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    private static class MaintenanceFieldInput {
        Map<String, Object> field;
        EditText editText;
        Spinner spinner;
        TextView imageNameText;

        MaintenanceFieldInput(Map<String, Object> field, EditText editText, Spinner spinner, TextView imageNameText) {
            this.field = field;
            this.editText = editText;
            this.spinner = spinner;
            this.imageNameText = imageNameText;
        }
    }
}
