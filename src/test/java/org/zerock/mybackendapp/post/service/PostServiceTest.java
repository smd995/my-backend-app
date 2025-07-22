package org.zerock.mybackendapp.post.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.zerock.mybackendapp.post.domain.Post;
import org.zerock.mybackendapp.post.repository.PostRepository;
import org.zerock.mybackendapp.user.domain.User;
import org.zerock.mybackendapp.user.service.UserService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;


@ExtendWith(MockitoExtension.class)
@Slf4j
@DisplayName("Post Service 테스트")
public class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private PostService postService;

    private User mockAuthor;
    private Post mockPost;

    @BeforeEach
    void setUp() {
        log.info("=== PostService 테스트 준비 ===");

        // User 객체 생성 및 ID 설정
        mockAuthor = User.of("testauthor", "author@example.com");
        ReflectionTestUtils.setField(mockAuthor, "id", 1L);

        // Post 객체 생성 및 ID 설정
        mockPost = Post.of("테스트 게시글", "테스트 내용", mockAuthor);
        ReflectionTestUtils.setField(mockPost, "id", 1L);

        log.info("Mock 객체 설정 완료 - Author ID: {}, Post ID: {}",
                mockAuthor.getId(), mockPost.getId());
    }

    @Test
    @DisplayName("새 게시글 생성 성공")
    void createPost_Success() {
        log.info("=== 새 게시글 생성 성공 테스트 시작 ===");

        // Given
        Long authorId = 1L;
        String title = "새 게시글";
        String content = "새 게시글 내용";

        given(userService.getUserById(authorId)).willReturn(Optional.of(mockAuthor));
        given(postRepository.save(any(Post.class))).willAnswer(
                invocation -> {
                    Post post = invocation.getArgument(0);
                    return Post.of(post.getTitle(), post.getContent(), post.getAuthor());
                }
        );

        log.info("Mock 설정 완료: authorid={}, title={}", authorId, title);

        // When
        Post createdPost = postService.createPost(title, content, authorId);

        log.info("게시글 생성 완료: {}", createdPost);

        // Then
        assertThat(createdPost.getTitle()).isEqualTo(title);
        assertThat(createdPost.getContent()).isEqualTo(content);
        assertThat(createdPost.getAuthor()).isEqualTo(mockAuthor);

        verify(userService).getUserById(authorId);
        verify(postRepository).save(any(Post.class));

        log.info("=== 새 게시글 생성 성공 테스트 완료 ===");
    }

    @Test
    @DisplayName("존재하지 않는 작성자로 게시글 생성 실패")
    void createPost_AuthorNotFound() {
        log.info("=== 존재하지 않는 작성자 게시글 생성 실패 테스트 시작 ===");

        // Given
        Long nonExistentAuthorId = 999L;
        String title = "제목";
        String content = "내용";

        given(userService.getUserById(nonExistentAuthorId)).willReturn(Optional.empty());

        log.info("존재하지 않는 작성자 ID: {}", nonExistentAuthorId);

        // When & Then
        assertThatThrownBy(() -> postService.createPost(title, content, nonExistentAuthorId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("작성자를 찾을 수 없습니다");

        verify(userService).getUserById(nonExistentAuthorId);
        verify(postRepository, never()).save(any(Post.class));

        log.info("=== 존재하지 않는 작성자 게시글 생성 실패 테스트 완료 ===");
    }

    @Test
    @DisplayName("모든 게시글 조회")
    void getAllPosts() {
        log.info("=== 모든 게시글 조회 테스트 시작 ===");

        // Given
        Post post1 = Post.of("게시글 1", "내용 1", mockAuthor);
        Post post2 = Post.of("게시글 2", "내용 2", mockAuthor);
        List<Post> mockPosts = List.of(post1, post2);

        given(postRepository.findAllByOrderByCreatedAtDesc()).willReturn(mockPosts);

        log.info("Mock 게시글 목록 설정: {} 개", mockPosts.size());

        // When
        List<Post> allPosts = postService.getAllPosts();

        // Then
        assertThat(allPosts).hasSize(2);
        assertThat(allPosts).extracting(Post::getTitle)
                .containsExactly("게시글 1", "게시글 2");

        verify(postRepository).findAllByOrderByCreatedAtDesc();

        log.info("조회된 게시글: {} 개", allPosts.size());
        log.info("=== 모든 게시글 조회 테스트 완료 ===");
    }

    @Test
    @DisplayName("ID로 게시글 조회 성공")
    void getPostById_Success() {
        log.info("=== ID로 게시글 조회 성공 테스트 시작 ===");

        // Given
        Long postId = 1L;
        given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));

        log.info("조회할 게시글 ID: {}", postId);

        // When
        Optional<Post> foundPost = postService.getPostById(postId);

        // Then
        assertThat(foundPost).isPresent();
        assertThat(foundPost.get().getTitle()).isEqualTo("테스트 게시글");

        verify(postRepository).findById(postId);

        log.info("게시글 조회 성공: {}", foundPost.get());
        log.info("=== ID로 게시글 조회 성공 테스트 완료 ===");
    }

    @Test
    @DisplayName("작성자별 게시글 조회")
    void getPostsByAuthor() {
        log.info("=== 작성자별 게시글 조회 테스트 시작 ===");

        // Given
        Long authorId = 1L;
        Post post1 = Post.of("작성자 게시글 1", "내용 1", mockAuthor);
        Post post2 = Post.of("작성자 게시글 2", "내용 2", mockAuthor);
        List<Post> authorPosts = List.of(post1, post2);

        given(userService.getUserById(authorId)).willReturn(Optional.of(mockAuthor));
        given(postRepository.findByAuthorOrderByCreatedAtDesc(mockAuthor)).willReturn(authorPosts);

        log.info("작성자 ID: {}, 예상 게시글 수: {}", authorId, authorPosts.size());

        // When
        List<Post> foundPosts = postService.getPostsByAuthor(authorId);

        // Then
        assertThat(foundPosts).hasSize(2);
        assertThat(foundPosts).extracting(Post::getTitle)
                .containsExactly("작성자 게시글 1", "작성자 게시글 2");

        verify(userService).getUserById(authorId);
        verify(postRepository).findByAuthorOrderByCreatedAtDesc(mockAuthor);

        log.info("작성자별 게시글 조회 완료: {} 개", foundPosts.size());
        log.info("=== 작성자별 게시글 조회 테스트 완료 ===");
    }

    @Test
    @DisplayName("제목으로 게시글 검색")
    void searchPostsByTitle() {
        log.info("=== 제목으로 게시글 검색 테스트 시작 ===");

        // Given
        String keyword = "Spring";
        Post post1 = Post.of("Spring Boot 가이드", "내용 1", mockAuthor);
        Post post2 = Post.of("Spring Security 튜토리얼", "내용 2", mockAuthor);
        List<Post> searchResults = List.of(post1, post2);

        given(postRepository.findByTitleContaining(keyword)).willReturn(searchResults);

        log.info("검색 키워드: {}, 예상 결과: {} 개", keyword, searchResults.size());

        // When
        List<Post> foundPosts = postService.searchPostsByTitle(keyword);

        // Then
        assertThat(foundPosts).hasSize(2);
        assertThat(foundPosts).extracting(Post::getTitle)
                .containsExactly("Spring Boot 가이드", "Spring Security 튜토리얼");

        verify(postRepository).findByTitleContaining(keyword);

        log.info("검색 결과: {} 개", foundPosts.size());
        foundPosts.forEach(post -> log.info("  - {}", post.getTitle()));
        log.info("=== 제목으로 게시글 검색 테스트 완료 ===");
    }

    @Test
    @DisplayName("게시글 업데이트 성공")
    void updatePost_Success() {
        log.info("=== 게시글 업데이트 성공 테스트 시작 ===");

        // Given
        Long postId = 1L;
        Long authorId = 1L;
        String newTitle = "업데이트된 제목";
        String newContent = "업데이트된 내용";

        given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));
        given(userService.getUserById(authorId)).willReturn(Optional.of(mockAuthor));
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> invocation.getArgument(0));

        log.info("업데이트 정보: postId={}, newTitle={}", postId, newTitle);

        // When
        Post updatedPost = postService.updatePost(postId, newTitle, newContent, authorId);

        // Then
        assertThat(updatedPost.getTitle()).isEqualTo(newTitle);
        assertThat(updatedPost.getContent()).isEqualTo(newContent);

        verify(postRepository).findById(postId);
        verify(userService).getUserById(authorId);
        verify(postRepository).save(mockPost);

        log.info("게시글 업데이트 완료: {}", updatedPost);
        log.info("=== 게시글 업데이트 성공 테스트 완료 ===");
    }

    @Test
    @DisplayName("권한 없는 사용자의 게시글 업데이트 실패")
    void updatePost_UnauthorizedUser() {
        log.info("=== 권한 없는 사용자 게시글 업데이트 실패 테스트 시작 ===");

        // Given
        Long postId = 1L;
        Long unauthorizedUserId = 2L;
        String newTitle = "업데이트 시도";
        String newContent = "업데이트 내용";

        User unauthorizedUser = User.of("unauthorized", "unauth@example.com");

        given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));
        given(userService.getUserById(unauthorizedUserId)).willReturn(Optional.of(unauthorizedUser));

        log.info("권한 없는 사용자 ID: {}", unauthorizedUserId);

        // When & Then
        assertThatThrownBy(() -> postService.updatePost(postId, newTitle, newContent, unauthorizedUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("게시글을 수정할 권한이 없습니다");

        verify(postRepository).findById(postId);
        verify(userService).getUserById(unauthorizedUserId);
        verify(postRepository, never()).save(any(Post.class));

        log.info("=== 권한 없는 사용자 게시글 업데이트 실패 테스트 완료 ===");
    }

    @Test
    @DisplayName("게시글 삭제 성공")
    void deletePost_Success() {
        log.info("=== 게시글 삭제 성공 테스트 시작 ===");

        // Given
        Long postId = 1L;
        Long authorId = 1L;

        given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));
        given(userService.getUserById(authorId)).willReturn(Optional.of(mockAuthor));

        log.info("삭제할 게시글 ID: {}, 작성자 ID: {}", postId, authorId);

        // When
        postService.deletePost(postId, authorId);

        // Then
        verify(postRepository).findById(postId);
        verify(userService).getUserById(authorId);
        verify(postRepository).deleteById(postId);

        log.info("게시글 삭제 완료");
        log.info("=== 게시글 삭제 성공 테스트 완료 ===");
    }

    @Test
    @DisplayName("존재하지 않는 게시글 삭제 실패")
    void deletePost_PostNotFound() {
        log.info("=== 존재하지 않는 게시글 삭제 실패 테스트 시작 ===");

        // Given
        Long nonExistentPostId = 999L;
        Long authorId = 1L;

        given(postRepository.findById(nonExistentPostId)).willReturn(Optional.empty());

        log.info("존재하지 않는 게시글 ID: {}", nonExistentPostId);

        // When & Then
        assertThatThrownBy(() -> postService.deletePost(nonExistentPostId, authorId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("게시글을 찾을 수 없습니다");

        verify(postRepository).findById(nonExistentPostId);
        verify(postRepository, never()).deleteById(anyLong());

        log.info("=== 존재하지 않는 게시글 삭제 실패 테스트 완료 ===");
    }

}
