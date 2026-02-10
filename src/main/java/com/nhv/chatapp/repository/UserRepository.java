package com.nhv.chatapp.repository;

import com.nhv.chatapp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository  extends JpaRepository<User, String> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u " +
            "WHERE u.id != :currentUserId " +
            "AND LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY " +
            "(CASE WHEN EXISTS(SELECT 1 FROM Contact c WHERE c.contactUser = u AND c.user.id = :currentUserId) " +
            "      THEN 0 ELSE 1 END), " +
            "u.name ASC")
    Page<User> findByNameWithContactPriority(
            @Param("keyword") String keyword,
            @Param("currentUserId") String currentUserId,
            Pageable pageable);

    @Query("SELECT u.username FROM User u WHERE u.id IN :userIds")
    List<String> findUsernamesByIds(@Param("userIds") List<String> userIds);
}
