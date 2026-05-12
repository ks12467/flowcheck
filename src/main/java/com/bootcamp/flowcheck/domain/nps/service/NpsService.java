package com.bootcamp.flowcheck.domain.nps.service;

import com.bootcamp.flowcheck.domain.nps.dto.NpsAnalysisResponse;
import com.bootcamp.flowcheck.domain.nps.entity.NpsResult;
import com.bootcamp.flowcheck.domain.nps.repository.NpsResultRepository;
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
public class NpsService {

    private final NpsResultRepository npsResultRepository;
    private final TrackRepository trackRepository;

    /** 업로드 이름 목록 반환 */
    @Transactional(readOnly = true)
    public List<String> getUploadNames(Long trackId) {
        return npsResultRepository.findDistinctUploadNamesByTrackId(trackId);
    }

    /** 업로드 이름 변경 */
    public void renameUpload(Long trackId, String oldName, String newName) {
        if (!trackRepository.existsById(trackId)) {
            throw new BusinessException(ErrorCode.TRACK_NOT_FOUND);
        }
        if (npsResultRepository.findDistinctUploadNamesByTrackId(trackId).contains(newName)) {
            throw new BusinessException(ErrorCode.NPS_DUPLICATE_UPLOAD_NAME);
        }
        int updated = npsResultRepository.renameUploadName(trackId, oldName, newName);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NPS_UPLOAD_NOT_FOUND);
        }
        log.info("[NPS] 이름 변경: trackId={}, {} → {}", trackId, oldName, newName);
    }

    /** 업로드 데이터 삭제 */
    public void deleteUpload(Long trackId, String uploadName) {
        if (!trackRepository.existsById(trackId)) {
            throw new BusinessException(ErrorCode.TRACK_NOT_FOUND);
        }
        npsResultRepository.deleteAllByTrackIdAndUploadName(trackId, uploadName);
        log.info("[NPS] 업로드 삭제: trackId={}, uploadName={}", trackId, uploadName);
    }

    /** 엑셀 업로드 — 같은 트랙+업로드명은 덮어쓰기, 다른 업로드명은 누적, 저장된 행 수 반환 */
    public int upload(Long trackId, String uploadName, MultipartFile file) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));

        List<NpsResult> results;
        try {
            results = parseExcel(file, track, uploadName);
        } catch (IOException e) {
            log.error("[NPS] 엑셀 파싱 실패: trackId={}, uploadName={}, error={}", trackId, uploadName, e.getMessage());
            throw new BusinessException(ErrorCode.NPS_PARSE_ERROR);
        }

        npsResultRepository.deleteAllByTrackIdAndUploadName(trackId, uploadName);
        npsResultRepository.saveAll(results);
        log.info("[NPS] 업로드 완료: trackId={}, uploadName={}, count={}", trackId, uploadName, results.size());
        return results.size();
    }

    /** 트랙+업로드명 NPS 분석 결과 반환 */
    @Transactional(readOnly = true)
    public NpsAnalysisResponse analyze(Long trackId, String uploadName) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));

        List<NpsResult> results = npsResultRepository.findAllByTrack_IdAndUploadNameOrderByNameAsc(trackId, uploadName);
        if (results.isEmpty()) {
            return NpsAnalysisResponse.builder()
                    .hasData(false)
                    .trackName(track.getName())
                    .build();
        }

        int total = results.size();
        LocalDateTime uploadedAt = results.stream()
                .map(NpsResult::getUploadedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        // ── 항목별 평균 ────────────────────────────────
        double avgDifficulty           = avgOf(results, NpsResult::getDifficulty);
        double avgSkill                = avgOf(results, NpsResult::getSkill);
        double avgGrowth               = avgOf(results, NpsResult::getGrowth);
        double avgCommunication        = avgOf(results, NpsResult::getCommunication);
        double avgImmersion            = avgOf(results, NpsResult::getImmersion);
        double avgOpsSatisfaction      = avgOf(results, NpsResult::getOpsSatisfaction);
        double avgCurriculumSatisfaction = avgOf(results, NpsResult::getCurriculumSatisfaction);

        // ── NPS 분포 ──────────────────────────────────
        List<NpsResult> withNps = results.stream()
                .filter(r -> r.getNps() != null).toList();
        int npsCount = withNps.size();

        List<NpsResult> promoterResults  = withNps.stream().filter(r -> r.getNps() >= 9).toList();
        List<NpsResult> passiveResults   = withNps.stream().filter(r -> r.getNps() >= 7 && r.getNps() <= 8).toList();
        List<NpsResult> detractorResults = withNps.stream().filter(r -> r.getNps() <= 6).toList();

        int promoters  = promoterResults.size();
        int passives   = passiveResults.size();
        int detractors = detractorResults.size();

        double promoterPct  = npsCount > 0 ? round1(promoters  * 100.0 / npsCount) : 0;
        double passivePct   = npsCount > 0 ? round1(passives   * 100.0 / npsCount) : 0;
        double detractorPct = npsCount > 0 ? round1(detractors * 100.0 / npsCount) : 0;
        double npsScore     = npsCount > 0 ? round1((promoters - detractors) * 100.0 / npsCount) : 0;

        // ── 상승/하락 인원 목록 ────────────────────────
        List<NpsAnalysisResponse.PersonScore> promoterList = promoterResults.stream()
                .map(r -> new NpsAnalysisResponse.PersonScore(r.getName(), r.getNps()))
                .sorted(Comparator.comparingInt(NpsAnalysisResponse.PersonScore::getNps).reversed())
                .toList();

        List<NpsAnalysisResponse.PersonScore> detractorList = detractorResults.stream()
                .map(r -> new NpsAnalysisResponse.PersonScore(r.getName(), r.getNps()))
                .sorted(Comparator.comparingInt(NpsAnalysisResponse.PersonScore::getNps))
                .toList();

        // ── 6점 이하 수강생 ────────────────────────────
        List<String> itemNames = List.of(
                "난이도", "실력", "성장", "소통", "몰입", "운영 만족도", "커리큘럼 만족도");
        List<Function<NpsResult, Integer>> getters = List.of(
                NpsResult::getDifficulty, NpsResult::getSkill, NpsResult::getGrowth,
                NpsResult::getCommunication, NpsResult::getImmersion,
                NpsResult::getOpsSatisfaction, NpsResult::getCurriculumSatisfaction);

        List<NpsAnalysisResponse.LowScoreStudent> lowScoreStudents = results.stream()
                .map(r -> {
                    List<String> low = new ArrayList<>();
                    for (int i = 0; i < itemNames.size(); i++) {
                        Integer v = getters.get(i).apply(r);
                        if (v != null && v <= 6) low.add(itemNames.get(i) + ": " + v);
                    }
                    return low.isEmpty() ? null
                            : new NpsAnalysisResponse.LowScoreStudent(r.getName(), low);
                })
                .filter(Objects::nonNull)
                .toList();

        // ── 코멘트 수집 ────────────────────────────────
        List<String> npsComments = results.stream()
                .map(NpsResult::getNpsComment)
                .filter(c -> c != null && !c.isBlank())
                .toList();
        List<String> opsComments = results.stream()
                .map(NpsResult::getOpsComment)
                .filter(c -> c != null && !c.isBlank())
                .toList();

        // ── 수강생 전체 상세 (팝업용) ───────────────────
        List<NpsAnalysisResponse.StudentDetail> studentDetails = results.stream()
                .map(r -> new NpsAnalysisResponse.StudentDetail(
                        r.getName(),
                        r.getDifficulty(), r.getSkill(), r.getGrowth(),
                        r.getCommunication(), r.getImmersion(),
                        r.getOpsSatisfaction(), r.getCurriculumSatisfaction(),
                        r.getNps(),
                        r.getOpsComment(), r.getNpsComment()))
                .toList();

        return NpsAnalysisResponse.builder()
                .hasData(true)
                .totalCount(total)
                .trackName(track.getName())
                .uploadedAt(uploadedAt)
                .avgDifficulty(avgDifficulty)
                .avgSkill(avgSkill)
                .avgGrowth(avgGrowth)
                .avgCommunication(avgCommunication)
                .avgImmersion(avgImmersion)
                .avgOpsSatisfaction(avgOpsSatisfaction)
                .avgCurriculumSatisfaction(avgCurriculumSatisfaction)
                .promoters(promoters)
                .passives(passives)
                .detractors(detractors)
                .promoterPct(promoterPct)
                .passivePct(passivePct)
                .detractorPct(detractorPct)
                .npsScore(npsScore)
                .promoterList(promoterList)
                .detractorList(detractorList)
                .lowScoreStudents(lowScoreStudents)
                .npsComments(npsComments)
                .opsComments(opsComments)
                .studentDetails(studentDetails)
                .build();
    }

    // ── 파싱 헬퍼 ────────────────────────────────────

    private List<NpsResult> parseExcel(MultipartFile file, Track track, String uploadName) throws IOException {
        List<NpsResult> results = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String name = getCellString(row, 0);
                if (name == null || name.isBlank()) continue; // 빈 행 스킵

                results.add(NpsResult.builder()
                        .track(track)
                        .name(name)
                        .className(getCellString(row, 1))
                        .specialty(getCellString(row, 2))
                        .difficulty(getCellInt(row, 3))
                        .skill(getCellInt(row, 4))
                        .growth(getCellInt(row, 5))
                        .communication(getCellInt(row, 6))
                        .immersion(getCellInt(row, 7))
                        .opsSatisfaction(getCellInt(row, 8))
                        .opsComment(getCellString(row, 9))
                        .curriculumSatisfaction(getCellInt(row, 10))
                        // col 11: 커리큘럼 만족도 코멘트 (저장 제외)
                        .nps(getCellInt(row, 12))
                        .npsComment(getCellString(row, 13))
                        .uploadedAt(now)
                        .uploadName(uploadName)
                        .build());
            }
        }
        return results;
    }

    private static Integer getCellInt(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING  -> {
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

    private static String getCellString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> null;
        };
    }

    private static double avgOf(List<NpsResult> list, Function<NpsResult, Integer> getter) {
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
