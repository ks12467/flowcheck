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
// TODO: 테스트 후 원복 — @EnableMethodSecurity(prePostEnabled = true) 로 변경
@EnableMethodSecurity(prePostEnabled = false)
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
                        // TODO: 테스트 후 원복 — 아래 1줄 삭제하고 원래 설정 블록 주석 해제
                        .anyRequest().permitAll()
                        /*
                        // ── 원래 설정 (테스트 종료 후 아래 주석 해제 + 위 .anyRequest().permitAll() 삭제) ──
                        .requestMatchers("/dashboard", "/tracks/**", "/admin/nps", "/admin/satisfaction").permitAll()
                        .requestMatchers("/api/v1/tracks/**").permitAll()
                        .requestMatchers("/api/v1/admin/nps/**").permitAll()
                        .requestMatchers("/api/v1/admin/satisfaction/**").permitAll()
                        .requestMatchers("/api/v1/survey/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                        */
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
