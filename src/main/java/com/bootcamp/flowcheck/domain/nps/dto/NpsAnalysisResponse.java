package com.bootcamp.flowcheck.domain.nps.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class NpsAnalysisResponse {

    private boolean hasData;
    private int totalCount;
    private String trackName;
    private LocalDateTime uploadedAt;

    // 항목별 평균 (1~10 척도)
    private double avgDifficulty;
    private double avgSkill;
    private double avgGrowth;
    private double avgCommunication;
    private double avgImmersion;
    private double avgOpsSatisfaction;
    private double avgCurriculumSatisfaction;

    // NPS 분포
    private int promoters;       // 9~10점
    private int passives;        // 7~8점
    private int detractors;      // 0~6점
    private double promoterPct;
    private double passivePct;
    private double detractorPct;
    private double npsScore;     // (추천-비추천)/전체*100

    // 상승/하락 인원 목록
    private List<PersonScore> promoterList;
    private List<PersonScore> detractorList;

    // 6점 이하 수강생
    private List<LowScoreStudent> lowScoreStudents;

    // 코멘트
    private List<String> npsComments;
    private List<String> opsComments;

    // 수강생별 전체 점수 + 코멘트 (팝업용)
    private List<StudentDetail> studentDetails;

    @Getter
    @AllArgsConstructor
    public static class PersonScore {
        private String name;
        private int nps;
    }

    @Getter
    @AllArgsConstructor
    public static class LowScoreStudent {
        private String name;
        private List<String> lowItems; // e.g. ["소통: 5", "몰입: 4"]
    }

    @Getter
    @AllArgsConstructor
    public static class StudentDetail {
        private String name;
        private Integer difficulty;
        private Integer skill;
        private Integer growth;
        private Integer communication;
        private Integer immersion;
        private Integer opsSatisfaction;
        private Integer curriculumSatisfaction;
        private Integer nps;
        private String opsComment;
        private String npsComment;
    }
}
