package com.bootcamp.flowcheck.domain.satisfaction.service;

import com.bootcamp.flowcheck.domain.satisfaction.dto.SatisfactionAiReportResponse;
import com.bootcamp.flowcheck.domain.satisfaction.dto.SatisfactionAnalysisResponse;
import com.bootcamp.flowcheck.domain.satisfaction.entity.SatisfactionResult;
import com.bootcamp.flowcheck.domain.satisfaction.repository.SatisfactionResultRepository;
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
public class SatisfactionService {

    private final SatisfactionResultRepository satisfactionRepository;
    private final TrackRepository trackRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    // ── AI 분석 리포트 ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SatisfactionAiReportResponse buildAiReport(Long trackId, String uploadName) {
        trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));

        List<SatisfactionResult> results =
                satisfactionRepository.findAllByTrack_IdAndUploadName(trackId, uploadName);

        // ── 1. 온라인 강의 평균 ─────────────────────────────────────────────
        double avgLectureDelivery   = avgOf(results, SatisfactionResult::getLectureDelivery);
        double avgLectureMaterial   = avgOf(results, SatisfactionResult::getLectureMaterial);
        double avgLectureQuality    = avgOf(results, SatisfactionResult::getLectureQuality);
        double avgLectureDifficulty = avgOf(results, SatisfactionResult::getLectureDifficulty);
        double avgLectureNps        = avgOf(results, SatisfactionResult::getLectureNps);

        // ── 2. 개인과제 통계 ────────────────────────────────────────────────
        List<Integer> assignmentScores = results.stream()
                .map(SatisfactionResult::getAssignmentSatisfaction)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        double assignmentAvg    = round1(assignmentScores.stream().mapToInt(i -> i).average().orElse(0));
        double assignmentMedian = median(assignmentScores);
        int    assignmentMode   = mode(assignmentScores);

        // ── 4. 커리큘럼 통계 ────────────────────────────────────────────────
        List<Integer> satScores = results.stream()
                .map(SatisfactionResult::getCurriculumSatisfaction)
                .filter(Objects::nonNull).sorted().toList();
        List<Integer> diffScores = results.stream()
                .map(SatisfactionResult::getCurriculumDifficulty)
                .filter(Objects::nonNull).sorted().toList();

        SatisfactionAiReportResponse.StatSummary curriculumSatStats = new SatisfactionAiReportResponse.StatSummary(
                avgOf(results, SatisfactionResult::getCurriculumSatisfaction),
                median(satScores), mode(satScores));
        SatisfactionAiReportResponse.StatSummary curriculumDiffStats = new SatisfactionAiReportResponse.StatSummary(
                avgOf(results, SatisfactionResult::getCurriculumDifficulty),
                median(diffScores), mode(diffScores));

        // 난이도 그룹 (4점, 5점)
        List<SatisfactionAiReportResponse.DifficultyGroup> difficultyGroups = new ArrayList<>();
        for (int score : new int[]{4, 5}) {
            String level = score == 4 ? "어려움" : "매우 어려움";
            List<SatisfactionResult> group = results.stream()
                    .filter(r -> r.getCurriculumDifficulty() != null && r.getCurriculumDifficulty() == score)
                    .toList();
            if (!group.isEmpty()) {
                double avgSat = round1(group.stream()
                        .map(SatisfactionResult::getCurriculumSatisfaction)
                        .filter(Objects::nonNull)
                        .mapToInt(i -> i).average().orElse(0));
                difficultyGroups.add(new SatisfactionAiReportResponse.DifficultyGroup(score, level, group.size(), avgSat));
            }
        }

        // 저만족 수강생 (만족도 6점 이하)
        List<SatisfactionAiReportResponse.LowSatStudent> lowSatStudents = results.stream()
                .filter(r -> r.getCurriculumSatisfaction() != null && r.getCurriculumSatisfaction() <= 6)
                .map(r -> new SatisfactionAiReportResponse.LowSatStudent(
                        r.getRespondentName(),
                        r.getCurriculumSatisfaction(),
                        r.getCurriculumDifficulty()))
                .sorted(Comparator.comparingInt(s -> s.getCurriculumSatisfaction()))
                .toList();

        // ── Gemini 호출 ─────────────────────────────────────────────────────
        String prompt = buildAiPrompt(results,
                avgLectureDelivery, avgLectureMaterial, avgLectureQuality, avgLectureDifficulty, avgLectureNps,
                assignmentAvg, assignmentMedian, assignmentMode,
                curriculumSatStats, curriculumDiffStats,
                difficultyGroups, lowSatStudents);

        String geminiRaw = geminiService.generateContent(prompt);
        Map<String, String> parsed = parseGeminiJson(geminiRaw);

        return SatisfactionAiReportResponse.builder()
                .avgLectureDelivery(avgLectureDelivery)
                .avgLectureMaterial(avgLectureMaterial)
                .avgLectureQuality(avgLectureQuality)
                .avgLectureDifficulty(avgLectureDifficulty)
                .avgLectureNps(avgLectureNps)
                .lectureAnalysis(parsed.getOrDefault("lectureAnalysis", ""))
                .assignmentAvg(assignmentAvg)
                .assignmentMedian(assignmentMedian)
                .assignmentMode(assignmentMode)
                .assignmentAnalysis(parsed.getOrDefault("assignmentAnalysis", ""))
                .sscStart(parsed.getOrDefault("sscStart", ""))
                .sscStop(parsed.getOrDefault("sscStop", ""))
                .sscContinue(parsed.getOrDefault("sscContinue", ""))
                .curriculumSatisfactionStats(curriculumSatStats)
                .curriculumDifficultyStats(curriculumDiffStats)
                .curriculumConclusion(parsed.getOrDefault("curriculumConclusion", ""))
                .studentRelationshipAnalysis(parsed.getOrDefault("studentRelationshipAnalysis", ""))
                .difficultyGroups(difficultyGroups)
                .difficultyGroupInsights(parsed.getOrDefault("difficultyGroupInsights", ""))
                .lowSatStudents(lowSatStudents)
                .lowSatisfactionAnalysis(parsed.getOrDefault("lowSatisfactionAnalysis", ""))
                .build();
    }

    private String buildAiPrompt(List<SatisfactionResult> results,
                                  double avgDelivery, double avgMaterial, double avgQuality,
                                  double avgLecDiff, double avgLecNps,
                                  double assignAvg, double assignMedian, int assignMode,
                                  SatisfactionAiReportResponse.StatSummary satStats,
                                  SatisfactionAiReportResponse.StatSummary diffStats,
                                  List<SatisfactionAiReportResponse.DifficultyGroup> diffGroups,
                                  List<SatisfactionAiReportResponse.LowSatStudent> lowSat) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                당신은 부트캠프 PM 어시스턴트입니다. 아래 만족도 설문 데이터를 분석하여 반드시 JSON 형식으로만 응답하세요.
                코드블록 없이 순수 JSON만 출력하고, 각 필드값은 한국어 마크다운으로 작성하세요.

                [응답 JSON 형식]
                {
                  "lectureAnalysis": "...",
                  "assignmentAnalysis": "...",
                  "sscStart": "...",
                  "sscStop": "...",
                  "sscContinue": "...",
                  "curriculumConclusion": "...",
                  "studentRelationshipAnalysis": "...",
                  "difficultyGroupInsights": "...",
                  "lowSatisfactionAnalysis": "..."
                }

                각 필드 작성 지침:
                - lectureAnalysis: 강의 전달력/교재/품질/체감난이도/추천도 점수 해석 및 개선 인사이트 (5점 만점 기준)
                - assignmentAnalysis: 과제 만족도 통계(평균/중앙값/최빈값) 해석 및 PM 시사점
                - sscStart: 수강생 의견에서 도출한 새롭게 시도할 것 목록과 근거
                - sscStop: 수강생 의견에서 도출한 개선·중단이 필요한 것 목록과 근거
                - sscContinue: 수강생 의견에서 도출한 계속 유지할 것 목록과 근거
                - curriculumConclusion: 커리큘럼 만족도·난이도 통계(평균/중앙값/최빈값) 해석 및 결론
                - studentRelationshipAnalysis: 수강생별 난이도-만족도 관계 분석 마크다운 테이블 (컬럼: 이름|만족도|난이도|난이도체감레벨|관계분석). 반드시 마크다운 테이블 형식으로 작성
                - difficultyGroupInsights: 난이도 고체감 그룹(4점·5점)의 만족도 데이터 기반 주요 시사점 및 권장 조치
                - lowSatisfactionAnalysis: 만족도 6점 이하 수강생별 문제유형·위험진단 마크다운 테이블(컬럼: 이름|만족도|난이도|문제유형|위험진단) + 유형별 개선방안. 반드시 마크다운 테이블 형식으로 작성

                """);

        // 강의 점수
        sb.append("\n[온라인 강의 평균 점수 (5점 만점)]\n")
          .append("전달력: ").append(avgDelivery).append("\n")
          .append("교재: ").append(avgMaterial).append("\n")
          .append("품질: ").append(avgQuality).append("\n")
          .append("체감 난이도: ").append(avgLecDiff).append("\n")
          .append("추천도: ").append(avgLecNps).append("\n");

        // 과제 통계
        sb.append("\n[개인과제 만족도 통계 (5점 만점)]\n")
          .append("평균: ").append(assignAvg)
          .append(" | 중앙값: ").append(assignMedian)
          .append(" | 최빈값: ").append(assignMode).append("\n");

        // 커리큘럼 통계
        sb.append("\n[커리큘럼 만족도 통계 (10점 만점)]\n")
          .append("평균: ").append(satStats.getAvg())
          .append(" | 중앙값: ").append(satStats.getMedian())
          .append(" | 최빈값: ").append(satStats.getMode()).append("\n");
        sb.append("\n[커리큘럼 난이도 통계 (5점 만점)]\n")
          .append("평균: ").append(diffStats.getAvg())
          .append(" | 중앙값: ").append(diffStats.getMedian())
          .append(" | 최빈값: ").append(diffStats.getMode()).append("\n");

        // 수강생별 커리큘럼 데이터
        sb.append("\n[수강생별 커리큘럼 만족도 & 난이도]\n");
        for (SatisfactionResult r : results) {
            if (r.getCurriculumSatisfaction() == null && r.getCurriculumDifficulty() == null) continue;
            sb.append("- ").append(r.getRespondentName())
              .append(" | 만족도: ").append(r.getCurriculumSatisfaction())
              .append(" | 난이도: ").append(r.getCurriculumDifficulty()).append("\n");
        }

        // 난이도 고체감 그룹
        sb.append("\n[난이도 고체감 그룹]\n");
        if (diffGroups.isEmpty()) {
            sb.append("해당 없음\n");
        } else {
            diffGroups.forEach(g -> sb.append("- ").append(g.getScore()).append("점(").append(g.getLevel())
                    .append("): ").append(g.getCount()).append("명, 평균 만족도 ").append(g.getAvgSatisfaction()).append("\n"));
        }

        // 저만족 수강생
        sb.append("\n[저만족 수강생 (만족도 6점 이하)]\n");
        if (lowSat.isEmpty()) {
            sb.append("해당 없음\n");
        } else {
            lowSat.forEach(s -> sb.append("- ").append(s.getName())
                    .append(" | 만족도: ").append(s.getCurriculumSatisfaction())
                    .append(" | 난이도: ").append(s.getCurriculumDifficulty()).append("\n"));
        }

        // 전체 의견 (SSC 분석용)
        sb.append("\n[전체 수강생 의견 (SSC 분석용)]\n");
        for (SatisfactionResult r : results) {
            String free = r.getFreeComment();
            String ops  = r.getOperationComment();
            if ((free != null && !free.isBlank()) || (ops != null && !ops.isBlank())) {
                sb.append("- ").append(r.getRespondentName()).append("\n");
                if (free != null && !free.isBlank()) sb.append("  자유의견: ").append(free).append("\n");
                if (ops  != null && !ops.isBlank())  sb.append("  운영의견: ").append(ops).append("\n");
            }
        }

        return sb.toString();
    }

    private Map<String, String> parseGeminiJson(String raw) {
        String cleaned = raw.strip();
        if (cleaned.startsWith("```")) {
            int first = cleaned.indexOf('\n');
            int last  = cleaned.lastIndexOf("```");
            if (first >= 0 && last > first) cleaned = cleaned.substring(first + 1, last).strip();
        }
        try {
            JsonNode node = objectMapper.readTree(cleaned);
            Map<String, String> result = new HashMap<>();
            node.fields().forEachRemaining(e -> result.put(e.getKey(), e.getValue().asText("")));
            return result;
        } catch (Exception e) {
            log.warn("[Satisfaction AI] Gemini JSON 파싱 실패: {}", e.getMessage());
            Map<String, String> fallback = new HashMap<>();
            fallback.put("lectureAnalysis", raw);
            return fallback;
        }
    }

    // ── 통계 헬퍼 ──────────────────────────────────────────────────────────

    private static double median(List<Integer> sorted) {
        if (sorted.isEmpty()) return 0;
        int n = sorted.size();
        if (n % 2 == 1) return sorted.get(n / 2);
        return round1((sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0);
    }

    private static int mode(List<Integer> scores) {
        if (scores.isEmpty()) return 0;
        return scores.stream()
                .collect(Collectors.groupingBy(i -> i, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0);
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
