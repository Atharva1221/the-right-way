package com.examplatform.question.dto;

import com.examplatform.question.entity.QuestionOption;

public record QuestionOptionResponse(
        Long id,
        String optionText,
        boolean correct,
        Integer optionOrder
) {
    public static QuestionOptionResponse from(QuestionOption option) {
        return new QuestionOptionResponse(
                option.getId(),
                option.getOptionText(),
                option.isCorrect(),
                option.getOptionOrder());
    }
}
