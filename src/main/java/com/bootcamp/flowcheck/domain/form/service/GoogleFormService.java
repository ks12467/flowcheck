package com.bootcamp.flowcheck.domain.form.service;

import com.bootcamp.flowcheck.domain.form.dto.GoogleFormRequest;
import com.bootcamp.flowcheck.domain.form.dto.GoogleFormResponse;
import com.bootcamp.flowcheck.domain.form.entity.GoogleForm;
import com.bootcamp.flowcheck.domain.form.repository.GoogleFormRepository;
import com.bootcamp.flowcheck.domain.track.entity.Track;
import com.bootcamp.flowcheck.domain.track.repository.TrackRepository;
import com.bootcamp.flowcheck.global.exception.BusinessException;
import com.bootcamp.flowcheck.global.exception.ErrorCode;
import com.bootcamp.flowcheck.global.sheets.GoogleSheetsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GoogleFormService {

    private final GoogleFormRepository googleFormRepository;
    private final TrackRepository trackRepository;
    private final GoogleSheetsService googleSheetsService;

    @Transactional(readOnly = true)
    public List<GoogleFormResponse> getAllForms() {
        return googleFormRepository.findAll().stream()
                .map(GoogleFormResponse::of)
                .toList();
    }

    public GoogleFormResponse createForm(GoogleFormRequest req) {
        Track track = trackRepository.findById(req.getTrackId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));
        GoogleForm form = GoogleForm.builder()
                .name(req.getName())
                .formUrl(req.getFormUrl())
                .spreadsheetId(req.getSpreadsheetId())
                .scoreColumnHeader(req.getScoreColumnHeader())
                .track(track)
                .createdAt(LocalDateTime.now())
                .build();
        GoogleForm saved = googleFormRepository.save(form);

        Boolean shareResult = null;
        String serviceAccountEmail = null;
        if (req.getSpreadsheetId() != null && !req.getSpreadsheetId().isBlank()) {
            serviceAccountEmail = googleSheetsService.getServiceAccountEmail();
            shareResult = googleSheetsService.shareSpreadsheetWithServiceAccount(req.getSpreadsheetId());
        }
        return GoogleFormResponse.ofWithShare(saved, shareResult, serviceAccountEmail);
    }

    public GoogleFormResponse updateForm(Long id, GoogleFormRequest req) {
        GoogleForm form = googleFormRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOOGLE_FORM_NOT_FOUND));
        Track track = trackRepository.findById(req.getTrackId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));
        form.update(req.getName(), req.getFormUrl(), req.getSpreadsheetId(),
                req.getScoreColumnHeader(), track);

        Boolean shareResult = null;
        String serviceAccountEmail = null;
        if (req.getSpreadsheetId() != null && !req.getSpreadsheetId().isBlank()) {
            serviceAccountEmail = googleSheetsService.getServiceAccountEmail();
            shareResult = googleSheetsService.shareSpreadsheetWithServiceAccount(req.getSpreadsheetId());
        }
        return GoogleFormResponse.ofWithShare(form, shareResult, serviceAccountEmail);
    }

    public void deleteForm(Long id) {
        GoogleForm form = googleFormRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOOGLE_FORM_NOT_FOUND));
        form.softDelete();
    }

    @Transactional(readOnly = true)
    public int getResponseCount(Long formId) {
        GoogleForm form = googleFormRepository.findById(formId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOOGLE_FORM_NOT_FOUND));
        return googleSheetsService.getResponseCount(form.getSpreadsheetId());
    }

    @Transactional(readOnly = true)
    public double getAverageScore(Long formId) {
        GoogleForm form = googleFormRepository.findById(formId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOOGLE_FORM_NOT_FOUND));
        if (form.getSpreadsheetId() == null || form.getScoreColumnHeader() == null) return -1;
        return googleSheetsService.getAverageScore(form.getSpreadsheetId(), form.getScoreColumnHeader());
    }

    /** @return int[]{highCount(9~10), midCount(7~8), lowCount(0~6)}, scoreColumnHeader 없거나 오류 시 null */
    @Transactional(readOnly = true)
    public int[] getScoreDistribution(Long formId) {
        GoogleForm form = googleFormRepository.findById(formId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOOGLE_FORM_NOT_FOUND));
        if (form.getSpreadsheetId() == null || form.getScoreColumnHeader() == null) return null;
        return googleSheetsService.getScoreDistribution(form.getSpreadsheetId(), form.getScoreColumnHeader());
    }
}
