package com.nhv.chatapp.repository;

import com.nhv.chatapp.entity.Contact;
import com.nhv.chatapp.entity.enums.ContactStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ContactRepository extends JpaRepository<Contact, String> {
    @Query("SELECT CASE " +
            "WHEN c.user.id = :currentUserId THEN c.contactUser.id " +
            "ELSE c.user.id END " +
            "FROM Contact c WHERE " +
            "(c.user.id = :currentUserId OR c.contactUser.id = :currentUserId) AND " +
            "c.status = 'ACCEPTED' AND " +
            "(c.user.id IN :userIds OR c.contactUser.id IN :userIds)")
    Set<String> findAcceptedFriendIds(@Param("currentUserId") String currentUserId,
                                      @Param("userIds") List<String> userIds);

    @Query("SELECT c FROM Contact c " +
            "WHERE c.user.id = :userId " +
            "AND (:keyword IS NULL OR LOWER(c.contactUser.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Contact> findByUserIdAndContactUserNameContainingIgnoreCase(@Param("userId") String userId,
                                 @Param("keyword") String keyword);

    @Query("SELECT c.contactUser.username FROM Contact c " +
            "WHERE c.user.id = :userId AND c.status = 'ACTIVE'")
    List<String> findFriendUsernames(@Param("userId") String userId);

    Contact findByUserIdAndContactUserId(String userId, String contactUserId);

    @Query("SELECT c FROM Contact c WHERE c.user.id = :userId AND c.contactUser.id IN :contactUserIds")
    List<Contact> findByUserIdAndContactUserIds(
            @Param("userId") String userId,
            @Param("contactUserIds") List<String> contactUserIds);
}
