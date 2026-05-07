package com.bootcamp.flowcheck.domain.form.service;

import com.bootcamp.flowcheck.domain.form.dto.GoogleFormRequest;
import com.bootcamp.flowcheck.domain.form.dto.GoogleFormResponse;
import com.bootcamp.flowcheck.domain.form.entity.GoogleForm;
import com.bootcamp.flowcheck.domain.form.repository.GoogleFormRepository;
import com.bootcamp.flowcheck.domain.track.entity.Track;
import com.bootcamp.flowcheck.domain.track.repository.TrackRepository;
import com.bootcamp.flowcheck.global.exception.BusinessException;
import com.bootcamp.flowcheck.global.exception.ErrorCode;
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
        return GoogleFormResponse.of(googleFormRepository.save(form));
    }

    public GoogleFormResponse updateForm(Long id, GoogleFormRequest req) {
        GoogleForm form = googleFormRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOOGLE_FORM_NOT_FOUND));
        Track track = trackRepository.findById(req.getTrackId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));
        form.update(req.getName(), req.getFormUrl(), req.getSpreadsheetId(),
                req.getScoreColumnHeader(), track);
        return GoogleFormResponse.of(form);
    }

    public void deleteForm(Long id) {
        GoogleForm form = googleFormRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOOGLE_FORM_NOT_FOUND));
        form.softDelete();
    }
}
