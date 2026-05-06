package com.bootcamp.flowcheck.domain.course.service;

import com.bootcamp.flowcheck.domain.course.dto.*;
import com.bootcamp.flowcheck.domain.course.entity.Course;
import com.bootcamp.flowcheck.domain.course.entity.CourseWeek;
import com.bootcamp.flowcheck.domain.course.repository.CourseRepository;
import com.bootcamp.flowcheck.domain.course.repository.CourseWeekRepository;
import com.bootcamp.flowcheck.domain.track.entity.Track;
import com.bootcamp.flowcheck.domain.track.repository.PmTrackRepository;
import com.bootcamp.flowcheck.domain.track.repository.TrackRepository;
import com.bootcamp.flowcheck.global.exception.BusinessException;
import com.bootcamp.flowcheck.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseWeekRepository courseWeekRepository;
    private final TrackRepository trackRepository;
    private final PmTrackRepository pmTrackRepository;

    public List<CourseResponse> getCourses(Long trackId) {
        Long pmId = getCurrentPmId();
        validateTrack(trackId);
        validateAccess(pmId, trackId);

        return courseRepository.findAllByTrackIdWithWeeks(trackId).stream()
                .map(CourseResponse::of)
                .toList();
    }

    @Transactional
    public CourseResponse createCourse(Long trackId, CourseCreateRequest request) {
        Long pmId = getCurrentPmId();
        Track track = validateTrack(trackId);
        validateAccess(pmId, trackId);

        if (request.getWeeks() != null) {
            validateNoDuplicateWeekNumbers(request.getWeeks().stream()
                    .map(WeekRequest::getWeekNumber).toList());
        }

        Course course = Course.builder()
                .track(track)
                .name(request.getName())
                .build();
        courseRepository.save(course);

        if (request.getWeeks() != null) {
            List<CourseWeek> weeks = request.getWeeks().stream()
                    .map(w -> CourseWeek.builder()
                            .course(course)
                            .weekNumber(w.getWeekNumber())
                            .lectureCount(w.getLectureCount())
                            .build())
                    .toList();
            course.replaceWeeks(weeks);
        }

        return CourseResponse.of(course);
    }

    @Transactional
    public CourseResponse updateCourse(Long trackId, Long courseId, CourseUpdateRequest request) {
        Long pmId = getCurrentPmId();
        validateTrack(trackId);
        validateAccess(pmId, trackId);

        Course course = courseRepository.findByIdWithWeeks(courseId)
                .filter(c -> c.getTrack().getId().equals(trackId))
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        if (request.getWeeks() != null) {
            validateNoDuplicateWeekNumbers(request.getWeeks().stream()
                    .map(WeekRequest::getWeekNumber).toList());
        }

        course.updateName(request.getName());

        List<CourseWeek> newWeeks = request.getWeeks() != null
                ? request.getWeeks().stream()
                        .map(w -> CourseWeek.builder()
                                .course(course)
                                .weekNumber(w.getWeekNumber())
                                .lectureCount(w.getLectureCount())
                                .build())
                        .toList()
                : List.of();

        course.replaceWeeks(newWeeks);

        return CourseResponse.of(course);
    }

    @Transactional
    public void deleteCourse(Long trackId, Long courseId) {
        Long pmId = getCurrentPmId();
        validateTrack(trackId);
        validateAccess(pmId, trackId);

        Course course = courseRepository.findByIdAndTrack_Id(courseId, trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        course.softDelete();
    }

    @Transactional
    public void permanentDeleteCourse(Long courseId) {
        courseRepository.findDeletedById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        courseRepository.permanentDeleteById(courseId);
    }

    @Transactional
    public WeekResponse addWeek(Long trackId, Long courseId, WeekCreateRequest request) {
        Long pmId = getCurrentPmId();
        validateTrack(trackId);
        validateAccess(pmId, trackId);

        Course course = courseRepository.findByIdAndTrack_Id(courseId, trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        if (courseWeekRepository.existsByCourse_IdAndWeekNumber(courseId, request.getWeekNumber())) {
            throw new BusinessException(ErrorCode.DUPLICATE_WEEK);
        }

        CourseWeek week = CourseWeek.builder()
                .course(course)
                .weekNumber(request.getWeekNumber())
                .lectureCount(request.getLectureCount())
                .build();
        courseWeekRepository.save(week);

        return WeekResponse.of(week);
    }

    private Long getCurrentPmId() {
        return Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    private Track validateTrack(Long trackId) {
        return trackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACK_NOT_FOUND));
    }

    private void validateAccess(Long pmId, Long trackId) {
        if (!pmTrackRepository.existsByPm_IdAndTrack_Id(pmId, trackId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateNoDuplicateWeekNumbers(List<Integer> weekNumbers) {
        if (weekNumbers.stream().distinct().count() != weekNumbers.size()) {
            throw new BusinessException(ErrorCode.DUPLICATE_WEEK);
        }
    }
}
