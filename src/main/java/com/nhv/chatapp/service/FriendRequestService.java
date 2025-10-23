package com.nhv.chatapp.service;

import com.nhv.chatapp.dto.response.FriendRequestResponse;
import com.nhv.chatapp.entity.enums.FriendRequestStatus;

import java.util.List;

public interface FriendRequestService {
    FriendRequestResponse sendFriendRequest(String userId);
    void acceptFriendRequest(String userId, FriendRequestStatus status);
    List<FriendRequestResponse> getRequesters();
    List<FriendRequestResponse> getRecipients();
    void deleteFriendRequest(String requesterId);
}
