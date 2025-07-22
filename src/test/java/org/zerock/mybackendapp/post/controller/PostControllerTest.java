package org.zerock.mybackendapp.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.zerock.mybackendapp.post.domain.Post;
import org.zerock.mybackendapp.post.service.PostService;
import org.zerock.mybackendapp.user.domain.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;


@WebMvcTest(controllers = PostController.class,
            excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
            })
@Slf4j
@DisplayName("Post Controller 테스트")
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PostService postService;

    @Test
    @DisplayName("게시글 생성 API 성공")
    void createPost_Success() throws Exception {
        log.info("=== 게시글 생성 API 성공 테스트 시작 ===");

        // Given
        String title = "새 게시글";
        String content = "새 게시글 내용";
        Long authorId = 1L;

        Map<String, Object> request = Map.of(
                "title", title,
                "content", content,
                "authorId", authorId
        );

        User mockAuthor = User.of("testauthor", "author@example.com");
        ReflectionTestUtils.setField(mockAuthor, "id", authorId);

        Post mockPost = Post.of(title, content, mockAuthor);
        ReflectionTestUtils.setField(mockPost, "id", 1L);

        given(postService.createPost(title, content, authorId)).willReturn(mockPost);

        log.info("요청 데이터: {}", request);

        // When & Then
        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.author.username").value("testauthor"));

        verify(postService).createPost(title, content, authorId);

        log.info("=== 게시글 생성 API 성공 테스트 완료 ===");
    }

    @Test
    @DisplayName("게시글 생성 API - 필수 필드 누락")
    void createPost_MissingFields() throws Exception {
        log.info("=== 게시글 생성 API 필수 필드 누락 테스트 시작 ===");

        // Given
        Map<String, Object> request = Map.of(
                "title", "제목만 있음"
                // content와 authorId 누락
        );

        log.info("불완전한 요청 데이터: {}", request);

        // When & Then
        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verify(postService, never()).createPost(anyString(), anyString(), anyLong());

        log.info("=== 게시글 생성 API 필수 필드 누락 테스트 완료 ===");
    }

    @Test
    @DisplayName("게시글 생성 API - 존재하지 않는 작성자")
    void createPost_AuthorNotFound() throws Exception {
        log.info("=== 게시글 생성 API 존재하지 않는 작성자 테스트 시작 ===");

        // Given
        String title = "게시글 제목";
        String content = "게시글 내용";
        Long nonExistentAuthorId = 999L;

        Map<String, Object> request = Map.of(
                "title", title,
                "content", content,
                "authorId", nonExistentAuthorId
        );

        given(postService.createPost(title, content,nonExistentAuthorId))
                .willThrow(new IllegalArgumentException("작성자를 찾을 수 없습니다: " + nonExistentAuthorId));

        log.info("존재하지 않는 작성자 ID: {}", nonExistentAuthorId);

        // When & Then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("작성자를 찾을 수 없습니다: " + nonExistentAuthorId));

        verify(postService).createPost(title, content, nonExistentAuthorId);

        log.info("=== 게시글 생성 API 존재하지 않는 작성자 테스트 완료 ===");
    }

    @Test
    @DisplayName("모든 게시글 조회 API")
    void getAllPosts() throws Exception {
        log.info("=== 모든 게시글 조회 API 테스트 시작 ===");

        // Given
        User author = User.of("author", "author@example.com");
        ReflectionTestUtils.setField(author, "id", 1L);

        Post post1 = Post.of("게시글 1", "내용 1", author);
        Post post2 = Post.of("게시글 2", "내용 2", author);
        ReflectionTestUtils.setField(post1, "id", 1L);
        ReflectionTestUtils.setField(post2, "id", 2L);

        List<Post> mockPosts = List.of(post1, post2);

        given(postService.getAllPosts()).willReturn(mockPosts);

        log.info("Mock 게시글 데이터: {} 개", mockPosts.size());

        // When & Then
        mockMvc.perform(get("/api/posts"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("게시글 1"))
                .andExpect(jsonPath("$[1].title").value("게시글 2"));

        verify(postService).getAllPosts();

        log.info("=== 모든 게시글 조회 API 테스트 완료 ===");
    }

    @Test
    @DisplayName("ID로 게시글 조회 API 성공")
    void getPostById_Success() throws Exception {
        log.info("=== ID로 게시글 조회 API 성공 테스트 시작 ===");

        // Given
        Long postId = 1L;

        User author = User.of("author", "author@example.com");
        ReflectionTestUtils.setField(author, "id", 1L);

        Post mockPost = Post.of("테스트 게시글", "테스트 내용", author);
        ReflectionTestUtils.setField(mockPost, "id", postId);

        given(postService.getPostById(postId)).willReturn(Optional.of(mockPost));

        log.info("조회할 게시글 ID: {}", postId);

        // When & Then
        mockMvc.perform(get("/api/posts/{id}", postId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("테스트 게시글"))
                .andExpect(jsonPath("$.content").value("테스트 내용"))
                .andExpect(jsonPath("$.author.username").value("author"));

        verify(postService).getPostById(postId);

        log.info("=== ID로 게시글 조회 API 성공 테스트 완료 ===");
    }

    @Test
    @DisplayName("ID로 게시글 조회 API - 게시글 없음")
    void getPostById_NotFound() throws Exception {
        log.info("=== ID로 게시글 조회 API 게시글 없음 테스트 시작 ===");

        // Given
        Long nonExistentPostId = 999L;
        given(postService.getPostById(nonExistentPostId)).willReturn(Optional.empty());

        log.info("존재하지 않는 게시글 ID: {}", nonExistentPostId);

        // When & Then
        mockMvc.perform(get("/api/posts/{id}", nonExistentPostId))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(postService).getPostById(nonExistentPostId);

        log.info("=== ID로 게시글 조회 API 게시글 없음 테스트 완료 ===");
    }

    @Test
    @DisplayName("작성자별 게시글 조회 API")
    void getPostsByAuthor() throws Exception {
        log.info("=== 작성자별 게시글 조회 API 테스트 시작 ===");

        // Given
        Long authorId = 1L;

        User author = User.of("author", "author@example.com");
        ReflectionTestUtils.setField(author, "id", authorId);

        Post post1 = Post.of("작성자 게시글 1", "내용 1", author);
        Post post2 = Post.of("작성자 게시글 2", "내용 2", author);
        List<Post> authorPosts = List.of(post1, post2);

        given(postService.getPostsByAuthor(authorId)).willReturn(authorPosts);

        log.info("작성자 ID: {}, 예상 게시글 수: {}", authorId, authorPosts.size());

        // When & Then
        mockMvc.perform(get("/api/posts/author/{authorId}", authorId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("작성자 게시글 1"))
                .andExpect(jsonPath("$[1].title").value("작성자 게시글 2"));

        verify(postService).getPostsByAuthor(authorId);

        log.info("=== 작성자별 게시글 조회 API 테스트 완료 ===");
    }

    @Test
    @DisplayName("제목으로 게시글 검색 API")
    void searchPostsByTitle() throws Exception {
        log.info("=== 제목으로 게시글 검색 API 테스트 시작 ===");

        // Given
        String keyword = "Spring";

        User author = User.of("author", "author@example.com");
        Post post1 = Post.of("Spring Boot 가이드", "내용 1", author);
        Post post2 = Post.of("Spring Security 튜토리얼", "내용 2", author);
        List<Post> searchResults = List.of(post1, post2);

        given(postService.searchPostsByTitle(keyword)).willReturn(searchResults);

        log.info("검색 키워드: {}, 예상 결과: {} 개", keyword, searchResults.size());

        // When & Then
        mockMvc.perform(get("/api/posts/search")
                        .param("keyword", keyword))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Spring Boot 가이드"))
                .andExpect(jsonPath("$[1].title").value("Spring Security 튜토리얼"));

        verify(postService).searchPostsByTitle(keyword);

        log.info("=== 제목으로 게시글 검색 API 테스트 완료 ===");
    }

    @Test
    @DisplayName("게시글 업데이트 API 성공")
    void updatePost_Success() throws Exception {
        log.info("=== 게시글 업데이트 API 성공 테스트 시작 ===");

        // Given
        Long postId = 1L;
        Long userId = 1L;
        String newTitle = "업데이트된 제목";
        String newContent = "업데이트된 내용";

        Map<String, Object> request = Map.of(
                "title", newTitle,
                "content", newContent,
                "userId", userId
        );

        User author = User.of("author", "author@example.com");
        ReflectionTestUtils.setField(author, "id", userId);

        Post updatedPost = Post.of(newTitle, newContent, author);
        ReflectionTestUtils.setField(updatedPost, "id", postId);

        given(postService.updatePost(postId, newTitle, newContent, userId)).willReturn(updatedPost);

        log.info("업데이트 요청: postId={}, newTitle={}", postId, newTitle);

        // When & Then
        mockMvc.perform(put("/api/posts/{id}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(newTitle))
                .andExpect(jsonPath("$.content").value(newContent));

        verify(postService).updatePost(postId, newTitle, newContent, userId);

        log.info("=== 게시글 업데이트 API 성공 테스트 완료 ===");
    }

    @Test
    @DisplayName("게시글 삭제 API 성공")
    void deletePost_Success() throws Exception {
        log.info("=== 게시글 삭제 API 성공 테스트 시작 ===");

        // Given
        Long postId = 1L;
        Long userId = 1L;

        Map<String, Object> request = Map.of("userId", userId);

        // When & Then
        mockMvc.perform(delete("/api/posts/{id}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("게시글이 삭제되었습니다"));

        verify(postService).deletePost(postId, userId);

        log.info("=== 게시글 삭제 API 성공 테스트 완료 ===");
    }
}