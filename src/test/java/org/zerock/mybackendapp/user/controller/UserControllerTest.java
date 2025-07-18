package org.zerock.mybackendapp.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.zerock.mybackendapp.user.domain.User;
import org.zerock.mybackendapp.user.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class,
        excludeAutoConfiguration = org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
)
@Slf4j
@DisplayName("User Controller 테스트")
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("사용자 생성 API 성공")
    void createUser_Success() throws Exception {
        log.info("=== 사용자 생성 API 성공 테스트 시작 ===");

        // Given
        String username = "testuser";
        String email = "test@example.com";
        Map<String, String> request = Map.of(
                "username", username,
                "email", email
        );

        User mockUser = User.of(username, email);
        given(userService.createUser(username, email)).willReturn(mockUser);

        log.info("요청 데이터: {}", request);

        // When & Then
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.email").value(email));

        verify(userService).createUser(username, email);

        log.info("=== 사용자 생성 API 성공 테스트 완료 ===");
    }

    @Test
    @DisplayName("사용자 생성 API - 필수 필드 누락")
    void createUser_MissingFields() throws Exception {
        log.info("=== 사용자 생성 API 필수 필드 누락 테스트 시작 ===");

        // Given
        Map<String, String> request = Map.of("username", "testuser");
        // email 필드 누락

        log.info("불완전한 요청 데이터: {}", request);

        // When & Then
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verify(userService, never()).createUser(anyString(), anyString());

        log.info("=== 사용자 생성 API 필수 필드 누락 테스트 완료 ===");
    }

    @Test
    @DisplayName("사용자 생성 API - 중복 사용자명")
    void createUser_DuplicateUsername() throws Exception {
        log.info("=== 사용자 생성 API 중복 사용자명 테스트 시작 ===");

        // Given
        String username = "duplicateuser";
        String email = "test@example.com";

        Map<String, String> request = Map.of(
                "username", username,
                "email", email
        );

        given(userService.createUser(username, email))
                .willThrow(new IllegalArgumentException("이미 존재하는 사용자명입니다: " + username));

        log.info("중복 사용자명 요청: {}", username);

        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("이미 존재하는 사용자명입니다: " + username));

        verify(userService).createUser(username, email);

        log.info("=== 사용자 생성 API 중복 사용자명 테스트 완료 ===");

    }

    @Test
    @DisplayName("모든 사용자 조회 API")
    void getAllUsers() throws Exception {
        log.info("=== 모든 사용자 조회 API 테스트 시작 ===");

        // Given
        User user1 = User.of("user1", "user1@example.com");
        User user2 = User.of("user2", "user2@example.com");
        List<User> mockUsers = List.of(user1, user2);

        given(userService.getAllUsers()).willReturn(mockUsers);

        log.info("Mock 사용자 데이터: {} 명",  mockUsers.size());

        // When & Then
        mockMvc.perform(get("/api/users"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").value("user1"))
                .andExpect(jsonPath("$[1].username").value("user2"));

        verify(userService).getAllUsers();

        log.info("=== 모든 사용자 조회 API 테스트 완료 ===");
    }

    @Test
    @DisplayName("ID로 사용자 조회 API 성공")
    void getUserById_Success() throws Exception {
        log.info("=== ID로 사용자 조회 API 성공 테스트 시작 ===");

        // Given
        Long userId = 1L;
        User mockUser = User.of("testuser", "test@example.com");

        given(userService.getUserById(userId)).willReturn(Optional.of(mockUser));

        log.info("조회할 사용자 ID: {}", userId);

        // When & Then
        mockMvc.perform(get("/api/users/{id}", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(userService).getUserById(userId);

        log.info("=== ID로 사용자 조회 API 성공 테스트 완료 ===");
    }

    @Test
    @DisplayName("ID로 사용자 조회 API - 사용자 없음")
    void getUserById_NotFound() throws Exception {
        log.info("=== ID로 사용자 조회 API 사용자 없음 테스트 시작 ===");

        // Given
        Long userId = 999L;
        given(userService.getUserById(userId)).willReturn(Optional.empty());

        log.info("존재하지 않는 사용자 ID: {}", userId);

        // When & Then
        mockMvc.perform(get("/api/users/{id}", userId))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(userService).getUserById(userId);

        log.info("=== ID로 사용자 조회 API 사용자 없음 테스트 완료 ===");
    }

    @Test
    @DisplayName("사용자명으로 조회 API")
    void getUserByUsername() throws Exception {
        log.info("=== 사용자명으로 조회 API 테스트 시작 ===");

        // Given
        String username = "searchuser";
        User mockUser = User.of(username, "search@example.com");

        given(userService.getUserByUsername(username)).willReturn(Optional.of(mockUser));

        log.info("조회할 사용자명: {}", username);

        // When & Then
        mockMvc.perform(get("/api/users/username/{username}", username))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.email").value("search@example.com"));

        verify(userService).getUserByUsername(username);

        log.info("=== 사용자명으로 조회 API 테스트 완료 ===");
    }
}
