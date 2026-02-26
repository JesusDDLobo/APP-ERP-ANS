package com.example.app_ans.core.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.example.app_ans.R;

public class StatusUtils {

    public static void applyStatusColor(TextView statusView, String status) {
        if (statusView == null || status == null) return;

        int colorResId;
        String normalizedStatus = status.toLowerCase();

        if (normalizedStatus.contains("proceso") || normalizedStatus.contains("rechazado")) {
            colorResId = R.color.ans_yellow;
        } else if (normalizedStatus.contains("completado") || normalizedStatus.contains("terminado")) {
            colorResId = R.color.ans_green;
        } else if (normalizedStatus.contains("cancelado")) {
            colorResId = R.color.ans_error;
        } else {
            colorResId = R.color.ans_primary; // Default color
        }

        int color = ContextCompat.getColor(statusView.getContext(), colorResId);
        ViewCompat.setBackgroundTintList(statusView, ColorStateList.valueOf(color));
    }
}