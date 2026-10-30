package com.hiresphere.user.repository;

import com.hiresphere.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("select u.tokenVersion from User u where u.id = :id")
    Optional<Integer> findTokenVersionById(@Param("id") Long id);

    @Query("select u.role, count(u) from User u group by u.role")
    List<Object[]> countGroupedByRole();

    long countByEnabledFalse();

    long countByEmailVerifiedFalse();

    long countByCreatedAtAfter(Instant since);


}