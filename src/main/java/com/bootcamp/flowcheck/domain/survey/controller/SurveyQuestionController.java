package com.bootcamp.flowcheck.domain.survey.controller;

import com.bootcamp.flowcheck.domain.survey.dto.SurveyQuestionResponse;
import com.bootcamp.flowcheck.domain.survey.dto.SurveyQuestionsUpdateRequest;
import com.bootcamp.flowcheck.domain.survey.service.SurveyQuestionService;
import com.bootcamp.flowcheck.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/survey")
@RequiredArgsConstructor
public class SurveyQuestionController {

    private final SurveyQuestionService surveyQuestionService;

    @GetMapping("/questions")
    public ResponseEntity<ApiResponse<List<SurveyQuestionResponse>>> getQuestions(
            @RequestParam Long trackId) {
        return ResponseEntity.ok(
                ApiResponse.success(surveyQuestionService.getQuestions(trackId), "문항 조회 성공"));
    }

    @PutMapping("/questions")
    public ResponseEntity<ApiResponse<List<SurveyQuestionResponse>>> updateQuestions(
            @RequestParam Long trackId,
            @RequestBody SurveyQuestionsUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(surveyQuestionService.updateQuestions(trackId, request), "문항 수정 성공"));
    }
}
