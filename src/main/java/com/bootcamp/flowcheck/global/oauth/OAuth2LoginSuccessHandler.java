package com.bootcamp.flowcheck.global.oauth;

import com.bootcamp.flowcheck.domain.auth.entity.Pm;
import com.bootcamp.flowcheck.domain.auth.repository.PmRepository;
import com.bootcamp.flowcheck.global.jwt.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final PmRepository pmRepository;
    private final JwtUtil jwtUtil;

    @Value("${jwt.expiration}")
    private long expiration;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            String email = oAuth2User.getAttribute("email");
            log.info("[OAuth2] 로그인 시도 email={}", email);

            if (email == null) {
                log.warn("[OAuth2] Google에서 이메일 정보를 가져오지 못했습니다.");
                response.sendRedirect("/login?error=server");
                return;
            }

            Pm pm = pmRepository.findByEmail(email).orElse(null);

            if (pm == null) {
                log.warn("[OAuth2] 미등록 이메일로 로그인 시도: email={}", email);
                response.sendRedirect("/login?error=unauthorized");
                return;
            }

            log.info("[OAuth2] 로그인 성공: pmId={}, email={}", pm.getId(), pm.getEmail());

            String token = jwtUtil.generateToken(pm.getId(), pm.getEmail(), pm.getName(), pm.getRole());

            Cookie cookie = new Cookie("access_token", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge((int) (expiration / 1000));
            response.addCookie(cookie);

            // 리다이렉트 전에 세션 무효화 (JWT로 인증 전환)
            try {
                request.getSession(false);  // 세션이 있을 때만
                var session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
            } catch (Exception sessionEx) {
                log.debug("[OAuth2] 세션 무효화 중 무시 가능한 오류: {}", sessionEx.getMessage());
            }

            response.sendRedirect("/dashboard");

        } catch (Exception e) {
            log.error("[OAuth2] 로그인 처리 중 오류 발생: {}", e.getMessage(), e);
            response.sendRedirect("/login?error=server");
        }
    }
}
