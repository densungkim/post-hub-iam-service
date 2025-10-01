package com.post.hub.iamservice.repository;

import com.post.hub.iamservice.model.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByIdAndDeletedFalse(Integer id);

    Optional<User> findUserByEmailAndDeletedFalse(String email);

    Optional<User> findUserByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

}
