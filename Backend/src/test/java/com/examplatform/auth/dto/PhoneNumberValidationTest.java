package com.examplatform.auth.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhoneNumberValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void acceptsInternationalNumberAndTrimsSurroundingWhitespace() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Student");
        request.setEmail("student@example.com");
        request.setPassword("long-password");
        request.setPhoneNumber("  +44 (20) 1234-5678  ");

        assertEquals("+44 (20) 1234-5678", request.getPhoneNumber());
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsMissingOrMalformedPhoneNumber() {
        RegisterRequest missing = new RegisterRequest();
        missing.setName("Student");
        missing.setEmail("student@example.com");
        missing.setPassword("long-password");

        RegisterRequest malformed = new RegisterRequest();
        malformed.setName("Student");
        malformed.setEmail("student@example.com");
        malformed.setPassword("long-password");
        malformed.setPhoneNumber("+91 abc 123");

        assertFalse(validator.validate(missing).isEmpty());
        assertFalse(validator.validate(malformed).isEmpty());
    }
}
