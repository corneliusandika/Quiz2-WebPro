package com.dompetku.util;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MoneyParserTest {

    @Test
    public void parsesIndonesianFormat() {
        assertEquals(new BigDecimal("1500.50"), MoneyParser.parse("1.500,50"));
        assertEquals(new BigDecimal("1500.50"), MoneyParser.parse("1500,50"));
        assertEquals(new BigDecimal("1500"), MoneyParser.parse("1.500"));
    }

    @Test
    public void parsesInternationalFormat() {
        assertEquals(new BigDecimal("1500.50"), MoneyParser.parse("1500.50"));
        assertEquals(new BigDecimal("1500.50"), MoneyParser.parse("1,500.50"));
        assertEquals(new BigDecimal("1500"), MoneyParser.parse("Rp 1.500"));
    }

    @Test
    public void parsesPlainNumbers() {
        assertEquals(new BigDecimal("250000"), MoneyParser.parse("250000"));
        assertEquals(new BigDecimal("0"), MoneyParser.parse("0"));
    }

    @Test
    public void rejectsInvalidOrNegativeValues() {
        assertNull(MoneyParser.parse(null));
        assertNull(MoneyParser.parse(""));
        assertNull(MoneyParser.parse("-1500"));
        assertNull(MoneyParser.parse("bukan angka"));
    }
}
