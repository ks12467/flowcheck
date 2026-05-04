package com.bootcamp.flowcheck.domain.progress.dto;

import com.bootcamp.flowcheck.domain.progress.entity.LearningProgress;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class LearningProgressResponse {

    private Long studentId;
    private String studentName;
    private String riskLevel;
    private Integer conditionScore;
    private Integer assignmentProgress;
    private Boolean tilWritten;
    private String courseName;
    private Integer weekNumber;
    private Integer lectureNumber;
    private String status;
    private String comment;
    private LocalDateTime submittedAt;

    public static LearningProgressResponse of(LearningProgress lp) {
        return new LearningProgressResponse(
                lp.getStudent().getId(),
                lp.getStudent().getName(),
                lp.getRiskLevel(),
                lp.getConditionScore(),
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
