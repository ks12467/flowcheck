package com.bootcamp.flowcheck.domain.course.dto;

import com.bootcamp.flowcheck.domain.course.entity.CourseWeek;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WeekResponse {

    private Long weekId;
    private Integer weekNumber;
    private Integer lectureCount;

    public static WeekResponse of(CourseWeek week) {
        return new WeekResponse(week.getId(), week.getWeekNumber(), week.getLectureCount());
    }
}
