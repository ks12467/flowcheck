package com.bootcamp.flowcheck.domain.survey.dto;

import com.bootcamp.flowcheck.domain.survey.entity.SurveyQuestion;
import lombok.Getter;

@Getter
public class SurveyQuestionResponse {

    private final Long id;
    private final Integer questionOrder;
    private final String questionText;
    private final String questionType;
    private final Integer minScore;
    private final Integer maxScore;

    private SurveyQuestionResponse(Long id, Integer questionOrder, String questionText,
                                   String questionType, Integer minScore, Integer maxScore) {
        this.id = id;
        this.questionOrder = questionOrder;
        this.questionText = questionText;
        this.questionType = questionType;
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public static SurveyQuestionResponse of(SurveyQuestion q) {
        return new SurveyQuestionResponse(
                q.getId(), q.getQuestionOrder(), q.getQuestionText(),
                q.getQuestionType().name(), q.getMinScore(), q.getMaxScore());
    }
}
