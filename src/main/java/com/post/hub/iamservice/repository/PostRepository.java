package com.post.hub.iamservice.repository;

import com.post.hub.iamservice.model.entities.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Integer>, JpaSpecificationExecutor<Post> {

    boolean existsByTitle(String title);

    Optional<Post> findByIdAndDeletedFalse(Integer id);

}
