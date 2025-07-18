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
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
})
@Transactional
@Slf4j
@DisplayName("User 전체 통합 테스트")
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
    @DisplayName("User 도메인 전체 플로우 테스트")
    void userDomainFullFlowTest() throws Exception {
        log.info("=== User 도메인 전체 플로우 테스트 시작 ===");

        // 1. API로 사용자 생성
        String username = "fullflowuser";
        String email = "fullflow@example.com";
        Map<String, String> createRequest = Map.of(
                "username", username,
                "email", email
        );

        String response = mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.email").value(email))
                .andReturn()
                .getResponse()
                .getContentAsString();

        User createdUser = objectMapper.readValue(response, User.class);
        log.info("API로 사용자 생성 완료: {}", createdUser);

        // 2. Service로 사용자 조회
        User serviceUser = userService.getUserById(createdUser.getId()).orElseThrow();
        log.info("Service로 조회한 사용자: {}", serviceUser);

        // 3. Repository로 직접 조회
        User repoUser = userRepository.findByUsername(username).orElseThrow();
        log.info("Repository로 조회한 사용자: {}", repoUser);

        // 4. 도메인 로직 테스트
        String newEmail = "update@example.com";
        serviceUser.updateEmail(newEmail);
        User updatedUser = userRepository.save(serviceUser);
        log.info("도메인 로직으로 이메일 업데이트: {}", updatedUser);

        // 5. 검증
        assertThat(createdUser.getId()).isEqualTo(serviceUser.getId());
        assertThat(serviceUser.getId()).isEqualTo(repoUser.getId());
        // 시간 대신 데이터 변경 확인
        assertThat(updatedUser.getEmail()).isEqualTo(newEmail);
        assertThat(updatedUser.getEmail()).isNotEqualTo("fullflow@example.com"); // 원래 이메일과 다른지 확인

        log.info("=== User 도메인 전체 플로우 테스트 완료 ===");
    }

    @Test
    @DisplayName("실제 PostgreSQL 연동 확인")
    void postgresqlIntegrationTest() throws Exception {
        log.info("=== PostgreSQL 연동 확인 테스트 시작 ===");

        // 1. 여러 사용자 생성
        User user1 = userService.createUser("pguser1", "pg1@example.com");
        User user2 = userService.createUser("pguser2", "pg2@example.com");
        User user3 = userService.createUser("pguser3", "pg3@example.com");

        log.info("PostgreSQL에 사용자 3명 생성 완료");

        // 2. 데이터 조회 및 검증
        var allUsers = userService.getAllUsers();
        assertThat(allUsers).hasSize(3);

        // 3. 검색 기능 테스트
        var searchResults = userRepository.findByUsernameContaining("pguser");
        assertThat(searchResults).hasSize(3);

        // 4. 중복 방지 테스트
        assertThatThrownBy(() -> userService.createUser("pguser1", "duplicate@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 존재하는 사용자명");

        log.info("PostgreSQL 연동 확인 완료: 모든 기능 정상 동작");
        log.info("=== PostgreSQL 연동 확인 테스트 완료 ===");
    }

    @Test
    @DisplayName("API 전체 CRUD 통합 테스트")
    void apiCrudIntegrationTest() throws Exception {
        log.info("=== API 전체 CRUD 통합 테스트 시작 ===");

        // 1. CREATE - 사용자 생성
        String username = "cruduser";
        String email = "crud@example.com";
        Map<String, String> createRequest = Map.of(
                "username", username,
                "email", email
        );

        String createResponse = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        User createdUser = objectMapper.readValue(createResponse, User.class);
        Long userId = createdUser.getId();
        log.info("CREATE 완료: {}", createdUser);

        // 2. READ - 모든 사용자 조회
        mockMvc.perform(get("/api/users"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value(username));

        log.info("READ ALL 완료");

        // 3. READ - ID로 사용자 조회
        mockMvc.perform(get("/api/users/{id}", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.email").value(email));

        log.info("READ BY ID 완료");

        // 4. READ - 사용자명으로 조회
        mockMvc.perform(get("/api/users/username/{username}", username))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.email").value(email));

        log.info("READ BY USERNAME 완료");

        // 5. DB 직접 검증
        User dbUser = userRepository.findById(userId).orElseThrow();
        assertThat(dbUser.getUsername()).isEqualTo(username);
        assertThat(dbUser.getEmail()).isEqualTo(email);
        assertThat(dbUser.getCreatedAt()).isNotNull();
        assertThat(dbUser.getUpdatedAt()).isNotNull();

        log.info("DB 직접 검증 완료: {}", dbUser);
        log.info("=== API 전체 CRUD 통합 테스트 완료 ===");
    }

    @Test
    @DisplayName("에러 시나리오 통합 테스트")
    void errorScenarioIntegrationTest() throws Exception {
        log.info("=== 에러 시나리오 통합 테스트 시작 ===");

        // 1. 필수 필드 누락
        Map<String, String> invalidRequest = Map.of("username", "onlyusername");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        log.info("필수 필드 누락 에러 처리 확인");

        // 2. 중복 사용자명
        String username = "duplicateuser";
        User firstUser = userService.createUser(username, "first@example.com");
        log.info("첫 번째 사용자 생성: {}", firstUser);

        Map<String, String> duplicateRequest = Map.of(
                "username", username,
                "email", "second@example.com"
        );

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("이미 존재하는 사용자명입니다: " + username));

        log.info("중복 사용자명 에러 처리 확인");

        // 3. 존재하지 않는 사용자 조회
        mockMvc.perform(get("/api/users/{id}", 99999L))
                .andDo(print())
                .andExpect(status().isNotFound());

        log.info("존재하지 않는 사용자 조회 에러 처리 확인");

        // 4. DB 상태 확인 (중복 방지가 제대로 작동했는지)
        long userCount = userRepository.count();
        assertThat(userCount).isEqualTo(1); // 첫 번째 사용자만 저장되어야 함

        log.info("DB 상태 확인 완료: 총 사용자 수 = {}", userCount);
        log.info("=== 에러 시나리오 통합 테스트 완료 ===");
    }
}
