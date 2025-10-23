package com.nhv.chatapp.repository;

import com.nhv.chatapp.entity.Friendrequest;
import com.nhv.chatapp.entity.User;
import com.nhv.chatapp.entity.enums.FriendRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRequestRepository extends JpaRepository<Friendrequest, String> {
    Optional<Friendrequest> findByRequesterIdAndRecipientIdAndStatus(String requesterId, String recipientId, FriendRequestStatus status);
    Optional<Friendrequest> findByRequesterIdAndRecipientId(String requesterId, String recipientId);
    List<Friendrequest> findByRequesterIdAndStatus(String requesterId, FriendRequestStatus status);
    List<Friendrequest> findByRecipientIdAndStatus(String recipientId, FriendRequestStatus status);
}
