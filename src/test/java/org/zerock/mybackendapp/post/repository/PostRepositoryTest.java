package org.zerock.mybackendapp.post.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.zerock.mybackendapp.post.domain.Post;
import org.zerock.mybackendapp.user.domain.User;
import org.zerock.mybackendapp.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "logging.level.org.hibernate.SQL=DEBUG"
})
@Slf4j
@DisplayName("Post Repository 테스트")
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private User testAuthor;

    @BeforeEach
    void setUp() {
        log.info("=== 테스트 데이터 정리 ===");
        postRepository.deleteAll();
        userRepository.deleteAll();

        // 테스트용 작성자 생성
        testAuthor = userRepository.save(User.of("testauthor", "author@example.com"));
        log.info("테스트 작성자 생성: {}", testAuthor);
    }

    @Test
    @DisplayName("게시글 저장 및 ID로 조회")
    void saveAndFindById() {
        log.info("=== 게시글 저장 및 ID로 조회 테스트 시작 ===");

        // Given
        Post post = Post.of("테스트 게시글", "테스트 내용", testAuthor);

        // When
        Post savedPost = postRepository.save(post);
        log.info("게시글 저장: {}", savedPost);

        Optional<Post> foundPost = postRepository.findById(savedPost.getId());

        // Then
        assertThat(foundPost).isPresent();
        assertThat(foundPost.get().getTitle()).isEqualTo("테스트 게시글");
        assertThat(foundPost.get().getContent()).isEqualTo("테스트 내용");
        assertThat(foundPost.get().getAuthor().getUsername()).isEqualTo("testauthor");

        log.info("ID로 조회된 게시글: {}", foundPost.get());
        log.info("=== 게시글 저장 및 ID로 조회 테스트 완료 ===");
    }

    @Test
    @DisplayName("작성자로 게시글 조회")
    void findByAuthor() {
        log.info("=== 작성자로 게시글 조회 테스트 시작 ===");

        // Given
        Post post1 = postRepository.save(Post.of("첫 번째 게시글", "첫 번째 내용", testAuthor));
        Post post2 = postRepository.save(Post.of("두 번째 게시글", "두 번째 내용", testAuthor));

        // 다른 작성자의 게시글
        User anotherAuthor = userRepository.save(User.of("another", "another@example.com"));
        Post post3 = postRepository.save(Post.of("다른 작성자 게시글", "다른 내용", anotherAuthor));

        log.info("테스트 게시글 3개 생성 완료");

        // When
        List<Post> authorPosts = postRepository.findByAuthor(testAuthor);

        // Then
        assertThat(authorPosts).hasSize(2);
        assertThat(authorPosts).extracting(Post::getTitle)
                .containsExactlyInAnyOrder("첫 번째 게시글", "두 번째 게시글");

        log.info("작성자로 조회된 게시글: {} 개", authorPosts.size());
        authorPosts.forEach(post -> log.info("  - {}", post));
        log.info("=== 작성자로 게시글 조회 테스트 완료 ===");
    }

    @Test
    @DisplayName("제목으로 게시글 검색")
    void findByTitleContaining() {
        log.info("=== 제목으로 게시글 검색 테스트 시작 ===");

        // Given
        Post post1 = postRepository.save(Post.of("Spring Boot 튜토리얼", "내용1", testAuthor));
        Post post2 = postRepository.save(Post.of("Spring Security 가이드", "내용2", testAuthor));
        Post post3 = postRepository.save(Post.of("React 기초", "내용3", testAuthor));

        log.info("검색용 게시글 3개 생성 완료");

        // When
        List<Post> springPosts = postRepository.findByTitleContaining("Spring");

        // Then
        assertThat(springPosts).hasSize(2);
        assertThat(springPosts).extracting(Post::getTitle)
                .containsExactlyInAnyOrder("Spring Boot 튜토리얼", "Spring Security 가이드");

        log.info("'Spring'으로 검색된 게시글: {} 개", springPosts.size());
        springPosts.forEach(post -> log.info("  - {}", post.getTitle()));
        log.info("=== 제목으로 게시글 검색 테스트 완료 ===");
    }

    @Test
    @DisplayName("최신 게시글 순으로 조회")
    void findAllByOrderByCreatedAtDesc() {
        log.info("=== 최신 게시글 순으로 조회 테스트 시작 ===");

        // Given
        Post post1 = postRepository.save(Post.of("첫 번째", "내용1", testAuthor));
        Post post2 = postRepository.save(Post.of("두 번째", "내용2", testAuthor));
        Post post3 = postRepository.save(Post.of("세 번째", "내용3", testAuthor));

        log.info("게시글 3개 순차 생성 완료");

        // When
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc();

        // Then
        assertThat(posts).hasSize(3);
        assertThat(posts.get(0).getTitle()).isEqualTo("세 번째");
        assertThat(posts.get(1).getTitle()).isEqualTo("두 번째");
        assertThat(posts.get(2).getTitle()).isEqualTo("첫 번째");

        log.info("최신 순 조회 결과:");
        posts.forEach(post -> log.info("  - {}", post.getTitle()));
        log.info("=== 최신 게시글 순으로 조회 테스트 완료 ===");
    }

    @Test
    @DisplayName("전체 게시글 수 확인")
    void countAllPosts() {
        log.info("=== 전체 게시글 수 확인 테스트 시작 ===");

        // Given
        postRepository.save(Post.of("게시글 1", "내용 1", testAuthor));
        postRepository.save(Post.of("게시글 2", "내용 2", testAuthor));
        postRepository.save(Post.of("게시글 3", "내용 3", testAuthor));

        // When
        long count = postRepository.count();

        // Then
        assertThat(count).isEqualTo(3);

        log.info("전체 게시글 수: {}", count);
        log.info("=== 전체 게시글 수 확인 테스트 완료 ===");
    }

}
