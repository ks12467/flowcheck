package com.bootcamp.flowcheck.domain.survey.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class SurveyQuestionsUpdateRequest {

    private List<QuestionUpdate> questions;

    @Getter
    @NoArgsConstructor
    public static class QuestionUpdate {
        private Long id;
        private String questionText;
    }
}
