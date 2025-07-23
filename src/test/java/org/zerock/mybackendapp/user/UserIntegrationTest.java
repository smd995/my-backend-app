package org.zerock.mybackendapp.user;

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
import org.zerock.mybackendapp.user.domain.User;
import org.zerock.mybackendapp.user.repository.UserRepository;
import org.zerock.mybackendapp.user.service.UserService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest()
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "jwt.secret=testSecretKeyForJWTTokenGenerationThatIsAtLeast256BitsLongForTesting",
        "jwt.access-token-expiration=3600000",
        "jwt.refresh-token-expiration=604800000"
})
@Transactional
@Slf4j
@DisplayName("User 전체 통합 테스트 (JWT 인증 포함)")
class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        log.info("=== 통합 테스트 데이터 초기화 ===");
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("User 도메인 전체 플로우 테스트 (JWT 인증 포함)")
    void userDomainFullFlowTest() throws Exception {
        log.info("=== User 도메인 전체 플로우 테스트 시작 ===");

        // 1. JWT 회원가입 API로 사용자 생성
        String username = "fullflowuser";
        String email = "fullflow@example.com";
        String password = "password123";

        RegisterRequest registerRequest = RegisterRequest.of(username, email, password);

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
                .andExpect(jsonPath("$.password").doesNotExist()) // 패스워드는 응답에 포함되지 않아야 함
                .andReturn()
                .getResponse()
                .getContentAsString();

        var createdUserMap = objectMapper.readValue(response, Map.class);
        Long userId = ((Number) createdUserMap.get("id")).longValue();
        log.info("JWT API로 사용자 생성 완료: userId={}, username={}", userId, username);

        // 2. JWT 로그인 API로 토큰 획득
        LoginRequest loginRequest = LoginRequest.of(username, password);

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var loginMap = objectMapper.readValue(loginResponse, Map.class);
        String accessToken = (String) loginMap.get("accessToken");

        log.info("JWT 로그인 완료: 토큰 길이={}", accessToken.length());

        // 3. Service로 사용자 조회
        User serviceUser = userService.getUserById(userId).orElseThrow();
        log.info("Service로 조회한 사용자: {}", serviceUser);

        // 4. Repository로 직접 조회
        User repoUser = userRepository.findByUsername(username).orElseThrow();
        log.info("Repository로 조회한 사용자: {}", repoUser);

        // 5. JWT 토큰으로 현재 사용자 정보 조회
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"));

        log.info("JWT 토큰으로 현재 사용자 정보 조회 성공");

        // 6. 검증
        assertThat(userId).isEqualTo(serviceUser.getId());
        assertThat(serviceUser.getId()).isEqualTo(repoUser.getId());
        assertThat(serviceUser.getUsername()).isEqualTo(username);
        assertThat(serviceUser.getEmail()).isEqualTo(email);

        log.info("=== User 도메인 전체 플로우 테스트 완료 ===");
    }

    @Test
    @DisplayName("JWT 인증 시스템 통합 테스트")
    void jwtAuthenticationSystemTest() throws Exception {
        log.info("=== JWT 인증 시스템 통합 테스트 시작 ===");

        // 1. 회원가입
        RegisterRequest registerRequest = RegisterRequest.of("authuser", "auth@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        log.info("회원가입 완료");

        // 2. 로그인 및 토큰 획득
        LoginRequest loginRequest = LoginRequest.of("authuser", "password123");

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var loginMap = objectMapper.readValue(loginResponse, Map.class);
        String accessToken = (String) loginMap.get("accessToken");
        String refreshToken = (String) loginMap.get("refreshToken");

        log.info("로그인 및 토큰 획득 완료");

        // 3. 액세스 토큰 검증
        mockMvc.perform(post("/api/auth/validate")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.user.username").value("authuser"));

        log.info("액세스 토큰 검증 성공");

        // 4. 리프레시 토큰으로 새 토큰 발급
        Map<String, String> refreshRequest = Map.of("refreshToken", refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("authuser"))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());

        log.info("리프레시 토큰으로 새 토큰 발급 성공");

        // 5. 유효하지 않은 토큰 검증
        mockMvc.perform(post("/api/auth/validate")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false));

        log.info("유효하지 않은 토큰 검증 실패 확인");

        log.info("=== JWT 인증 시스템 통합 테스트 완료 ===");
    }

    @Test
    @DisplayName("실제 PostgreSQL 연동 확인 (JWT 포함)")
    void postgresqlIntegrationWithJWTTest() throws Exception {
        log.info("=== PostgreSQL JWT 연동 확인 테스트 시작 ===");

        // 1. 여러 사용자 JWT 회원가입
        String[] users = {"pguser1", "pguser2", "pguser3"};

        for (String username : users) {
            RegisterRequest request = RegisterRequest.of(username, username + "@example.com", "password123");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        log.info("PostgreSQL에 JWT로 사용자 3명 생성 완료");

        // 2. 공개 API로 데이터 조회 및 검증
        mockMvc.perform(get("/api/users"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        log.info("공개 API로 사용자 목록 조회 성공");

        // 3. 중복 방지 테스트 (JWT API)
        RegisterRequest duplicateRequest = RegisterRequest.of("pguser1", "duplicate@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("이미 존재하는 사용자명입니다: pguser1"));

        log.info("JWT API 중복 사용자명 방지 확인");

        // 4. 잘못된 로그인 시도
        LoginRequest wrongPasswordRequest = LoginRequest.of("pguser1", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPasswordRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("패스워드가 일치하지 않습니다."));

        log.info("잘못된 패스워드 로그인 차단 확인");

        log.info("PostgreSQL JWT 연동 확인 완료: 모든 기능 정상 동작");
        log.info("=== PostgreSQL JWT 연동 확인 테스트 완료 ===");
    }

    @Test
    @DisplayName("JWT 에러 시나리오 통합 테스트")
    void jwtErrorScenarioIntegrationTest() throws Exception {
        log.info("=== JWT 에러 시나리오 통합 테스트 시작 ===");

        // 1. 회원가입 필수 필드 누락 - 400 Bad Request (유효성 검증 실패)
        Map<String, String> invalidRegisterRequest = Map.of("username", "onlyusername");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRegisterRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest()); // 400이 맞음

        log.info("회원가입 필수 필드 누락 에러 처리 확인 (400 Bad Request)");

        // 2. 존재하지 않는 사용자 로그인 시도 - 401 Unauthorized
        LoginRequest nonExistentUserLogin = LoginRequest.of("nonexistent", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistentUserLogin)))
                .andDo(print())
                .andExpect(status().isUnauthorized()) // 401이 맞음
                .andExpect(jsonPath("$.error").value("사용자를 찾을 수 없습니다."));

        log.info("존재하지 않는 사용자 로그인 차단 확인 (401 Unauthorized)");

        // 3. 유효하지 않은 리프레시 토큰 - 401 Unauthorized
        Map<String, String> invalidRefreshRequest = Map.of("refreshToken", "invalid.refresh.token");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRefreshRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized()) // 401이 맞음
                .andExpect(jsonPath("$.error").exists());

        log.info("유효하지 않은 리프레시 토큰 차단 확인 (401 Unauthorized)");

        // 4. Authorization 헤더 없이 보호된 API 접근 - 401 Unauthorized
        mockMvc.perform(get("/api/auth/me"))
                .andDo(print())
                .andExpect(status().isUnauthorized()); // 401이 맞음

        log.info("Authorization 헤더 없이 보호된 API 접근 차단 확인 (401 Unauthorized)");

        // 5. 잘못된 패스워드 로그인 - 401 Unauthorized
        // 먼저 유효한 사용자 생성
        RegisterRequest validUser = RegisterRequest.of("testuser", "test@example.com", "correctpassword");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUser)))
                .andExpect(status().isCreated());

        LoginRequest wrongPasswordLogin = LoginRequest.of("testuser", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPasswordLogin)))
                .andDo(print())
                .andExpect(status().isUnauthorized()) // 401이 맞음
                .andExpect(jsonPath("$.error").value("패스워드가 일치하지 않습니다."));

        log.info("잘못된 패스워드 로그인 차단 확인 (401 Unauthorized)");

        log.info("=== JWT 에러 시나리오 통합 테스트 완료 ===");
    }
}
