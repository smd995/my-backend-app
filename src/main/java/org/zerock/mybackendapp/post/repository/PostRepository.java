package org.zerock.mybackendapp.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zerock.mybackendapp.post.domain.Post;
import org.zerock.mybackendapp.user.domain.User;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByAuthor(User Author);

    List<Post> findByTitleContaining(String keyword);

    List<Post> findAllByOrderByCreatedAtDesc();

    List<Post> findByAuthorOrderByCreatedAtDesc(User Author);
}
