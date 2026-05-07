package com.bootcamp.flowcheck.web;

import com.bootcamp.flowcheck.domain.auth.entity.Pm;
import com.bootcamp.flowcheck.domain.auth.repository.PmRepository;
import com.bootcamp.flowcheck.domain.course.dto.CourseResponse;
import com.bootcamp.flowcheck.domain.course.repository.CourseRepository;
import com.bootcamp.flowcheck.domain.progress.entity.LearningProgress;
import com.bootcamp.flowcheck.domain.progress.repository.LearningProgressRepository;
import com.bootcamp.flowcheck.domain.student.entity.Student;
import com.bootcamp.flowcheck.domain.student.repository.StudentRepository;
import com.bootcamp.flowcheck.domain.track.entity.Track;
import com.bootcamp.flowcheck.domain.track.repository.PmTrackRepository;
import com.bootcamp.flowcheck.domain.track.repository.TrackRepository;
import com.bootcamp.flowcheck.global.exception.BusinessException;
import com.bootcamp.flowcheck.global.exception.ErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final PmRepository pmRepository;
    private final TrackRepository trackRepository;
    private final PmTrackRepository pmTrackRepository;
    private final StudentRepository studentRepository;
    private final LearningProgressRepository progressRepository;
    private final CourseRepository courseRepository;

    // 인증된 모든 요청에 PM 정보와 사이드바 트랙 목록을 주입
    @ModelAttribute
    public void addCommonAttributes(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return;
        try {
            Long pmId = Long.parseLong(authentication.getName());
            pmRepository.findById(pmId).ifPresent(pm -> model.addAttribute("pm", pm));
            model.addAttribute("sidebarTracks", trackRepository.findAllByPmId(pmId));
        } catch (NumberFormatException ignored) {
        }
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }
    // ── 로그인 ──────────────────────────────────────────────────────────────

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            String msg = "unauthorized".equals(error)
                    ? "허용되지 않은 이메일입니다. PM으로 등록된 계정으로 로그인해주세요."
                    : "로그인에 실패했습니다.";
            model.addAttribute("errorMessage", msg);
        }
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("access_token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/login";
    }

    // ── 대시보드 ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        Long pmId = Long.parseLong(authentication.getName());
        List<Track> tracks = trackRepository.findAllByPmId(pmId);
        List<Long> trackIds = tracks.stream().map(Track::getId).toList();

        // 즉시면담 수강생
        List<LearningProgress> urgentList = trackIds.isEmpty()
                ? List.of()
                : progressRepository.findLatestWithRiskLevelInTracks(trackIds, "즉시면담");
        model.addAttribute("urgentStudents", urgentList);

        // 트랙별 오늘 제출 현황
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        List<Map<String, Object>> trackStats = tracks.stream().map(t -> {
            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("trackId", t.getId());
            stat.put("trackName", t.getName());
            stat.put("totalStudents", studentRepository.countByTrack_Id(t.getId()));
            stat.put("todayCount", progressRepository.countTodaySubmissionsByTrackId(t.getId(), startOfDay));
            return stat;
        }).toList();
        model.addAttribute("trackStats", trackStats);

        // 난이도 분포 (1~5)
        long[] conditionCounts = new long[5];
        if (!trackIds.isEmpty()) {
            for (Object[] row : progressRepository.countByConditionScoreInTracks(trackIds)) {
                Integer score = (Integer) row[0];
                if (score != null && score >= 1 && score <= 5) {
                    conditionCounts[score - 1] = (Long) row[1];
                }
            }
        }
        model.addAttribute("conditionCounts", conditionCounts);

        return "dashboard";
    }

    // ── 수강생 목록 ──────────────────────────────────────────────────────────

    @GetMapping("/tracks/{trackId}/students")
    public String students(@PathVariable Long trackId,
                           @RequestParam(required = false) String riskLevel,
                           @RequestParam(required = false) String keyword,
                           Model model, Authentication authentication) {
        Long pmId = Long.parseLong(authentication.getName());

        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));
        if (!pmTrackRepository.existsByPm_IdAndTrack_Id(pmId, trackId)) {
            return "redirect:/dashboard";
        }

        List<Student> students = (keyword != null && !keyword.isBlank())
                ? studentRepository.findByTrackIdAndKeyword(trackId, keyword)
                : studentRepository.findAllByTrack_Id(trackId);

        Map<Long, LearningProgress> latestMap = students.isEmpty()
                ? Map.of()
                : progressRepository.findLatestByStudentIds(
                        students.stream().map(Student::getId).toList()).stream()
                .collect(Collectors.toMap(lp -> lp.getStudent().getId(), Function.identity()));

        List<Map<String, Object>> studentViews = students.stream()
                .map(s -> buildStudentView(s, latestMap.get(s.getId())))
                .filter(v -> riskLevel == null || riskLevel.equals(v.get("riskLevel")))
                .sorted(Comparator.comparingInt(v -> getRiskOrder((String) v.get("riskLevel"))))
                .toList();

        model.addAttribute("track", track);
        model.addAttribute("students", studentViews);
        model.addAttribute("selectedRiskLevel", riskLevel);
        model.addAttribute("keyword", keyword);
        return "students";
    }

    // ── 수강생 상세 ──────────────────────────────────────────────────────────

    @GetMapping("/tracks/{trackId}/students/{studentId}")
    public String studentDetail(@PathVariable Long trackId, @PathVariable Long studentId,
                                Model model, Authentication authentication) {
        Long pmId = Long.parseLong(authentication.getName());

        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));
        if (!pmTrackRepository.existsByPm_IdAndTrack_Id(pmId, trackId)) {
            return "redirect:/dashboard";
        }

        Student student = studentRepository.findByIdAndTrack_Id(studentId, trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));

        LearningProgress latest = progressRepository
                .findTopByStudent_IdOrderBySubmittedAtDesc(studentId).orElse(null);

        List<LearningProgress> history = progressRepository
                .findAllByStudentIdOrderBySubmittedAtDesc(studentId);

        model.addAttribute("track", track);
        model.addAttribute("student", student);
        model.addAttribute("latest", latest);
        model.addAttribute("progressHistory", history);
        return "student-detail";
    }

    // ── 어드민: 트랙 관리 ────────────────────────────────────────────────────

    @GetMapping("/admin/tracks")
    public String adminTracks(Model model) {
        model.addAttribute("allTracks", trackRepository.findAll());
        return "admin/tracks";
    }

    // ── 어드민: PM 관리 ──────────────────────────────────────────────────────

    @GetMapping("/admin/pms")
    public String adminPms(Model model) {
        List<Pm> allPms = pmRepository.findAll();
        List<Track> allTracks = trackRepository.findAll();

        List<Map<String, Object>> pmViews = allPms.stream().map(pm -> {
            List<Map<String, Object>> tracks = pmTrackRepository.findAllByPmIdWithTrack(pm.getId())
                    .stream().map(pt -> {
                        Map<String, Object> t = new LinkedHashMap<>();
                        t.put("trackId", pt.getTrack().getId());
                        t.put("trackName", pt.getTrack().getName());
                        return t;
                    }).toList();
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("pmId", pm.getId());
            view.put("name", pm.getName());
            view.put("email", pm.getEmail());
            view.put("tracks", tracks);
            return view;
        }).toList();

        model.addAttribute("pmViews", pmViews);
        model.addAttribute("allTracks", allTracks);
        return "admin/pms";
    }

    // ── 어드민: 수강생 관리 ──────────────────────────────────────────────────

    @GetMapping("/admin/students")
    public String adminStudents(Model model) {
        model.addAttribute("allTracks", trackRepository.findAll());
        return "admin/students";
    }

    // ── 어드민: 삭제된 데이터 관리 ───────────────────────────────────────────

    @GetMapping("/admin/deleted")
    public String adminDeleted() {
        return "admin/deleted";
    }

    // ── 진척도 제출 폼 (인증 불필요) ─────────────────────────────────────────

    @GetMapping("/progress/{trackId}")
    public String progressForm(@PathVariable Long trackId, Model model) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));
        List<CourseResponse> courses = courseRepository.findAllByTrackIdWithWeeks(trackId)
                .stream().map(CourseResponse::of).toList();
        model.addAttribute("track", track);
        model.addAttribute("courses", courses);
        return "progress-form";
    }

    // ── 유틸 ─────────────────────────────────────────────────────────────────

    private Map<String, Object> buildStudentView(Student s, LearningProgress lp) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("studentId", s.getId());
        v.put("name", s.getName());
        v.put("email", s.getEmail());
        v.put("phone", s.getPhone());
        v.put("riskLevel", lp != null ? lp.getRiskLevel() : null);
        v.put("conditionScore", lp != null ? lp.getConditionScore() : null);
        v.put("submittedAt", lp != null ? lp.getSubmittedAt() : null);
        return v;
    }

    private int getRiskOrder(String riskLevel) {
        return switch (riskLevel == null ? "" : riskLevel) {
            case "즉시면담" -> 1;
            case "주의"   -> 2;
            case "관찰"   -> 3;
            case "정상"   -> 4;
            default       -> 5;
        };
    }
}
