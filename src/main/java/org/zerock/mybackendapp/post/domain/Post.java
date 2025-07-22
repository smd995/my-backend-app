package org.zerock.mybackendapp.post.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.zerock.mybackendapp.user.domain.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 정적 팩토리 메서드
    public static Post of(String title, String content, User author) {
        validateTitle(title);
        validateContent(content);
        validateAuthor(author);

        Post post = new Post();
        post.title = title;
        post.content = content;
        post.author = author;

        log.info("새 게시글 도메인 객체 생성: title={}, author={}", title, author.getUsername());

        return post;
    }

    // 도메인 로직 - 제목 업데이트
    public void updateTitle(String newTitle) {
        validateTitle(newTitle);
        log.info("게시글 제목 업데이트: {} -> {}", this.title, newTitle);
        this.title = newTitle;
    }

    public void updateContent(String newContent) {
        validateContent(newContent);
        log.info("게시글 내용 업데이트: 길이 {} -> {}", this.content, newContent);
        this.content = newContent;
    }

    // 검증 로직
    private static void validateTitle(String title) {
        if(title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("제목은 필수입니다.");
        }
        if(title.length() > 200) {
            throw new IllegalArgumentException("제목은 200자를 초과할 수 없습니다.");
        }
    }

    private static void validateContent(String content) {
        if(content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("내용은 필수입니다.");
        }
    }

    private static void validateAuthor(User author) {
        if(author == null) {
            throw new IllegalArgumentException("작성자는 필수입니다.");
        }
    }

    @PrePersist
    protected void onCreate() {
        log.info("게시글 엔티티 저장: title={}, author={}", title, author.getUsername());
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        log.info("게시글 엔티티 업데이트: title={}", title);
        updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("Post{id=%d, title='%s', author='%s'}", id, title, author != null ? author.getUsername() : "null");
    }

    // 비즈니스 로직
    public boolean isAuthoredBy(User user) {
        return this.author.getId().equals(user.getId());
    }

    public boolean hasTitle(String title) {
        return this.title.equals(title);
    }
}
