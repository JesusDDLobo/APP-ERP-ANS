package com.example.app_ans.core.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    private static final String INPUT_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final String INPUT_FORMAT_SHORT = "yyyy-MM-dd'T'HH:mm:ss";
    private static final String OUTPUT_FORMAT = "HH:mm dd-MM-yyyy";

    public static String formatDateTime(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "N/A";
        Date date = parseIsoDate(isoDate);
        if (date == null) return isoDate;
        return new SimpleDateFormat(OUTPUT_FORMAT, Locale.getDefault()).format(date);
    }

    public static String formatDateOnly(String isoDate) {
        String formatted = formatDateTime(isoDate);
        if (formatted.contains(" ")) {
            return formatted.split(" ")[1]; // Returns dd-MM-yyyy
        }
        return formatted;
    }

    public static String formatTimeOnly(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "--:--";
        Date date = parseIsoDate(isoDate);
        if (date == null) return "--:--";
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
    }

    /**
     * Parsea una duración (HH:mm:ss o mm:ss) a segundos totales.
     */
    public static long parseDurationToSeconds(String duration) {
        if (duration == null || duration.isEmpty() || duration.equals("En progreso...")) return 0;
        try {
            String[] parts = duration.split(":");
            if (parts.length == 3) {
                return Long.parseLong(parts[0].trim()) * 3600 +
                       Long.parseLong(parts[1].trim()) * 60 +
                       Long.parseLong(parts[2].trim());
            } else if (parts.length == 2) {
                return Long.parseLong(parts[0].trim()) * 60 +
                       Long.parseLong(parts[1].trim());
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public static String formatSecondsToDuration(long totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Parsea una fecha ISO del servidor (UTC) a Date.
     */
    public static Date parseIsoDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty() || isoDate.equalsIgnoreCase("null")) return null;
        try {
            // Formato estándar ISO 8601 con zona horaria Z (UTC)
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            return sdf.parse(isoDate);
        } catch (Exception e) {
            try {
                // Intento con formato corto sin milisegundos
                SimpleDateFormat sdfShort = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                sdfShort.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                return sdfShort.parse(isoDate);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    /**
     * Calcula los segundos totales transcurridos (Histórico + Sesión Actual).
     */
    public static long getTotalRunningSeconds(com.example.app_ans.tasks.model.Task task) {
        if (task == null) return 0;

        // Intentamos obtener el historial guardado por el servidor
        long accumulatedSeconds = parseDurationToSeconds(task.getTotalTimeSpent());

        // Si no está corriendo, el acumulado es el total
        if (!task.isRunning()) return accumulatedSeconds;

        // Si está corriendo, calculamos el tiempo de la sesión actual
        long currentSessionSeconds = 0;
        if (task.getTimeLogs() != null) {
            for (com.example.app_ans.tasks.model.Task.TaskTimeLog log : task.getTimeLogs()) {
                // El ciclo activo es aquel que NO tiene end_time
                if (log.getEndTime() == null || log.getEndTime().isEmpty() || log.getEndTime().equalsIgnoreCase("null")) {
                    Date startDate = parseIsoDate(log.getStartTime());
                    if (startDate != null) {
                        // Importante: System.currentTimeMillis() es UTC, igual que el parseIsoDate simplificado
                        long diffMillis = System.currentTimeMillis() - startDate.getTime();
                        currentSessionSeconds = diffMillis / 1000;

                        // Si por algún error de reloj el tiempo es negativo, usamos 0
                        if (currentSessionSeconds < 0) currentSessionSeconds = 0;
                    }
                    break;
                }
            }
        }

        return accumulatedSeconds + currentSessionSeconds;
    }
}
