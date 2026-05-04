package com.bootcamp.flowcheck.domain.course.controller;

import com.bootcamp.flowcheck.domain.course.dto.*;
import com.bootcamp.flowcheck.domain.course.service.CourseService;
import com.bootcamp.flowcheck.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tracks/{trackId}/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getCourses(@PathVariable Long trackId) {
        return ResponseEntity.ok(ApiResponse.success(courseService.getCourses(trackId), "강의 목록을 조회했습니다."));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(
            @PathVariable Long trackId,
            @RequestBody @Valid CourseCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(courseService.createCourse(trackId, request), "강의를 등록했습니다."));
    }

    @PutMapping("/{courseId}")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(
            @PathVariable Long trackId,
            @PathVariable Long courseId,
            @RequestBody @Valid CourseUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(courseService.updateCourse(trackId, courseId, request), "강의를 수정했습니다."));
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(
            @PathVariable Long trackId,
            @PathVariable Long courseId) {
        courseService.deleteCourse(trackId, courseId);
        return ResponseEntity.ok(ApiResponse.success(null, "강의를 삭제했습니다."));
    }

    @PostMapping("/{courseId}/weeks")
    public ResponseEntity<ApiResponse<WeekResponse>> addWeek(
            @PathVariable Long trackId,
            @PathVariable Long courseId,
            @RequestBody @Valid WeekCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(courseService.addWeek(trackId, courseId, request), "주차를 추가했습니다."));
    }
}
