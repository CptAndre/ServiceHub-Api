package com.example.servicehub_api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CPFValidator implements ConstraintValidator<CPF, String> {

    @Override
    public boolean isValid(String cpf, ConstraintValidatorContext context) {
        if (cpf == null) {
            return true;
        }
        if (!cpf.matches("[0-9.\\-\\s]+")) {
            return false;
        }

        String digits = cpf.replaceAll("\\D", "");
        if (digits.length() != 11 || digits.chars().distinct().count() == 1) {
            return false;
        }

        int firstDigit = checkDigit(digits, 9);
        int secondDigit = checkDigit(digits, 10);
        return digits.charAt(9) - '0' == firstDigit
                && digits.charAt(10) - '0' == secondDigit;
    }

    private int checkDigit(String digits, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (digits.charAt(i) - '0') * (length + 1 - i);
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
