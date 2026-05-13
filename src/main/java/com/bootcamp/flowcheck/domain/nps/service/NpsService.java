package com.bootcamp.flowcheck.domain.nps.service;

import com.bootcamp.flowcheck.domain.nps.dto.NpsAiReportResponse;
import com.bootcamp.flowcheck.domain.nps.dto.NpsAnalysisResponse;
import com.bootcamp.flowcheck.domain.nps.entity.NpsResult;
import com.bootcamp.flowcheck.domain.nps.repository.NpsResultRepository;
import com.bootcamp.flowcheck.domain.student.entity.Student;
import com.bootcamp.flowcheck.domain.student.repository.StudentRepository;
import com.bootcamp.flowcheck.domain.track.entity.Track;
import com.bootcamp.flowcheck.domain.track.repository.TrackRepository;
import com.bootcamp.flowcheck.global.exception.BusinessException;
import com.bootcamp.flowcheck.global.exception.ErrorCode;
import com.bootcamp.flowcheck.global.gemini.GeminiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NpsService {

    private final NpsResultRepository npsResultRepository;
    private final TrackRepository trackRepository;
    private final StudentRepository studentRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    // ── AI 분석 리포트 ────────────────────────────────

    @Transactional(readOnly = true)
    public NpsAiReportResponse buildAiReport(Long trackId, String uploadName) {
        trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));

        List<NpsResult> results = npsResultRepository.findAllByTrack_IdAndUploadNameOrderByNameAsc(trackId, uploadName);
        List<Student> allStudents = studentRepository.findAllByTrack_Id(trackId);

        // ── 1. 제출현황 ──────────────────────────────
        Set<String> submittedNames = results.stream().map(NpsResult::getName).collect(Collectors.toSet());
        List<String> notSubmitted = allStudents.stream()
                .map(Student::getName)
                .filter(name -> !submittedNames.contains(name))
                .sorted()
                .toList();

        // ── 2. NPS 분류 ──────────────────────────────
        List<NpsResult> detractorResults = results.stream()
                .filter(r -> r.getNps() != null && r.getNps() <= 6)
                .toList();
        List<NpsResult> passiveResults = results.stream()
                .filter(r -> r.getNps() != null && r.getNps() >= 7 && r.getNps() <= 8)
                .toList();
        List<NpsResult> promoterResults = results.stream()
                .filter(r -> r.getNps() != null && r.getNps() >= 9)
                .toList();

        List<NpsAiReportResponse.NpsStudentEntry> detractors = toEntries(detractorResults);
        List<NpsAiReportResponse.NpsStudentEntry> passives   = toEntries(passiveResults);
        List<NpsAiReportResponse.NpsStudentEntry> promoters  = toEntries(promoterResults);

        // ── 3. NPS 변동 인원 ─────────────────────────
        List<String> uploadNames = npsResultRepository.findDistinctUploadNamesByTrackId(trackId);
        int currentIdx = uploadNames.indexOf(uploadName);
        boolean hasPreviousUpload = currentIdx > 0;

        List<NpsAiReportResponse.NpsChange> declined = new ArrayList<>();
        List<NpsAiReportResponse.NpsChange> improved = new ArrayList<>();
        String previousUploadName = null;
        List<NpsResult> previousResults = List.of();

        if (hasPreviousUpload) {
            previousUploadName = uploadNames.get(currentIdx - 1);
            previousResults = npsResultRepository
                    .findAllByTrack_IdAndUploadNameOrderByNameAsc(trackId, previousUploadName);

            Map<String, Integer> prevNpsMap = previousResults.stream()
                    .filter(r -> r.getNps() != null)
                    .collect(Collectors.toMap(NpsResult::getName, NpsResult::getNps, (a, b) -> a));

            for (NpsResult cur : results) {
                if (cur.getNps() == null) continue;
                Integer prevNps = prevNpsMap.get(cur.getName());
                if (prevNps == null) continue;
                int diff = cur.getNps() - prevNps;
                if (diff < 0) {
                    declined.add(new NpsAiReportResponse.NpsChange(cur.getName(), prevNps, cur.getNps()));
                } else if (diff > 0) {
                    improved.add(new NpsAiReportResponse.NpsChange(cur.getName(), prevNps, cur.getNps()));
                }
            }
        }

        // ── 4. Gemini 분석 호출 ───────────────────────
        String prompt = buildAiPrompt(results, detractorResults, passiveResults, promoterResults,
                declined, improved, hasPreviousUpload, previousUploadName);

        String geminiResponse = geminiService.generateContent(prompt);
        Map<String, String> parsed = parseGeminiJson(geminiResponse);

        return NpsAiReportResponse.builder()
                .totalStudents(allStudents.size())
                .submittedCount(results.size())
                .notSubmitted(notSubmitted)
                .detractors(detractors)
                .passives(passives)
                .promoters(promoters)
                .hasPreviousUpload(hasPreviousUpload)
                .previousUploadName(previousUploadName)
                .declined(declined)
                .improved(improved)
                .detractorAnalysis(parsed.getOrDefault("detractorAnalysis", ""))
                .passiveAnalysis(parsed.getOrDefault("passiveAnalysis", ""))
                .promoterAnalysis(parsed.getOrDefault("promoterAnalysis", ""))
                .npsChangeAnalysis(parsed.getOrDefault("npsChangeAnalysis", ""))
                .difficultyAnalysis(parsed.getOrDefault("difficultyAnalysis", ""))
                .sscStart(parsed.getOrDefault("sscStart", ""))
                .sscStop(parsed.getOrDefault("sscStop", ""))
                .sscContinue(parsed.getOrDefault("sscContinue", ""))
                .build();
    }

    private List<NpsAiReportResponse.NpsStudentEntry> toEntries(List<NpsResult> list) {
        return list.stream()
                .map(r -> new NpsAiReportResponse.NpsStudentEntry(
                        r.getName(), r.getNps(), r.getOpsSatisfaction(),
                        r.getNpsComment(), r.getOpsComment()))
                .toList();
    }

    private String buildAiPrompt(List<NpsResult> all,
                                  List<NpsResult> detractors, List<NpsResult> passives, List<NpsResult> promoters,
                                  List<NpsAiReportResponse.NpsChange> declined,
                                  List<NpsAiReportResponse.NpsChange> improved,
                                  boolean hasPrevious, String prevUploadName) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                당신은 부트캠프 PM 어시스턴트입니다. 아래 NPS 설문 데이터를 분석하여 반드시 JSON 형식으로만 응답하세요.
                코드블록 없이 순수 JSON만 출력하고, 각 필드값은 한국어 마크다운 형식으로 작성하세요.

                [응답 JSON 형식]
                {
                  "detractorAnalysis": "...",
                  "passiveAnalysis": "...",
                  "promoterAnalysis": "...",
                  "npsChangeAnalysis": "...",
                  "difficultyAnalysis": "...",
                  "sscStart": "...",
                  "sscStop": "...",
                  "sscContinue": "..."
                }

                각 필드 작성 지침:
                - detractorAnalysis: NPS 0~6 수강생별로 코멘트 사유를 분석. 수강생 이름, 점수, 코멘트 원문을 포함하고 PM이 주목해야 할 핵심 이슈를 설명
                - passiveAnalysis: NPS 7~8 수강생별 특이사항 분석. 개선 가능한 부분 위주로 서술
                - promoterAnalysis: NPS 9~10 수강생 분석. 긍정적 요인 및 유지해야 할 강점 정리
                - npsChangeAnalysis: NPS 변동 인원 분석 (데이터가 없으면 "이전 데이터 없음" 기재)
                - difficultyAnalysis: 난이도 점수 분포 분석 및 전체 VOC에서 난이도 관련 언급 분석
                - sscStart: 수강생 코멘트에서 도출한 "새롭게 시도해볼 것" 목록과 근거
                - sscStop: 수강생 코멘트에서 도출한 "개선하거나 그만둬야 할 것" 목록과 근거
                - sscContinue: 수강생 코멘트에서 도출한 "계속 유지할 것" 목록과 근거

                """);

        // 비추천(0~6)
        sb.append("\n[비추천 수강생 (NPS 0~6)]\n");
        if (detractors.isEmpty()) {
            sb.append("없음\n");
        } else {
            for (NpsResult r : detractors) {
                sb.append("- ").append(r.getName())
                        .append(" | NPS: ").append(r.getNps())
                        .append(" | 운영만족도: ").append(r.getOpsSatisfaction())
                        .append(" | 난이도: ").append(r.getDifficulty())
                        .append("\n  NPS 코멘트: ").append(orEmpty(r.getNpsComment()))
                        .append("\n  운영 코멘트: ").append(orEmpty(r.getOpsComment()))
                        .append("\n");
            }
        }

        // 중립(7~8)
        sb.append("\n[중립 수강생 (NPS 7~8)]\n");
        if (passives.isEmpty()) {
            sb.append("없음\n");
        } else {
            for (NpsResult r : passives) {
                sb.append("- ").append(r.getName())
                        .append(" | NPS: ").append(r.getNps())
                        .append(" | 운영만족도: ").append(r.getOpsSatisfaction())
                        .append("\n  NPS 코멘트: ").append(orEmpty(r.getNpsComment()))
                        .append("\n  운영 코멘트: ").append(orEmpty(r.getOpsComment()))
                        .append("\n");
            }
        }

        // 추천(9~10)
        sb.append("\n[추천 수강생 (NPS 9~10)]\n");
        if (promoters.isEmpty()) {
            sb.append("없음\n");
        } else {
            for (NpsResult r : promoters) {
                sb.append("- ").append(r.getName())
                        .append(" | NPS: ").append(r.getNps())
                        .append(" | 운영만족도: ").append(r.getOpsSatisfaction())
                        .append("\n  NPS 코멘트: ").append(orEmpty(r.getNpsComment()))
                        .append("\n  운영 코멘트: ").append(orEmpty(r.getOpsComment()))
                        .append("\n");
            }
        }

        // NPS 변동
        sb.append("\n[NPS 변동 인원 (이전 업로드: ").append(hasPrevious ? prevUploadName : "없음").append(")]\n");
        if (!hasPrevious) {
            sb.append("이전 업로드 데이터 없음\n");
        } else {
            sb.append("하락:\n");
            if (declined.isEmpty()) sb.append("  없음\n");
            else declined.forEach(c -> sb.append("  - ").append(c.getName())
                    .append(": ").append(c.getPreviousNps()).append(" → ").append(c.getCurrentNps()).append("\n"));
            sb.append("상승:\n");
            if (improved.isEmpty()) sb.append("  없음\n");
            else improved.forEach(c -> sb.append("  - ").append(c.getName())
                    .append(": ").append(c.getPreviousNps()).append(" → ").append(c.getCurrentNps()).append("\n"));
        }

        // 난이도 분포
        sb.append("\n[난이도 분포]\n");
        Map<Integer, Long> diffDist = all.stream()
                .filter(r -> r.getDifficulty() != null)
                .collect(Collectors.groupingBy(NpsResult::getDifficulty, Collectors.counting()));
        diffDist.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append("  ").append(e.getKey()).append("점: ").append(e.getValue()).append("명\n"));

        // 전체 코멘트 (SSC 분석용)
        sb.append("\n[전체 수강생 코멘트 (SSC 분석용)]\n");
        for (NpsResult r : all) {
            String npsC = r.getNpsComment();
            String opsC = r.getOpsComment();
            if ((npsC != null && !npsC.isBlank()) || (opsC != null && !opsC.isBlank())) {
                sb.append("- ").append(r.getName()).append("\n");
                if (npsC != null && !npsC.isBlank()) sb.append("  NPS: ").append(npsC).append("\n");
                if (opsC != null && !opsC.isBlank()) sb.append("  운영: ").append(opsC).append("\n");
            }
        }

        return sb.toString();
    }

    private Map<String, String> parseGeminiJson(String raw) {
        // 코드블록 제거 (```json ... ```)
        String cleaned = raw.strip();
        if (cleaned.startsWith("```")) {
            int first = cleaned.indexOf('\n');
            int last  = cleaned.lastIndexOf("```");
            if (first >= 0 && last > first) {
                cleaned = cleaned.substring(first + 1, last).strip();
            }
        }
        try {
            JsonNode node = objectMapper.readTree(cleaned);
            Map<String, String> result = new HashMap<>();
            node.fields().forEachRemaining(e -> result.put(e.getKey(), e.getValue().asText("")));
            return result;
        } catch (Exception e) {
            log.warn("[NPS AI] Gemini 응답 JSON 파싱 실패. 원문 반환. error={}", e.getMessage());
            Map<String, String> fallback = new HashMap<>();
            fallback.put("detractorAnalysis", raw);
            return fallback;
        }
    }

    private static String orEmpty(String s) {
        return s != null && !s.isBlank() ? s : "(코멘트 없음)";
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
