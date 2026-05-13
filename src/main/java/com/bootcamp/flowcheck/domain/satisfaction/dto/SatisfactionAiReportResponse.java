package com.bootcamp.flowcheck.domain.satisfaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SatisfactionAiReportResponse {

    // ── 1. 온라인 강의 분석 ────────────────────────────────────────────────────
    private double avgLectureDelivery;
    private double avgLectureMaterial;
    private double avgLectureQuality;
    private double avgLectureDifficulty;
    private double avgLectureNps;
    private String lectureAnalysis;          // Gemini 마크다운

    // ── 2. 개인과제 분석 ───────────────────────────────────────────────────────
    private double assignmentAvg;
    private double assignmentMedian;
    private int    assignmentMode;
    private String assignmentAnalysis;       // Gemini 마크다운

    // ── 3. 운영 및 서포트 SSC ──────────────────────────────────────────────────
    private String sscStart;
    private String sscStop;
    private String sscContinue;

    // ── 4. 커리큘럼 만족도 및 난이도 분석 ────────────────────────────────────
    private StatSummary curriculumSatisfactionStats;
    private StatSummary curriculumDifficultyStats;
    private String      curriculumConclusion;         // Gemini (I·II 결론)
    private String      studentRelationshipAnalysis;  // Gemini (III 마크다운 테이블)
    private List<DifficultyGroup> difficultyGroups;   // IV (백엔드 계산)
    private String      difficultyGroupInsights;      // Gemini (IV 시사점)
    private List<LowSatStudent>   lowSatStudents;     // V (백엔드 계산)
    private String      lowSatisfactionAnalysis;      // Gemini (V 분석 + 개선방안)

    // ── 내부 클래스 ───────────────────────────────────────────────────────────

    @Getter
    @AllArgsConstructor
    public static class StatSummary {
        private double avg;
        private double median;
        private int    mode;
    }

    @Getter
    @AllArgsConstructor
    public static class DifficultyGroup {
        private int    score;
        private String level;
        private int    count;
        private double avgSatisfaction;
    }

    @Getter
    @AllArgsConstructor
    public static class LowSatStudent {
        private String  name;
        private Integer curriculumSatisfaction;
        private Integer curriculumDifficulty;
    }
}
