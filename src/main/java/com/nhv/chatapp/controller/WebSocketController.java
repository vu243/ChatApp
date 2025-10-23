package com.nhv.chatapp.controller;

import com.nhv.chatapp.dto.UserProfileDTO;
import com.nhv.chatapp.dto.request.SendMessageRequest;
import com.nhv.chatapp.dto.response.MessageResponse;
import com.nhv.chatapp.service.ChatRoomService;
import com.nhv.chatapp.service.MessageService;
import com.nhv.chatapp.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
@Slf4j
public class WebSocketController {
    @Autowired
    private UserService userService;
    @Autowired
    private MessageService messageService;

    @MessageMapping("/test")
    public void testConnection() {
        log.info("aaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    }

    @MessageMapping("/user/connect")
    public void connect() {
        this.userService.setOnline();
    }

    @MessageMapping("/user/disconnect")
    public void disconnect() {
        this.userService.setOffline();
    }

    @MessageMapping("/chatrooms/{chatRoomId}/send-message")
    @SendTo("/topic/chatrooms/{chatRoomId}/new-message")
    public MessageResponse  sendMessage(@DestinationVariable String chatRoomId, @RequestBody SendMessageRequest sendMessageRequest) {
        return this.messageService.sendMessage(sendMessageRequest, chatRoomId);
    }
}
