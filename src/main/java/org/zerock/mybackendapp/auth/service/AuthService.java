package org.zerock.mybackendapp.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.mybackendapp.auth.dto.LoginRequest;
import org.zerock.mybackendapp.auth.dto.LoginResponse;
import org.zerock.mybackendapp.auth.dto.RegisterRequest;
import org.zerock.mybackendapp.auth.util.JwtUtil;
import org.zerock.mybackendapp.user.domain.User;
import org.zerock.mybackendapp.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public User register(RegisterRequest request) {
        log.info("=== 사용자 회원가입 시작 ===");
        log.info("회원가입 요청: username={}, email={}", request.getUsername(), request.getEmail());

        // 중복 검증
        if(userRepository.existsByUsername(request.getUsername())) {
            log.warn("이미 존재하는 사용자명: {}", request.getUsername());
            throw new IllegalArgumentException("이미 존재하는 사용자명입니다: " + request.getUsername());
        }

        if(userRepository.existsByEmail(request.getEmail())) {
            log.warn("이미 존재하는 이메일: {}", request.getEmail());
            throw new IllegalArgumentException("이미 존재하는 이메일입니다: " + request.getEmail());
        }

        // 사용자 생성 및 패스워드 인코딩
        User user = User.of(request.getUsername(), request.getEmail(),
                            request.getPassword(), User.UserRole.USER);

        user.encodePassword(passwordEncoder);

        User savedUser = userRepository.save(user);
        log.info("회원가입 완료: id={}, username={}", savedUser.getId(), savedUser.getUsername());

        return savedUser;
    }

    public LoginResponse login(LoginRequest request) {
        log.info("=== 사용자 로그인 시작 ===");
        log.info("로그인 요청: username={}", request.getUsername());

        // 사용자 조회
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 사용자: username={}", request.getUsername());
                    return new IllegalArgumentException("사용자를 찾을 수 없습니다.");
                });

        // 패스워드 검증
        if(!user.matchesPassword(request.getPassword(), passwordEncoder)) {
            log.warn("패스워드 불일치: username={}", request.getUsername());
            throw new IllegalArgumentException("패스워드가 일치하지 않습니다.");
        }

        // JWT 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        log.info("로그인 성공: username={}, userId={}", user.getUsername(), user.getId());
        log.info("토큰 발급 완료: accessToken 길이={}, refreshToken 길이={}",
                accessToken.length(), refreshToken.length());

        return LoginResponse.of(user, accessToken, refreshToken);
    }

    public LoginResponse refreshToken(String refreshToken) {
        log.info("=== 토큰 갱신 시작 ===");
        log.info("리프레시 토큰 길이: {}", refreshToken.length());

        // 리프레시 토큰 유효성 검증
        if(!jwtUtil.isTokenValid(refreshToken)) {
            log.warn("유효하지 않은 리프레시 토큰");
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        // 사용자명 추출 및 사용자 조회
        String username = jwtUtil.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("토큰의 사용자를 찾을 수 없음: username={}", username);
                    return new IllegalArgumentException("사용자를 찾을 수 없습니다.");
                });

        // 새 액세스 토큰 생성
        String newAccessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getId());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        log.info("토큰 갱신 완료: username={}, userId={}", user.getUsername(), user.getId());

        return LoginResponse.of(user, newAccessToken, newRefreshToken);
    }

    public boolean validateToken(String token) {
        log.info("=== 토큰 검증 시작 ===");
        log.info("토큰 길이: {}", token.length());

        try{
            boolean isValid = jwtUtil.isTokenValid(token) && jwtUtil.isAccessToken(token);
            log.info("토큰 검증 결과: valid={}", isValid);

            if(isValid){
                String username = jwtUtil.extractUsername(token);
                Long userId = jwtUtil.extractUserId(token);
                log.info("토큰 정보 추출: username={}, userId={}", username, userId);
            }

            return isValid;
        } catch(Exception e){
            log.error("토큰 검증 중 오류 발생: {}", e.getMessage());
            return false;
        }
    }

    public User getUserFromToken(String token) {
        log.info("=== 토큰에서 사용자 정보 추출 시작 ===");

        if(!validateToken(token)) {
            log.warn("유효하지 않은 토큰으로 사용자 추출 정보 시도");
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        String username = jwtUtil.extractUsername(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("토큰의 사용자를 찾을 수 없음: username={}", username);
                    return new IllegalArgumentException("사용자를 찾을 수 없습니다.");
                });

        log.info("토큰에서 사용자 정보 추출 완료: username={}, userId={}",
                user.getUsername(), user.getId());

        return user;
    }
}
