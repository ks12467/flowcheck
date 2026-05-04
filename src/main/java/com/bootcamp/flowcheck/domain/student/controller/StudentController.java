package com.bootcamp.flowcheck.domain.student.controller;

import com.bootcamp.flowcheck.domain.student.dto.*;
import com.bootcamp.flowcheck.domain.student.service.StudentService;
import com.bootcamp.flowcheck.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tracks/{trackId}/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<StudentResponse>>> getStudents(
            @PathVariable Long trackId,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success(
                studentService.getStudents(trackId, riskLevel, keyword), "수강생 목록을 조회했습니다."));
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<ApiResponse<StudentDetailResponse>> getStudent(
            @PathVariable Long trackId,
            @PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.success(
                studentService.getStudent(trackId, studentId), "수강생을 조회했습니다."));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StudentResponse>> createStudent(
            @PathVariable Long trackId,
            @RequestBody @Valid StudentCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                studentService.createStudent(trackId, request), "수강생을 등록했습니다."));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<BulkCreateResponse>> bulkCreateStudents(
            @PathVariable Long trackId,
            @RequestBody @Valid StudentBulkCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                studentService.bulkCreateStudents(trackId, request), "수강생 일괄 등록을 완료했습니다."));
    }

    @PutMapping("/{studentId}")
    public ResponseEntity<ApiResponse<StudentResponse>> updateStudent(
            @PathVariable Long trackId,
            @PathVariable Long studentId,
            @RequestBody @Valid StudentUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                studentService.updateStudent(trackId, studentId, request), "수강생 정보를 수정했습니다."));
    }

    @DeleteMapping("/{studentId}")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(
            @PathVariable Long trackId,
            @PathVariable Long studentId) {
        studentService.deleteStudent(trackId, studentId);
        return ResponseEntity.ok(ApiResponse.success(null, "수강생을 삭제했습니다."));
    }
}
