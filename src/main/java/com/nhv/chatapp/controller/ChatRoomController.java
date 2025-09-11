package com.nhv.chatapp.controller;

import com.nhv.chatapp.dto.request.AddMemberRequest;
import com.nhv.chatapp.dto.request.CreateChatRoomRequest;
import com.nhv.chatapp.dto.request.SendMessageRequest;
import com.nhv.chatapp.dto.response.APIResponse;
import com.nhv.chatapp.dto.response.APIResponseMessage;
import com.nhv.chatapp.service.ChatRoomService;
import com.nhv.chatapp.service.MessageService;
import com.nhv.chatapp.utils.FilterUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatrooms")
public class ChatRoomController {
    @Autowired
    private ChatRoomService chatRoomService;
    @Autowired
    private MessageService messageService;

    @PostMapping("/create-chatroom")
    ResponseEntity<?> createChatroom(@RequestBody CreateChatRoomRequest createChatRoomRequest){
        APIResponse apiResponse = APIResponse.builder()
                .message(APIResponseMessage.SUCCESSFULLY_CREATED.name())
                .result(this.chatRoomService.createChatRoom(createChatRoomRequest))
                .status(HttpStatus.CREATED.value())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @GetMapping
    ResponseEntity<?> getChatrooms(){
        APIResponse apiResponse = APIResponse.builder()
                .message(APIResponseMessage.SUCCESSFULLY_RETRIEVED.name())
                .result(this.chatRoomService.getChatRooms())
                .status(HttpStatus.OK.value())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping(value = "/{chatRoomId}/send-message", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<?> sendMessage(@ModelAttribute SendMessageRequest sendMessageRequest, @PathVariable String chatRoomId){
        APIResponse apiResponse = APIResponse.builder()
                .message(APIResponseMessage.SUCCESSFULLY_CREATED.name())
                .result(this.messageService.sendMessage(sendMessageRequest, chatRoomId))
                .status(HttpStatus.CREATED.value())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @GetMapping("/{chatRoomId}/messages")
    ResponseEntity<?> getMessage(@PathVariable String chatRoomId,
                                 @RequestParam(defaultValue = FilterUtils.PAGE) int page,
                                 @RequestParam(defaultValue = FilterUtils.PAGE_SIZE) int size){
        APIResponse apiResponse = APIResponse.builder()
                .message(APIResponseMessage.SUCCESSFULLY_RETRIEVED.name())
                .result(this.messageService.getMessages(chatRoomId, page, size))
                .status(HttpStatus.OK.value())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/{chatRoomId}/add-member")
    ResponseEntity<?> addMember(@PathVariable String chatRoomId, @RequestBody AddMemberRequest addMemberRequest) {
        APIResponse apiResponse = APIResponse.builder()
                .message(APIResponseMessage.SUCCESSFULLY_CREATED.name())
                .result(this.chatRoomService.addMemberToChatRoom(chatRoomId, addMemberRequest))
                .status(HttpStatus.CREATED.value())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @GetMapping("/{chatRoomId}/members")
    ResponseEntity<?> getMembers(@PathVariable String chatRoomId){
        APIResponse apiResponse = APIResponse.builder()
                .message(APIResponseMessage.SUCCESSFULLY_RETRIEVED.name())
                .result(this.chatRoomService.getChatRoomMembers(chatRoomId))
                .status(HttpStatus.OK.value())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PutMapping("/{chatRoomId}/mark-read")
    ResponseEntity<?> markRead(@PathVariable String chatRoomId)
    {
        APIResponse apiResponse = APIResponse.builder()
                .message(APIResponseMessage.SUCCESSFULLY_UPDATED.name())
                .result(this.messageService.markReadMessages(chatRoomId))
                .status(HttpStatus.OK.value())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}
