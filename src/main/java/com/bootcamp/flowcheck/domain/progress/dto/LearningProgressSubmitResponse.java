package com.bootcamp.flowcheck.domain.progress.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class LearningProgressSubmitResponse {

    private int similarStudentPercent;
    private List<CaseStats> topCases;
    private CaseStats myCase;

    @Getter
    @AllArgsConstructor
    public static class CaseStats {
        private int conditionScore;
        private long count;
        private String label;
    }
}
