package com.dompetku.util;

import java.math.BigDecimal;

/**
 * Single money parser for form input and CSV import.
 *
 * Rules:
 * - Indonesian format: 1.500,50 -> 1500.50
 * - International decimals: 1500.50 -> 1500.50
 * - Grouped international: 1,500.50 -> 1500.50
 * - Grouped thousands: 1.500 -> 1500
 */
public final class MoneyParser {

    private MoneyParser() {
    }

    public static BigDecimal parse(String value) {
        if (value == null) {
            return null;
        }
        String text = value.replace("Rp", "").replace("IDR", "")
                .replace("\u00a0", "").replaceAll("\\s", "").trim();
        if (text.isEmpty() || text.startsWith("-")) {
            return null;
        }

        int comma = text.lastIndexOf(',');
        int dot = text.lastIndexOf('.');
        try {
            if (comma >= 0 && dot >= 0) {
                if (comma > dot) {
                    text = text.replace(".", "").replace(',', '.');
                } else {
                    text = text.replace(",", "");
                }
            } else if (comma >= 0) {
                if (text.length() - comma - 1 <= 2) {
                    text = text.replace(',', '.');
                } else {
                    text = text.replace(",", "");
                }
            } else if (dot >= 0 && text.matches("\\d{1,3}(\\.\\d{3})+")) {
                text = text.replace(".", "");
            }
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
