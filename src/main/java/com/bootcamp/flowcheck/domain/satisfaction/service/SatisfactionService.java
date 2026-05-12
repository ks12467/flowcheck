package com.bootcamp.flowcheck.domain.satisfaction.service;

import com.bootcamp.flowcheck.domain.satisfaction.dto.SatisfactionAnalysisResponse;
import com.bootcamp.flowcheck.domain.satisfaction.entity.SatisfactionResult;
import com.bootcamp.flowcheck.domain.satisfaction.repository.SatisfactionResultRepository;
import com.bootcamp.flowcheck.domain.track.entity.Track;
import com.bootcamp.flowcheck.domain.track.repository.TrackRepository;
import com.bootcamp.flowcheck.global.exception.BusinessException;
import com.bootcamp.flowcheck.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SatisfactionService {

    private final SatisfactionResultRepository satisfactionRepository;
    private final TrackRepository trackRepository;

    /** 업로드 이름 목록 반환 */
    @Transactional(readOnly = true)
    public List<String> getUploadNames(Long trackId) {
        return satisfactionRepository.findDistinctUploadNamesByTrackId(trackId);
    }

    /** 업로드 이름 변경 */
    public void renameUpload(Long trackId, String oldName, String newName) {
        if (!trackRepository.existsById(trackId)) {
            throw new BusinessException(ErrorCode.TRACK_NOT_FOUND);
        }
        if (satisfactionRepository.findDistinctUploadNamesByTrackId(trackId).contains(newName)) {
            throw new BusinessException(ErrorCode.SATISFACTION_DUPLICATE_UPLOAD_NAME);
        }
        int updated = satisfactionRepository.renameUploadName(trackId, oldName, newName);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.SATISFACTION_UPLOAD_NOT_FOUND);
        }
        log.info("[Satisfaction] 이름 변경: trackId={}, {} → {}", trackId, oldName, newName);
    }

    /** 업로드 데이터 삭제 */
    public void deleteUpload(Long trackId, String uploadName) {
        if (!trackRepository.existsById(trackId)) {
            throw new BusinessException(ErrorCode.TRACK_NOT_FOUND);
        }
        satisfactionRepository.deleteAllByTrackIdAndUploadName(trackId, uploadName);
        log.info("[Satisfaction] 업로드 삭제: trackId={}, uploadName={}", trackId, uploadName);
    }

    /** 엑셀 업로드 — 같은 트랙+업로드명은 덮어쓰기, 다른 업로드명은 누적, 저장된 행 수 반환 */
    public int upload(Long trackId, String uploadName, MultipartFile file) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));

        List<SatisfactionResult> results;
        try {
            results = parseExcel(file, track, uploadName);
        } catch (IOException e) {
            log.error("[Satisfaction] 엑셀 파싱 실패: trackId={}, uploadName={}, error={}",
                    trackId, uploadName, e.getMessage());
            throw new BusinessException(ErrorCode.SATISFACTION_PARSE_ERROR);
        }

        satisfactionRepository.deleteAllByTrackIdAndUploadName(trackId, uploadName);
        satisfactionRepository.saveAll(results);
        log.info("[Satisfaction] 업로드 완료: trackId={}, uploadName={}, count={}", trackId, uploadName, results.size());
        return results.size();
    }

    /** 분석 결과 반환 */
    @Transactional(readOnly = true)
    public SatisfactionAnalysisResponse analyze(Long trackId, String uploadName) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));

        List<SatisfactionResult> results =
                satisfactionRepository.findAllByTrack_IdAndUploadName(trackId, uploadName);
        if (results.isEmpty()) {
            return SatisfactionAnalysisResponse.builder()
                    .hasData(false)
                    .trackName(track.getName())
                    .build();
        }

        int total = results.size();
        LocalDateTime uploadedAt = results.stream()
                .map(SatisfactionResult::getUploadedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        // ── 커리큘럼 평균 ───────────────────────────────
        double avgCurriculumSatisfaction = avgOf(results, SatisfactionResult::getCurriculumSatisfaction);
        double avgCurriculumDifficulty   = avgOf(results, SatisfactionResult::getCurriculumDifficulty);

        // ── 강의 평균 ────────────────────────────────────
        double avgLectureDelivery        = avgOf(results, SatisfactionResult::getLectureDelivery);
        double avgLectureMaterial        = avgOf(results, SatisfactionResult::getLectureMaterial);
        double avgLectureQuality         = avgOf(results, SatisfactionResult::getLectureQuality);
        double avgLectureDifficulty      = avgOf(results, SatisfactionResult::getLectureDifficulty);
        double avgLectureNps             = avgOf(results, SatisfactionResult::getLectureNps);
        double avgAssignmentSatisfaction = avgOf(results, SatisfactionResult::getAssignmentSatisfaction);

        // ── 운영 평균 ────────────────────────────────────
        double avgScrumScore             = avgOf(results, SatisfactionResult::getScrumScore);
        double avgTutorHelpScore         = avgOf(results, SatisfactionResult::getTutorHelpScore);
        double avgTutorMeetScore         = avgOf(results, SatisfactionResult::getTutorMeetScore);
        double avgManagerScore           = avgOf(results, SatisfactionResult::getManagerScore);
        double avgFeedbackSpeedScore     = avgOf(results, SatisfactionResult::getFeedbackSpeedScore);
        double avgBriefScore             = avgOf(results, SatisfactionResult::getBriefScore);

        // ── 의견 수집 ────────────────────────────────────
        List<SatisfactionAnalysisResponse.CommentEntry> freeComments = results.stream()
                .filter(r -> r.getFreeComment() != null && !r.getFreeComment().isBlank())
                .map(r -> new SatisfactionAnalysisResponse.CommentEntry(
                        r.getRespondentName(), r.getFreeComment()))
                .toList();
        List<SatisfactionAnalysisResponse.CommentEntry> operationComments = results.stream()
                .filter(r -> r.getOperationComment() != null && !r.getOperationComment().isBlank())
                .map(r -> new SatisfactionAnalysisResponse.CommentEntry(
                        r.getRespondentName(), r.getOperationComment()))
                .toList();

        return SatisfactionAnalysisResponse.builder()
                .hasData(true)
                .totalCount(total)
                .trackName(track.getName())
                .uploadedAt(uploadedAt)
                .avgCurriculumSatisfaction(avgCurriculumSatisfaction)
                .avgCurriculumDifficulty(avgCurriculumDifficulty)
                .avgLectureDelivery(avgLectureDelivery)
                .avgLectureMaterial(avgLectureMaterial)
                .avgLectureQuality(avgLectureQuality)
                .avgLectureDifficulty(avgLectureDifficulty)
                .avgLectureNps(avgLectureNps)
                .avgAssignmentSatisfaction(avgAssignmentSatisfaction)
                .avgScrumScore(avgScrumScore)
                .avgTutorHelpScore(avgTutorHelpScore)
                .avgTutorMeetScore(avgTutorMeetScore)
                .avgManagerScore(avgManagerScore)
                .avgFeedbackSpeedScore(avgFeedbackSpeedScore)
                .avgBriefScore(avgBriefScore)
                .freeComments(freeComments)
                .operationComments(operationComments)
                .build();
    }

    // ── 파싱 ───────────────────────────────────────────────────────────────

    /** 헤더 키워드 → 컬럼 인덱스 맵 (첫 번째 매칭 우선) */
    private static final List<String[]> HEADER_KEYWORDS = List.of(
            new String[]{"respondentName",          "이름을 적어주세요"},
            new String[]{"curriculumSatisfaction",  "커리큘럼 만족도"},
            new String[]{"curriculumDifficulty",    "커리큘럼 난이도"},
            new String[]{"lectureDelivery",         "강의 전달력"},
            new String[]{"lectureMaterial",         "강의 교재"},
            new String[]{"lectureQuality",          "강의 품질"},
            new String[]{"lectureDifficulty",       "체감 난이도"},
            new String[]{"lectureNps",              "해당 강의를 얼마나 추천"},
            new String[]{"assignmentSatisfaction",  "과제에 대한 만족도"},
            new String[]{"scrumScore",              "[팀원들과 스크럼]"},
            new String[]{"tutorHelpScore",          "[튜터님들의 도움]"},
            new String[]{"tutorMeetScore",          "[튜터님들의 면담]"},
            new String[]{"managerScore",            "[담임매니저의 도움 및 면담]"},
            new String[]{"feedbackSpeedScore",      "[운영진의 피드백 속도]"},
            new String[]{"briefScore",              "[상세한 발제문]"},
            new String[]{"freeComment",             "자유롭게 이야기해주세요"},
            new String[]{"operationComment",        "운영진에게 해주고 싶은 말"}
    );

    private static Map<String, Integer> buildHeaderMap(Row headerRow) {
        Map<String, Integer> map = new LinkedHashMap<>();
        int lastCol = headerRow.getLastCellNum();

        for (int col = 0; col < lastCol; col++) {
            Cell cell = headerRow.getCell(col);
            if (cell == null) continue;
            String header = cell.getStringCellValue().trim();

            for (String[] kw : HEADER_KEYWORDS) {
                String field   = kw[0];
                String keyword = kw[1];
                if (!map.containsKey(field) && header.contains(keyword)) {
                    map.put(field, col);
                }
            }
        }
        return map;
    }

    private List<SatisfactionResult> parseExcel(MultipartFile file, Track track, String uploadName)
            throws IOException {
        List<SatisfactionResult> results = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() < 2) return results;

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return results;
            Map<String, Integer> colMap = buildHeaderMap(headerRow);

            log.debug("[Satisfaction] 헤더 매핑: {}", colMap);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String name = getString(row, colMap, "respondentName");
                if (name == null || name.isBlank()) continue; // 빈 행 스킵

                results.add(SatisfactionResult.builder()
                        .track(track)
                        .uploadName(uploadName)
                        .respondentName(name)
                        .curriculumSatisfaction(getInt(row, colMap, "curriculumSatisfaction"))
                        .curriculumDifficulty(getInt(row, colMap, "curriculumDifficulty"))
                        .lectureDelivery(getInt(row, colMap, "lectureDelivery"))
                        .lectureMaterial(getInt(row, colMap, "lectureMaterial"))
                        .lectureQuality(getInt(row, colMap, "lectureQuality"))
                        .lectureDifficulty(getInt(row, colMap, "lectureDifficulty"))
                        .lectureNps(getInt(row, colMap, "lectureNps"))
                        .assignmentSatisfaction(getInt(row, colMap, "assignmentSatisfaction"))
                        .scrumScore(getInt(row, colMap, "scrumScore"))
                        .tutorHelpScore(getInt(row, colMap, "tutorHelpScore"))
                        .tutorMeetScore(getInt(row, colMap, "tutorMeetScore"))
                        .managerScore(getInt(row, colMap, "managerScore"))
                        .feedbackSpeedScore(getInt(row, colMap, "feedbackSpeedScore"))
                        .briefScore(getInt(row, colMap, "briefScore"))
                        .freeComment(getString(row, colMap, "freeComment"))
                        .operationComment(getString(row, colMap, "operationComment"))
                        .uploadedAt(now)
                        .build());
            }
        }
        return results;
    }

    private static Integer getInt(Row row, Map<String, Integer> colMap, String field) {
        Integer col = colMap.get(field);
        if (col == null) return null;
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> {
                try { yield Integer.parseInt(cell.getStringCellValue().trim()); }
                catch (NumberFormatException e) { yield null; }
            }
            case FORMULA -> {
                try { yield (int) cell.getNumericCellValue(); }
                catch (Exception e) { yield null; }
            }
            default -> null;
        };
    }

    private static String getString(Row row, Map<String, Integer> colMap, String field) {
        Integer col = colMap.get(field);
        if (col == null) return null;
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> { String v = cell.getStringCellValue().trim(); yield v.isEmpty() ? null : v; }
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> null;
        };
    }

    private static double avgOf(List<SatisfactionResult> list,
                                 Function<SatisfactionResult, Integer> getter) {
        return round1(list.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0));
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
