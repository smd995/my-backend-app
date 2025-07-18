package org.zerock.mybackendapp.user.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zerock.mybackendapp.user.domain.User;
import org.zerock.mybackendapp.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@Slf4j
@DisplayName("User Service 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        log.info("=== UserService 테스트 준비 ===");
    }

    @Test
    @DisplayName("새 사용자 생성 성공")
    void createUser_Success() {
        log.info("=== 새 사용자 생성 성공 테스트 시작 ===");

        // Given
        String username = "newuser";
        String email = "new@example.com";

        given(userRepository.existsByUsername(username)).willReturn(false);
        given(userRepository.existsByEmail(email)).willReturn(false);
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User user = invocation.getArgument(0);
            // ID가 자동 생성되었다고 가정
            return User.of(user.getUsername(), user.getEmail());
        });

        log.info("Mock 설정 완료: username={}, email={}", username, email);

        // When
        User createdUser = userService.createUser(username, email);

        log.info("사용자 생성 완료: {}", createdUser);

        // Then
        assertThat(createdUser.getUsername()).isEqualTo(username);
        assertThat(createdUser.getEmail()).isEqualTo(email);

        verify(userRepository).existsByUsername(username);
        verify(userRepository).existsByEmail(email);
        verify(userRepository).save(any(User.class));

        log.info("=== 새 사용자 생성 성공 테스트 완료 ===");
    }

    @Test
    @DisplayName("중복 사용자명으로 생성 실패")
    void createUser_DuplicateUsername() {
        log.info("===  중복 사용자명 생성 실패 테스트 시작 ===");

        // Given
        String username = "duplicateuser";
        String email = "test@example.com";

        given(userRepository.existsByUsername(username)).willReturn(true);

        log.info("중복 사용자명 설정:　｛｝", username);

        // When & Then
        assertThatThrownBy(() -> userService.createUser(username, email))
                .isInstanceOf((IllegalArgumentException.class))
                .hasMessageContaining("이미 존재하는 사용자명");

        verify(userRepository).existsByUsername(username);
        verify(userRepository, never()).save(any(User.class));

        log.info("=== 중복 사용자명 생성 실패 테스트 완료 ===");
    }

    @Test
    @DisplayName("중복 이메일로 생성 실패")
    void createUser_DuplicateEmail() {
        log.info("=== 중복 이메일 생성 실패 테스트 시작 ===");

        // Given
        String username = "testuser";
        String email = "duplicate@example.com";

        given(userRepository.existsByUsername(username)).willReturn(false);
        given(userRepository.existsByEmail(email)).willReturn(true);

        log.info("중복 이메일 설정: {}", email);

        // When & Then
        assertThatThrownBy(() -> userService.createUser(username, email))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 존재하는 이메일");

        verify(userRepository).existsByUsername(username);
        verify(userRepository).existsByEmail(email);
        verify(userRepository, never()).save(any(User.class));

        log.info("=== 중복 이메일 생성 실패 테스트 완료 ===");
    }

    @Test
    @DisplayName("모든 사용자 조회")
    void getAllUsers() {
        log.info("=== 모든 사용자 조회 테스트 시작 ===");

        // Given
        User user1 = User.of("user1", "user1@example.com");
        User user2 = User.of("user2", "user2@example.com");
        List<User> mockUsers = List.of(user1, user2);

        given(userRepository.findAll()).willReturn(mockUsers);

        log.info("Mock 사용자 목록 설정: {} 명", mockUsers.size());

        // When
        List<User> allUsers = userService.getAllUsers();

        // Then
        assertThat(allUsers).hasSize(2);
        assertThat(allUsers).extracting(User::getUsername)
                .containsExactlyInAnyOrder("user1", "user2");

        verify(userRepository).findAll();

        log.info("조회된 사용자: {} 명", allUsers.size());
        log.info("=== 모든 사용자 조회 테스트 완료 ===");
    }

    @Test
    @DisplayName("ID로 사용자 조회 성공")
    void getUserById_Success() {
        log.info("=== ID로 사용자 조회 성공 테스트 시작 ===");

        // Given
        Long userId = 1L;
        User mockUser = User.of("testuser", "testuser@example.com");

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

        log.info("Mock 사용자 설정: ID={}, username={}", userId, mockUser.getUsername());

        // When
        Optional<User> foundUser = userService.getUserById(userId);

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("testuser");

        verify(userRepository).findById(userId);

        log.info("사용자 조회 성공: {}", foundUser.get());
        log.info("=== ID로 사용자 조회 성공 테스트 완료 ===");
    }

    @Test
    @DisplayName("사용자명으로 조회")
    void getUserByUsername() {
        log.info("=== 사용자명으로 조회 테스트 시작 ===");

        // Given
        String username = "searchuser";
        User mockUser = User.of(username, "search@example.com");

        given(userRepository.findByUsername(username)).willReturn(Optional.of(mockUser));

        log.info("Mock 사용자 설정: username={}", username);

        // When
        Optional<User> foundUser = userService.getUserByUsername(username);

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("search@example.com");

        verify(userRepository).findByUsername(username);

        log.info("사용자명으로 조회 성공: {}", foundUser.get());
        log.info("=== 사용자명으로 조회 테스트 완료 ===");
    }
}
