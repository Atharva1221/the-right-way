package com.examplatform.question.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class QuestionRequestValidationTest {

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
    void acceptsQuestionWithValidOptions() {
        assertTrue(validator.validate(validRequest()).isEmpty());
    }

    @Test
    void rejectsBlankQuestionAndOptionText() {
        CreateQuestionRequest request = new CreateQuestionRequest(
                " ",
                null,
                1,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                List.of(
                        new QuestionOptionRequest(" ", true, 1),
                        new QuestionOptionRequest("Second", false, 2)));

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsFewerThanTwoOptionsAndIncorrectCorrectAnswerCount() {
        CreateQuestionRequest tooFew = new CreateQuestionRequest(
                "Question",
                null,
                1,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                List.of(new QuestionOptionRequest("Only option", true, 1)));
        CreateQuestionRequest noCorrectAnswer = new CreateQuestionRequest(
                "Question",
                null,
                1,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                List.of(
                        new QuestionOptionRequest("First", false, 1),
                        new QuestionOptionRequest("Second", false, 2)));
        CreateQuestionRequest multipleCorrectAnswers = new CreateQuestionRequest(
                "Question",
                null,
                1,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                List.of(
                        new QuestionOptionRequest("First", true, 1),
                        new QuestionOptionRequest("Second", true, 2)));

        assertFalse(validator.validate(tooFew).isEmpty());
        assertFalse(validator.validate(noCorrectAnswer).isEmpty());
        assertFalse(validator.validate(multipleCorrectAnswers).isEmpty());
    }

    @Test
    void rejectsNegativeMarksAndNonPositiveOrDuplicateOrders() {
        CreateQuestionRequest request = new CreateQuestionRequest(
                "Question",
                null,
                0,
                BigDecimal.ONE.negate(),
                BigDecimal.ZERO,
                List.of(
                        new QuestionOptionRequest("First", true, 1),
                        new QuestionOptionRequest("Second", false, 1)));

        assertFalse(validator.validate(request).isEmpty());
    }

    private CreateQuestionRequest validRequest() {
        return new CreateQuestionRequest(
                "Question",
                "Explanation",
                1,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                List.of(
                        new QuestionOptionRequest("First", true, 1),
                        new QuestionOptionRequest("Second", false, 2)));
    }
}
