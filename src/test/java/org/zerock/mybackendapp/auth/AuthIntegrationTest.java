package org.zerock.mybackendapp.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.mybackendapp.auth.dto.LoginRequest;
import org.zerock.mybackendapp.auth.dto.RegisterRequest;
import org.zerock.mybackendapp.user.repository.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "jwt.secret=testSecretKeyForJWTTokenGenerationThatIsAtLeast256BitsLongForTesting",
        "jwt.access-token-expiration=3600000",
        "jwt.refresh-token-expiration=604800000"
})
@Transactional
@Slf4j
@DisplayName("인증 기능 통합 테스트")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        log.info("=== 인증 테스트 데이터 초기화 ===");
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("회원가입 성공 테스트")
    void registerSuccess() throws Exception {
        log.info("=== 회원가입 성공 테스트 시작 ===");

        RegisterRequest request = RegisterRequest.of("testuser", "test@example.com", "password123");

        log.info("회원가입 요청: username={}, email={}", request.getUsername(), request.getEmail());

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
                .andExpect(jsonPath("$.password").doesNotExist()); // 패스워드는 응답에 포함되지 않아야 함

        log.info("=== 회원가입 성공 테스트 완료 ===");
    }

    @Test
    @DisplayName("로그인 성공 테스트")
    void loginSuccess() throws Exception {
        log.info("=== 로그인 성공 테스트 시작 ===");

        // 1. 먼저 회원가입
        RegisterRequest registerRequest = RegisterRequest.of("loginuser", "login@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        log.info("회원가입 완료: username={}", registerRequest.getUsername());

        // 2. 로그인 시도
        LoginRequest loginRequest = LoginRequest.of("loginuser", "password123");

        log.info("로그인 요청: username={}", loginRequest.getUsername());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("loginuser"))
                .andExpect(jsonPath("$.email").value("login@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());

        log.info("=== 로그인 성공 테스트 완료 ===");
    }

    @Test
    @DisplayName("JWT 토큰을 통한 인증된 API 접근 테스트")
    void authenticatedApiAccess() throws Exception {
        log.info("=== JWT 인증된 API 접근 테스트 시작 ===");

        // 1. 회원가입 및 로그인으로 토큰 획득
        RegisterRequest registerRequest = RegisterRequest.of("authuser", "auth@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = LoginRequest.of("authuser", "password123");

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 토큰 추출
        var responseMap = objectMapper.readValue(loginResponse, java.util.Map.class);
        String accessToken = (String) responseMap.get("accessToken");

        log.info("액세스 토큰 획득: 길이={}", accessToken.length());

        // 2. 토큰으로 인증이 필요한 API 접근 (게시글 생성)
        var postRequest = java.util.Map.of(
                "title", "인증된 게시글",
                "content", "JWT 토큰으로 인증된 사용자가 작성한 게시글",
                "authorId", responseMap.get("userId")
        );

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("인증된 게시글"))
                .andExpect(jsonPath("$.author.username").value("authuser"));

        log.info("인증된 게시글 생성 성공");

        // 3. 현재 사용자 정보 조회
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("authuser"))
                .andExpect(jsonPath("$.email").value("auth@example.com"));

        log.info("=== JWT 인증된 API 접근 테스트 완료 ===");
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 API 접근 실패 테스트")
    void invalidTokenAccess() throws Exception {
        log.info("=== 유효하지 않은 토큰 접근 실패 테스트 시작 ===");

        String invalidToken = "invalid.jwt.token";

        log.info("유효하지 않은 토큰으로 API 접근 시도: {}", invalidToken);

        // 인증이 필요한 API에 유효하지 않은 토큰으로 접근
        var postRequest = java.util.Map.of(
                "title", "실패할 게시글",
                "content", "유효하지 않은 토큰으로 작성 시도",
                "authorId", 1L
        );

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + invalidToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        log.info("유효하지 않은 토큰으로 접근 차단 확인");
        log.info("=== 유효하지 않은 토큰 접근 실패 테스트 완료 ===");
    }

    @Test
    @DisplayName("토큰 없이 인증 필요 API 접근 실패 테스트")
    void noTokenAccess() throws Exception {
        log.info("=== 토큰 없이 인증 필요 API 접근 실패 테스트 시작 ===");

        var postRequest = java.util.Map.of(
                "title", "토큰 없는 게시글",
                "content", "토큰 없이 작성 시도",
                "authorId", 1L
        );

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        log.info("토큰 없이 접근 차단 확인");
        log.info("=== 토큰 없이 인증 필요 API 접근 실패 테스트 완료 ===");
    }

    @Test
    @DisplayName("인증 불필요 API는 토큰 없이도 접근 가능 테스트")
    void publicApiAccess() throws Exception {
        log.info("=== 공개 API 접근 테스트 시작 ===");

        // 게시글 목록 조회 (인증 불필요)
        mockMvc.perform(get("/api/posts"))
                .andDo(print())
                .andExpect(status().isOk());

        log.info("공개 API 접근 성공 확인");

        // 사용자 목록 조회 (인증 불필요)
        mockMvc.perform(get("/api/users"))
                .andDo(print())
                .andExpect(status().isOk());

        log.info("=== 공개 API 접근 테스트 완료 ===");
    }

}
