package org.zerock.mybackendapp.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.zerock.mybackendapp.auth.dto.LoginRequest;
import org.zerock.mybackendapp.auth.dto.LoginResponse;
import org.zerock.mybackendapp.auth.dto.RegisterRequest;
import org.zerock.mybackendapp.auth.dto.TokenRefreshRequest;
import org.zerock.mybackendapp.auth.service.AuthService;
import org.zerock.mybackendapp.user.domain.User;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Validated @RequestBody RegisterRequest request) {
        try {
            log.info("=== 회원가입 API 요청 ===");
            log.info("요청 데이터: username={} email={}", request.getUsername(), request.getEmail());

            User user = authService.register(request);

            log.info("회원가입 성공: id={}, username={}", user.getId(), user.getUsername());

            // 패스워드 제외하고 응답
            Map<String, Object> response = Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "role", user.getRole().name(),
                    "message", "회원가입이 완료되었습니다."
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("회원가입 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("예상치 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "내부 서버 오류"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Validated @RequestBody LoginRequest request) {
        try {
            log.info("=== 로그인 API 요청 ===");
            log.info("요청 데이터: username={}", request.getUsername());

            LoginResponse response = authService.login(request);

            log.info("로그인 성공: username={}, userId={}",
                    response.getUsername(), response.getUserId());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("로그인 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("예상치 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "내부 서버 오류"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Validated @RequestBody TokenRefreshRequest request) {
        try {
            log.info("=== 토큰 갱신 API 요청");
            log.info("리프레시 토큰 길이: {}", request.getRefreshToken().length());

            LoginResponse response = authService.refreshToken(request.getRefreshToken());

            log.info("토큰 갱신 성공: username={}", response.getUsername());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("토큰 갱신 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("예상치 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "내부 서버 오류"));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader (value = "Authorization", required = false) String authHeader) {
        try {
            log.info("=== 토큰 검증 API 요청 ===");
            if(authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("유효하지 않은 Authorization 헤더: {}", authHeader);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "유효하지 않은 토큰 형식"));
            }

            String token = authHeader.substring(7);
            log.info("추출된 토큰 길이: {}", token.length());

            boolean isValid = authService.validateToken(token);

            if(isValid) {
                User user = authService.getUserFromToken(token);

                Map<String, Object> response = Map.of(
                        "valid", true,
                        "user", Map.of(
                                "id", user.getId(),
                                "username", user.getUsername(),
                                "email", user.getEmail(),
                                "role", user.getRole().name()
                        )
                );

                log.info("토큰 검증 성공: username={}", user.getUsername());
                return ResponseEntity.ok(response);
            } else {
                log.warn("토큰 검증 실패");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "valid", false,
                                "error", "유효하지 않은 토큰"
                        ));
            }
        } catch (Exception e) {
            log.error("토큰 검증 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "valid", false,
                            "error", "토큰 검증 실패"
                    ));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            log.info("=== 현재 사용자 정보 조회 API 요청 ===");

            if(authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("유효하지 않은 Authorization 헤더");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "인증이 필요합니다"));
            }

            String token = authHeader.substring(7);
            User user = authService.getUserFromToken(token);

            Map<String, Object> response = Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "role", user.getRole().name(),
                    "createdAt", user.getCreatedAt(),
                    "updatedAt", user.getUpdatedAt()
            );

            log.info("현재 사용자 정보 조회 성공: username={}", user.getUsername());
            return ResponseEntity.ok(response);

        } catch(Exception e) {
            log.error("현재 사용자 정보 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증 실패"));
        }
    }

}
