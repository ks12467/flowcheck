package com.bootcamp.flowcheck.global.config;

import com.bootcamp.flowcheck.global.jwt.JwtAuthenticationFilter;
import com.bootcamp.flowcheck.global.oauth.OAuth2LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        .requestMatchers("/progress/**").permitAll()
                        // [사용성 테스트] PM 웹 페이지 - 로그인 없이 접근 허용 (되돌릴 때 이 블록 삭제)
                        .requestMatchers("/dashboard", "/tracks/**", "/admin/nps", "/admin/satisfaction").permitAll()
                        // [사용성 테스트] PM API - 로그인 없이 접근 허용 (되돌릴 때 이 블록 삭제)
                        .requestMatchers("/api/v1/tracks/**").permitAll()
                        .requestMatchers("/api/v1/admin/nps/**").permitAll()
                        .requestMatchers("/api/v1/admin/satisfaction/**").permitAll()
                        .requestMatchers("/api/v1/survey/**").permitAll()
                        // 그 외 어드민 DELETE는 ADMIN만
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/admin/**").hasRole("ADMIN")
                        // 나머지는 인증된 사용자 모두 허용
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
