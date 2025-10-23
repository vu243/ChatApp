package com.nhv.chatapp.repository;

import com.nhv.chatapp.entity.Chatroom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<Chatroom, String> {
    @Query("""
        SELECT CASE WHEN COUNT(cr) > 0 THEN true ELSE false END
        FROM Chatroom cr
        JOIN Userchatroom ucr1 ON cr.id = ucr1.chatRoom.id
        JOIN Userchatroom ucr2 ON cr.id = ucr2.chatRoom.id
        WHERE ucr1.user.id = :userId1 
          AND ucr2.user.id = :userId2 
          AND cr.type = 'PRIVATE'
    """)
    boolean existsPrivateChatroomByUserIds(
            @Param("userId1") String userId1,
            @Param("userId2") String userId2
    );

    @Query("""
       SELECT c.id
          FROM Chatroom c
          JOIN Userchatroom uc1 ON c.id = uc1.chatRoom.id
          JOIN User u1 ON uc1.user.id = u1.id
          JOIN Userchatroom uc2 ON c.id = uc2.chatRoom.id
          JOIN User u2 ON uc2.user.id = u2.id
          WHERE u1.username = :username1
            AND u2.username = :username2
            AND c.type = 'PRIVATE'
    """)
    Optional<String> findChatroomIdByUserPair(@Param("username1") String username1, @Param("username2") String username2);

}