package com.nhv.chatapp.controller;

import com.cloudinary.api.ApiResponse;
import com.nhv.chatapp.dto.response.APIResponse;
import com.nhv.chatapp.dto.response.APIResponseMessage;
import com.nhv.chatapp.service.FriendRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/friend-request")
public class FriendRequestController {
    @Autowired
    private FriendRequestService friendRequestService;

    @GetMapping("/requesters")
    public ResponseEntity<?> getRequesters() {
        APIResponse apiResponse = APIResponse.builder()
                .message(APIResponseMessage.SUCCESSFULLY_RETRIEVED.name())
                .result(this.friendRequestService.getRequesters())
                .status(HttpStatus.OK.value())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @GetMapping("/recipients")
    public ResponseEntity<?> getRecipients() {
        APIResponse apiResponse = APIResponse.builder()
                .message(APIResponseMessage.SUCCESSFULLY_RETRIEVED.name())
                .result(this.friendRequestService.getRecipients())
                .status(HttpStatus.OK.value())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}
