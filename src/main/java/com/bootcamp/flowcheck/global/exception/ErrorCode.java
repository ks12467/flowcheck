package com.bootcamp.flowcheck.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    UNAUTHORIZED(401, "인증이 필요합니다."),
    UNAUTHORIZED_EMAIL(401, "허용되지 않은 이메일입니다."),
    FORBIDDEN(403, "접근 권한이 없습니다."),
    NOT_FOUND(404, "리소스를 찾을 수 없습니다."),
    VALIDATION_ERROR(400, "입력값이 올바르지 않습니다."),
    TRACK_NOT_FOUND(404, "트랙을 찾을 수 없습니다."),
    STUDENT_NOT_FOUND(404, "수강생을 찾을 수 없습니다."),
    COURSE_NOT_FOUND(404, "코스를 찾을 수 없습니다."),
    COURSE_WEEK_NOT_FOUND(404, "코스 주차를 찾을 수 없습니다."),
    DUPLICATE_STUDENT(409, "이미 등록된 수강생입니다."),
    DUPLICATE_WEEK(409, "이미 존재하는 주차입니다."),
    ALREADY_JOINED(409, "이미 참여 중입니다."),
    ALREADY_REGISTERED(409, "이미 등록된 이메일입니다."),
    INTERNAL_ERROR(500, "서버 내부 오류가 발생했습니다.");

    private final int status;
    private final String message;
}
