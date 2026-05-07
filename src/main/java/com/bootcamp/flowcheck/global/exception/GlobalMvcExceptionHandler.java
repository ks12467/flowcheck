package com.bootcamp.flowcheck.global.exception;

import com.bootcamp.flowcheck.web.WebController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@ControllerAdvice(assignableTypes = WebController.class)
@Order(1)
public class GlobalMvcExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ModelAndView handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("[MVC BusinessException] code={}, message={}", errorCode.name(), errorCode.getMessage());
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorMessage", errorCode.getMessage());
        mav.addObject("statusCode", errorCode.getStatus());
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(Exception e) {
        Throwable root = getRootCause(e);
        log.error("[MVC UnhandledException] type={}, message={}, rootCause={}: {}",
                e.getClass().getSimpleName(), e.getMessage(),
                root.getClass().getSimpleName(), root.getMessage(), e);
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorMessage", "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        mav.addObject("statusCode", 500);
        return mav;
    }

    private Throwable getRootCause(Throwable t) {
        Throwable cause = t.getCause();
        return (cause == null || cause == t) ? t : getRootCause(cause);
    }
}
