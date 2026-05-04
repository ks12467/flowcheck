package com.bootcamp.flowcheck.domain.student.dto;

import com.bootcamp.flowcheck.domain.progress.entity.LearningProgress;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class LatestProgressResponse {

    private Integer conditionScore;
    private String riskLevel;
    private Integer assignmentProgress;
    private Boolean tilWritten;
    private String comment;
    private LocalDateTime submittedAt;

    public static LatestProgressResponse of(LearningProgress lp) {
        return new LatestProgressResponse(
                lp.getConditionScore(),
                lp.getRiskLevel(),
                lp.getAssignmentProgress(),
                lp.getTilWritten(),
                lp.getComment(),
                lp.getSubmittedAt()
        );
    }
}
