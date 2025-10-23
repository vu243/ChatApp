package com.nhv.chatapp.service;

import com.nhv.chatapp.dto.MessageStatusDTO;
import com.nhv.chatapp.dto.request.SendMessageRequest;
import com.nhv.chatapp.dto.response.MessageResponse;
import com.nhv.chatapp.dto.response.PageResponse;
import com.nhv.chatapp.entity.enums.MessageStatus;

public interface MessageService {
    MessageResponse sendMessage(SendMessageRequest message, String chatRoomId);
    PageResponse<MessageResponse> getMessages(String chatRoomId, int page, int size);
    MessageStatusDTO markReadMessages(String chatRoomId);
}
