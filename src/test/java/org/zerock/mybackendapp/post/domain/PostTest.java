package org.zerock.mybackendapp.post.domain;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import org.zerock.mybackendapp.user.domain.User;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "logging.level.org.hibernate.SQL=DEBUG"
})
@Slf4j
@DisplayName("Post Domain 테스트")
class PostTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("유효한 게시글 생성 테스트")
    void createValidPost() {
        log.info("=== 유효한 게시글 생성 테스트 시작 ===");

        // Given
        User author = User.of("postuser", "post@example.com");
        User savedAuthor = entityManager.persistAndFlush(author);

        String title = "테스트 게시글";
        String content = "테스트 게시글 내용입니다.";

        log.info("작성자 저장: {}", savedAuthor);
        log.info("게시글 정보: title={}, content={}", title, content);

        // When
        Post post = Post.of(title, content, savedAuthor);

        // Then
        assertThat(post.getTitle()).isEqualTo(title);
        assertThat(post.getContent()).isEqualTo(content);
        assertThat(post.getAuthor()).isEqualTo(savedAuthor);
        assertThat(post.getId()).isNull();

        log.info("게시글 생성 검증 완료");
        log.info("=== 유효한 게시글 생성 테스트 완료 ===");
    }

    @Test
    @DisplayName("게시글 엔티티 저장 테스트")
    void savePost() {
        log.info("=== 게시글 엔티티 저장 테스트 시작 ===");

        // Given
        User author = User.of("saveuser", "save@example.com");
        User savedAuthor = entityManager.persistAndFlush(author);

        Post post = Post.of("저장 테스트 게시글", "저장 테스트 내용", savedAuthor);

        // When
        Post savedPost = entityManager.persistAndFlush(post);

        log.info("게시글 저장 완료: id={}, title={}", savedPost.getId(), post.getTitle());

        // Then
        assertThat(savedPost.getId()).isNotNull();
        assertThat(savedPost.getCreatedAt()).isNotNull();
        assertThat(savedPost.getUpdatedAt()).isNotNull();
        assertThat(savedPost.getTitle()).isEqualTo("저장 테스트 게시글");
        assertThat(savedPost.getContent()).isEqualTo("저장 테스트 내용");
        assertThat(savedPost.getAuthor()).isEqualTo(savedAuthor);

        log.info("=== 게시글 엔티티 저장 테스트 완료 ===");
    }

    @Test
    @DisplayName("게시글 내용 업데이트 테스트")
    void updatePostContent() throws InterruptedException {
        log.info("=== 게시글 내용 업데이트 테스트 시작 ===");

        // Given
        User author = User.of("updateuser", "update@example.com");
        User savedAuthor = entityManager.persistAndFlush(author);

        Post post = Post.of("업데이트 테스트", "원본 내용", savedAuthor);
        Post savedPost = entityManager.persistAndFlush(post);
        LocalDateTime originalUpdatedAt = savedPost.getUpdatedAt();

        log.info("원본 내용: {}", savedPost.getContent());

        Thread.sleep(10);

        // When
        String newContent = "업데이트된 내용입니다.";
        savedPost.updateContent(newContent);
        Post updatedPost = entityManager.persistAndFlush(savedPost);

        log.info("업데이트된 내용: {}", updatedPost.getContent());

        // Then
        assertThat(updatedPost.getContent()).isEqualTo(newContent);
        assertThat(updatedPost.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(updatedPost.getCreatedAt()).isEqualTo(savedPost.getCreatedAt());

        log.info("=== 게시글 내용 업데이트 테스트 완료 ===");
    }

    @Test
    @DisplayName("게시글 제목과 내용 동시 업데이트 테스트")
    void updatePostTitleAndContent() {
        log.info("=== 게시글 제목과 내용 동시 업데이트 테스트 시작 ===");

        // Given
        User author = User.of("multiupdate", "multi@example.com");
        User savedAuthor = entityManager.persistAndFlush(author);

        Post post = Post.of("원본 제목", "원본 내용", savedAuthor);
        Post savedPost = entityManager.persistAndFlush(post);

        // When
        String newTitle = "업데이트된 제목";
        String newContent = "업데이트된 내용";

        savedPost.updateTitle(newTitle);
        savedPost.updateContent(newContent);

        Post updatedPost = entityManager.persistAndFlush(savedPost);

        log.info("업데이트 결과: title={}, content={}", updatedPost.getTitle(), updatedPost.getContent());

        // Then
        assertThat(updatedPost.getTitle()).isEqualTo(newTitle);
        assertThat(updatedPost.getContent()).isEqualTo(newContent);

        log.info("=== 게시글 제목과 내용 동시 업데이트 테스트 완료 ===");
    }

    @Test
    @DisplayName("잘못된 제목 검증 테스트")
    void validateInvalidTitle() {
        log.info("=== 잘못된 제목 검증 테스트 시작 ===");

        // Given
        User author = User.of("validationuser", "validation@example.com");
        User savedAuthor = entityManager.persistAndFlush(author);

        // When & Then
        assertThatThrownBy(() -> Post.of("", "내용", savedAuthor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("제목은 필수");

        assertThatThrownBy(() -> Post.of(null, "내용", savedAuthor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("제목은 필수");

        // 제목 길이 제한 (200자 초과)
        String longTitle = "a".repeat(201);
        assertThatThrownBy(() -> Post.of(longTitle, "내용", savedAuthor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("제목은 200자를 초과할 수 없습니다");

        log.info("잘못된 제목 검증 완료");
        log.info("=== 잘못된 제목 검증 테스트 완료 ===");
    }

    @Test
    @DisplayName("잘못된 내용 검증 테스트")
    void validateInvalidContent() {
        log.info("=== 잘못된 내용 검증 테스트 시작 ===");

        // Given
        User author = User.of("contentuser", "content@example.com");
        User savedAuthor = entityManager.persistAndFlush(author);

        // When & Then
        assertThatThrownBy(() -> Post.of("제목", "", savedAuthor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("내용은 필수");

        assertThatThrownBy(() -> Post.of("제목", null, savedAuthor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("내용은 필수");

        log.info("잘못된 내용 검증 완료");
        log.info("=== 잘못된 내용 검증 테스트 완료 ===");
    }

    @Test
    @DisplayName("작성자 없는 게시글 검증 테스트")
    void validatePostWithoutAuthor() {
        log.info("=== 작성자 없는 게시글 검증 테스트 시작 ===");

        // When & Then
        assertThatThrownBy(() -> Post.of("제목", "내용", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("작성자는 필수");

        log.info("작성자 없는 게시글 검증 완료");
        log.info("=== 작성자 없는 게시글 검증 테스트 완료 ===");
    }

    @Test
    @DisplayName("게시글 비즈니스 로직 테스트")
    void postBusinessLogicTest() {
        log.info("=== 게시글 비즈니스 로직 테스트 시작 ===");

        // Given
        User author = User.of("businessuser", "business@example.com");
        User anotherUser = User.of("another", "another@example.com");
        User savedAuthor = entityManager.persistAndFlush(author);
        User savedAnotherUser = entityManager.persistAndFlush(anotherUser);

        Post post = Post.of("비즈니스 로직 테스트", "테스트 내용", savedAuthor);
        Post savedPost = entityManager.persistAndFlush(post);

        // When & Then
        assertThat(savedPost.isAuthoredBy(savedAuthor)).isTrue();
        assertThat(savedPost.isAuthoredBy(savedAnotherUser)).isFalse();

        assertThat(savedPost.hasTitle("비즈니스 로직 테스트")).isTrue();
        assertThat(savedPost.hasTitle("다른 제목")).isFalse();

        log.info("비즈니스 로직 검증 완료");
        log.info("=== 게시글 비즈니스 로직 테스트 완료 ===");
    }
}
