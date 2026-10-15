package com.example.servicehub_api.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CPFValidatorTest {

    private final CPFValidator validator = new CPFValidator();

    @Test
    void acceptsValidCpfWithAndWithoutPunctuation() {
        assertTrue(validator.isValid("529.982.247-25", null));
        assertTrue(validator.isValid("52998224725", null));
    }

    @Test
    void rejectsInvalidRepeatedAndNonNumericCpf() {
        assertFalse(validator.isValid("529.982.247-26", null));
        assertFalse(validator.isValid("111.111.111-11", null));
        assertFalse(validator.isValid("529abc24725", null));
    }
}
