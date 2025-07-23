package org.zerock.mybackendapp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.zerock.mybackendapp.auth.filter.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("=== PasswordEncoder 빈 등록 ===");
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        log.info("=== Spring Security 설정 시작 ===");

        http
                // CSRF 비활성화 (JWT 사용)
                .csrf(AbstractHttpConfigurer::disable)

                // 세션 사용 안함 (JWT)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 권한 설정
                .authorizeHttpRequests(authz -> authz
                        // 인증 없이 접근 가능한 경로들
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users").permitAll() // 사용자 목록 조회는 누구나 가능
                        .requestMatchers(HttpMethod.GET, "/api/users/{id}").permitAll() // 사용자 상세 조회는 누구나 가능
                        .requestMatchers(HttpMethod.GET, "/api/users/username/{username}").permitAll() // 사용자명으로 조회는 누구나 가능
                        .requestMatchers(HttpMethod.GET, "/api/posts").permitAll() // 게시글 목록 조회는 누구나 가능
                        .requestMatchers(HttpMethod.GET, "/api/posts/{id}").permitAll() // 게시글 상세 조회는 누구나 가능
                        .requestMatchers(HttpMethod.GET, "/api/posts/author/{authorId}").permitAll() // 작성자별 게시글 조회는 누구나 가능
                        .requestMatchers(HttpMethod.GET, "/api/posts/search").permitAll() // 게시글 검색은 누구나 가능
                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                )

                // 인증 실패 시 401 Unauthorized 반환
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);


            log.info("Spring Security 설정 완료");
            log.info("- CSRF: 비활성화");
            log.info("- 세션: STATELESS");
            log.info("- 인증 불필요 경로: /api/auth/**, GET /api/users, GET /api/posts/**");
            log.info("- JWT 필터: UsernamePasswordAuthenticationFilter 이전에 추가");
            log.info("- 인증 실패 시: 401 Unauthorized 반환");

            return http.build();
    }

}
