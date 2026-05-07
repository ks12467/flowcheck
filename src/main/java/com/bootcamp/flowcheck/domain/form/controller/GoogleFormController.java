package com.bootcamp.flowcheck.domain.form.controller;

import com.bootcamp.flowcheck.domain.form.dto.GoogleFormRequest;
import com.bootcamp.flowcheck.domain.form.dto.GoogleFormResponse;
import com.bootcamp.flowcheck.domain.form.service.GoogleFormService;
import com.bootcamp.flowcheck.global.response.ApiResponse;
import com.bootcamp.flowcheck.global.sheets.GoogleSheetsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/forms")
@RequiredArgsConstructor
public class GoogleFormController {

    private final GoogleFormService googleFormService;
    private final GoogleSheetsService googleSheetsService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<GoogleFormResponse>>> getForms() {
        return ResponseEntity.ok(ApiResponse.success(googleFormService.getAllForms(), "구글폼 목록을 조회했습니다."));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createForm(
            @RequestBody @Valid GoogleFormRequest req) {
        GoogleFormResponse form = googleFormService.createForm(req);
        boolean shared = false;
        if (req.getSpreadsheetId() != null && !req.getSpreadsheetId().isBlank()) {
            shared = googleSheetsService.shareSpreadsheetWithServiceAccount(req.getSpreadsheetId());
        }
        Map<String, Object> result = buildResult(form, shared);
        return ResponseEntity.ok(ApiResponse.success(result, "구글폼이 등록되었습니다."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateForm(
            @PathVariable Long id,
            @RequestBody @Valid GoogleFormRequest req) {
        GoogleFormResponse form = googleFormService.updateForm(id, req);
        boolean shared = false;
        if (req.getSpreadsheetId() != null && !req.getSpreadsheetId().isBlank()) {
            shared = googleSheetsService.shareSpreadsheetWithServiceAccount(req.getSpreadsheetId());
        }
        Map<String, Object> result = buildResult(form, shared);
        return ResponseEntity.ok(ApiResponse.success(result, "구글폼이 수정되었습니다."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteForm(@PathVariable Long id) {
        googleFormService.deleteForm(id);
        return ResponseEntity.ok(ApiResponse.success(null, "구글폼이 삭제되었습니다."));
    }

    @GetMapping("/{id}/responses")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getFormResponses(@PathVariable Long id) {
        int count = googleFormService.getResponseCount(id);
        String message = count >= 0 ? "응답 수를 조회했습니다." : "스프레드시트에 접근할 수 없습니다.";
        return ResponseEntity.ok(ApiResponse.success(Map.of("responseCount", count), message));
    }

    @GetMapping("/service-account-email")
    public ResponseEntity<ApiResponse<Map<String, String>>> getServiceAccountEmail() {
        String email = googleSheetsService.getServiceAccountEmail();
        if (email == null) {
            return ResponseEntity.ok(ApiResponse.success(Map.of(), "서비스 계정 정보를 확인할 수 없습니다. credentials.json을 확인해주세요."));
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of("email", email), "서비스 계정 이메일을 조회했습니다."));
    }

    private Map<String, Object> buildResult(GoogleFormResponse form, boolean shared) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("formId", form.getFormId());
        result.put("name", form.getName());
        result.put("trackId", form.getTrackId());
        result.put("trackName", form.getTrackName());
        result.put("formUrl", form.getFormUrl());
        result.put("spreadsheetId", form.getSpreadsheetId());
        result.put("scoreColumnHeader", form.getScoreColumnHeader());
        result.put("sharedWithServiceAccount", shared);
        result.put("serviceAccountEmail", googleSheetsService.getServiceAccountEmail());
        return result;
    }
}
