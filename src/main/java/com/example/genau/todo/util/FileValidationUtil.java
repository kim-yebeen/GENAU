package com.example.genau.todo.util;

public class FileValidationUtil {
    public static String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    public static boolean isValidExtension(String fileName, String[] allowedExtensions) {
        String ext = getExtension(fileName);
        for (String allowed : allowedExtensions) {
            if (allowed.equalsIgnoreCase(ext)) {
                return true;
            }
        }
        return false;
    }
}
