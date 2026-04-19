package ru.iposhka.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EmailUtils {

    public static String maskEmail(String email) {
        String trimmed = email.trim();
        int at = trimmed.indexOf('@');
        String localPart = trimmed.substring(0, at);
        String domain = trimmed.substring(at);

        if (localPart.length() <= 2) {
            return "***" + domain;
        }

        return localPart.substring(0, 2) + "***" + domain;
    }
}
