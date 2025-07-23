package org.zerock.mybackendapp.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.zerock.mybackendapp.auth.service.AuthService;
import org.zerock.mybackendapp.user.domain.User;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Lazy
    private final AuthService authService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        log.info("=== JWT 필터 시작: {} {} ===", method, requestURI);

        // Authorization 헤더에서 토큰 추출
        String authHeader = request.getHeader("Authorization");

        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.info("Authorization 헤더 없음 또는 Bearer 토큰 아님: {}", authHeader);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            log.info("토큰 추출 완료: 길이={}", token.length());

            // 토큰 유효성 검증
            if(!authService.validateToken(token)) {
                log.warn("유효하지 않은 토큰");
                filterChain.doFilter(request, response);
                return;
            }

            // 토큰에서 사용자 정보 추출
            User user = authService.getUserFromToken(token);
            log.info("토큰에서 사용자 정보 추출: username={}, role={}",
                    user.getUsername(), user.getRole());

            // Spring Security 인증 객체 생성
            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority(user.getRole().toString())
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, authorities);

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // SecurityContext에 인증 정보 설정
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("Spring Security 인증 설정 완료: username={}, authorities={}",
                    user.getUsername(), authorities);
        } catch (Exception e) {
            log.error("JWT 인증 처리 중 오류 발생: {}", e.getMessage());
            // 인증 실패해도 다음 필터로 진행 (Spring Security가 처리)
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 인증이 필요없는 경로들은 필터 적용 안함
        boolean shouldSkip = path.startsWith("/api/auth/") ||
                (path.equals("/api/users") && "GET".equals(method)) ||
                (path.equals("/api/posts") && "GET".equals(method)) ||
                (path.matches("/api/posts/\\d+") && "GET".equals(method)) ||
                (path.equals("/api/posts/search") && "GET".equals(method));

        if(shouldSkip) {
            log.info("JWT 필터 스킵: {} {}", method, path);
        }

        return shouldSkip;
    }

}
