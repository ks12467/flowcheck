package com.bootcamp.flowcheck.domain.course.dto;

import com.bootcamp.flowcheck.domain.course.entity.Course;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CourseResponse {

    private Long courseId;
    private String name;
    private List<WeekResponse> weeks;

    public static CourseResponse of(Course course) {
        List<WeekResponse> weeks = course.getWeeks().stream()
                .map(WeekResponse::of)
                .toList();
        return new CourseResponse(course.getId(), course.getName(), weeks);
    }
}
