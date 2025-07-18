package org.zerock.mybackendapp.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
public class User {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 정적 팩토리 메서드
    public static User of(String username, String email) {
        validateUsername(username);
        validateEmail(email);

        User user = new User();
        user.username = username;
        user.email = email;

        log.info("새 사용자 도메인 객체 생성: username={}, email={}", username, email);

        return user;
    }

    // 도메인 로직 - 이메일 업데이트
    public void updateEmail(String newEmail) {
        validateEmail(newEmail);
        log.info("사용자 이메일 업데이트: {} => {}", this.email, newEmail);
        this.email = newEmail;
    }

    private static void validateUsername(String username) {
        if(username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자명은 필수입니다.");
        }
        if(username.length() > 50) {
            throw new IllegalArgumentException("사용자명은 50자를 초과할 수 없습니다.");
        }
    }

    private static void validateEmail(String email) {
        if(email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }

        if(!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("유효하지 않은 이메일 형식입니다.");
        }
    }

    @PrePersist
    protected void onCreate() {
        log.info("사용자 엔티티 저장: username={]", username);
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        log.info("사용자 엔티티 업데이트: username={}", username);
        updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("User{id=%d, username='%s', email='%s'}", id, username, email);
    }
}
