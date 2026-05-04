package com.bootcamp.flowcheck.domain.progress.dto;

import com.bootcamp.flowcheck.domain.progress.entity.LearningProgress;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class LearningProgressHistoryResponse {

    private Long progressId;
    private Integer conditionScore;
    private String riskLevel;
    private Integer assignmentProgress;
    private Boolean tilWritten;
    private String courseName;
    private Integer weekNumber;
    private Integer lectureNumber;
    private String status;
    private String comment;
    private LocalDateTime submittedAt;

    public static LearningProgressHistoryResponse of(LearningProgress lp) {
        return new LearningProgressHistoryResponse(
                lp.getId(),
                lp.getConditionScore(),
                lp.getRiskLevel(),
                lp.getAssignmentProgress(),
                lp.getTilWritten(),
                lp.getCourseWeek().getCourse().getName(),
                lp.getCourseWeek().getWeekNumber(),
                lp.getLectureNumber(),
                lp.getStatus(),
                lp.getComment(),
                lp.getSubmittedAt()
        );
    }
}
