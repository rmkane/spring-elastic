package com.example.springelastic.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class StringHelperTest {

    @Test
    void trimToNull_null_returnsNull() {
        assertNull(StringHelper.trimToNull(null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  ", "\t", "\n", "\r\n"})
    void trimToNull_blank_returnsNull(String input) {
        assertNull(StringHelper.trimToNull(input));
    }

    @Test
    void trimToNull_noBreakSpace_notStrippedByJdk() {
        assertEquals("\u00a0", StringHelper.trimToNull("\u00a0"));
    }

    @Test
    void trimToNull_nonBlank_returnsTrimmed() {
        assertEquals("a", StringHelper.trimToNull("a"));
        assertEquals("hello", StringHelper.trimToNull("  hello  "));
        assertEquals("x y", StringHelper.trimToNull("\tx y\n"));
    }
}
