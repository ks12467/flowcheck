package com.bootcamp.flowcheck.domain.form.controller;

import com.bootcamp.flowcheck.domain.form.dto.GoogleFormRequest;
import com.bootcamp.flowcheck.domain.form.dto.GoogleFormResponse;
import com.bootcamp.flowcheck.domain.form.service.GoogleFormService;
import com.bootcamp.flowcheck.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/forms")
@RequiredArgsConstructor
public class GoogleFormController {

    private final GoogleFormService googleFormService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<GoogleFormResponse>>> getForms() {
        return ResponseEntity.ok(ApiResponse.success(googleFormService.getAllForms(), "구글폼 목록을 조회했습니다."));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GoogleFormResponse>> createForm(
            @RequestBody @Valid GoogleFormRequest req) {
        return ResponseEntity.ok(ApiResponse.success(googleFormService.createForm(req), "구글폼이 등록되었습니다."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GoogleFormResponse>> updateForm(
            @PathVariable Long id,
            @RequestBody @Valid GoogleFormRequest req) {
        return ResponseEntity.ok(ApiResponse.success(googleFormService.updateForm(id, req), "구글폼이 수정되었습니다."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteForm(@PathVariable Long id) {
        googleFormService.deleteForm(id);
        return ResponseEntity.ok(ApiResponse.success(null, "구글폼이 삭제되었습니다."));
    }
}
