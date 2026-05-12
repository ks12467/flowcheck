package com.bootcamp.flowcheck.domain.satisfaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class SatisfactionAnalysisResponse {

    private boolean hasData;
    private int totalCount;
    private String trackName;
    private LocalDateTime uploadedAt;

    // ── 커리큘럼 ──────────────────────────────────────────
    private double avgCurriculumSatisfaction;
    private double avgCurriculumDifficulty;

    // ── 강의 ──────────────────────────────────────────────
    private double avgLectureDelivery;
    private double avgLectureMaterial;
    private double avgLectureQuality;
    private double avgLectureDifficulty;
    private double avgLectureNps;
    private double avgAssignmentSatisfaction;

    // ── 운영 ──────────────────────────────────────────────
    private double avgScrumScore;
    private double avgTutorHelpScore;
    private double avgTutorMeetScore;
    private double avgManagerScore;
    private double avgFeedbackSpeedScore;
    private double avgBriefScore;

    // ── 의견 ──────────────────────────────────────────────
    private List<CommentEntry> freeComments;
    private List<CommentEntry> operationComments;

    @Getter
    @AllArgsConstructor
    public static class CommentEntry {
        private String name;
        private String comment;
    }
}
