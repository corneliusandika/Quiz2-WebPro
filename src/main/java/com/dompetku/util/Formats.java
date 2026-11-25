package com.dompetku.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;

/** Formatting helpers exposed to JSP through a TLD. */
public final class Formats {

    private static final Locale ID = new Locale("id", "ID");
    private static final String CURRENCY_DECIMALS = normalizeCurrencyDecimals(
            System.getenv("DOMPETKU_CURRENCY_DECIMALS"));
    private static final String[] SHORT_MONTHS = {
            "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
    };
    private static final String[] LONG_MONTHS = {
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    };

    private Formats() {
    }

    private static String normalizeCurrencyDecimals(String value) {
        if ("always".equalsIgnoreCase(value)) {
            return "always";
        }
        if ("never".equalsIgnoreCase(value)) {
            return "never";
        }
        return "auto";
    }

    public static String currencyMode() {
        return CURRENCY_DECIMALS;
    }

    public static String rupiah(BigDecimal value) {
        if (value == null) {
            return "always".equals(CURRENCY_DECIMALS) ? "Rp 0,00" : "Rp 0";
        }
        NumberFormat nf = NumberFormat.getNumberInstance(ID);
        boolean fractional = value.stripTrailingZeros().scale() > 0;
        nf.setMinimumFractionDigits("always".equals(CURRENCY_DECIMALS)
                || ("auto".equals(CURRENCY_DECIMALS) && fractional) ? 2 : 0);
        nf.setMaximumFractionDigits("never".equals(CURRENCY_DECIMALS) ? 0 : 2);
        return "Rp " + nf.format(value);
    }

    public static String compact(BigDecimal value) {
        if (value == null) {
            return "Rp 0";
        }
        BigDecimal abs = value.abs();
        BigDecimal divisor;
        String suffix;
        if (abs.compareTo(BigDecimal.valueOf(1_000_000_000L)) >= 0) {
            divisor = BigDecimal.valueOf(1_000_000_000L);
            suffix = " M";
        } else if (abs.compareTo(BigDecimal.valueOf(1_000_000L)) >= 0) {
            divisor = BigDecimal.valueOf(1_000_000L);
            suffix = " jt";
        } else if (abs.compareTo(BigDecimal.valueOf(1_000L)) >= 0) {
            divisor = BigDecimal.valueOf(1_000L);
            suffix = " rb";
        } else {
            return rupiah(value);
        }
        DecimalFormat df = new DecimalFormat(
                "always".equals(CURRENCY_DECIMALS) ? "0.00"
                        : "never".equals(CURRENCY_DECIMALS) ? "0" : "0.#",
                DecimalFormatSymbols.getInstance(ID));
        String sign = value.signum() < 0 ? "-" : "";
        return "Rp " + sign + df.format(abs.divide(divisor, 1, RoundingMode.HALF_UP)) + suffix;
    }

    public static String date(LocalDate value) {
        if (value == null) {
            return "-";
        }
        return value.getDayOfMonth() + " " + SHORT_MONTHS[value.getMonthValue() - 1] + " " + value.getYear();
    }

    public static String month(LocalDate value) {
        if (value == null) {
            return "-";
        }
        return LONG_MONTHS[value.getMonthValue() - 1] + " " + value.getYear();
    }

    public static String shortMonth(LocalDate value) {
        if (value == null) {
            return "-";
        }
        return SHORT_MONTHS[value.getMonthValue() - 1] + " " + value.getYear();
    }

    public static String initials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase(ID);
    }

    public static String thousands(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        DecimalFormat df = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(ID));
        return df.format(value);
    }

    public static String moneyInput(BigDecimal value) {
        if (value == null || value.signum() == 0) {
            return "";
        }
        boolean fractional = value.stripTrailingZeros().scale() > 0;
        String pattern = "always".equals(CURRENCY_DECIMALS)
                || ("auto".equals(CURRENCY_DECIMALS) && fractional) ? "#,##0.00"
                : "never".equals(CURRENCY_DECIMALS) ? "#,##0" : "#,##0.##";
        DecimalFormat df = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(ID));
        return df.format(value);
    }

    public static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&':
                    out.append("&amp;");
                    break;
                case '<':
                    out.append("&lt;");
                    break;
                case '>':
                    out.append("&gt;");
                    break;
                case '"':
                    out.append("&quot;");
                    break;
                case '\'':
                    out.append("&#39;");
                    break;
                default:
                    out.append(c);
            }
        }
        return out.toString();
    }
}
