package com.bootcamp.flowcheck.domain.progress.dto;

import com.bootcamp.flowcheck.domain.course.dto.CourseResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ProgressFormResponse {

    private String trackName;
    private List<CourseResponse> courses;
}
