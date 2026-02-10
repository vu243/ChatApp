package com.nhv.chatapp.controller;

import com.nhv.chatapp.dto.UserProfileDTO;
import com.nhv.chatapp.dto.response.APIResponse;
import com.nhv.chatapp.dto.response.APIResponseMessage;
import com.nhv.chatapp.entity.enums.FriendRequestStatus;
import com.nhv.chatapp.service.FriendRequestService;
import com.nhv.chatapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private FriendRequestService friendRequestService;

    @GetMapping
    ResponseEntity<?> getUsers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size){
        APIResponse apiResponse = APIResponse.builder()
                .message(APIResponseMessage.SUCCESSFULLY_RETRIEVED.name())
                .result(userService.getUsers(keyword, page, size))
                .status(HttpStatus.OK.value()).build();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @GetMapping("/{userId}/profile")
    ResponseEntity<?> getUser(@PathVariable("userId") String userId){
        APIResponse apiResponse = APIResponse.builder()
                .message(APIResponseMessage.SUCCESSFULLY_RETRIEVED.name())
                .result(this.userService.getUserProfile(userId))
                .status(HttpStatus.OK.value()).build();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @GetMapping("/profile")
    ResponseEntity<?> getProfile(){
        APIResponse apiResponse = APIResponse.builder()
                .message(APIResponseMessage.SUCCESSFULLY_RETRIEVED.name())
                .result(this.userService.getUserProfile())
                .status(HttpStatus.OK.value())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PutMapping(value = "/update-profile",  consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<?> updateUser(@ModelAttribute UserProfileDTO userUpdateDTO){
        APIResponse apiResponse = APIResponse.builder()
                .message(APIResponseMessage.SUCCESSFULLY_UPDATED.name())
                .result(this.userService.updateUser(userUpdateDTO))
                .status(HttpStatus.OK.value()).build();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @DeleteMapping("/{userId}")
    ResponseEntity<?> deleteUser(@PathVariable("userId") String userId){
        APIResponse apiResponse = APIResponse.builder()
                .message(APIResponseMessage.SUCCESSFULLY_DELETED.name())
                .status(HttpStatus.NO_CONTENT.value()).build();
        return new ResponseEntity<>(apiResponse, HttpStatus.NO_CONTENT);
    }

    @PostMapping("/{userId}/request")
    ResponseEntity<?> requestFriend(@PathVariable("userId") String userId){
        APIResponse apiResponse = APIResponse.builder()
                .message(APIResponseMessage.SUCCESSFULLY_CREATED.name())
                .result(this.friendRequestService.sendFriendRequest(userId))
                .status(HttpStatus.CREATED.value())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @PutMapping("/{userId}/accept-request")
    ResponseEntity<?>  updateRequest(@PathVariable("userId") String userId){
        this.friendRequestService.acceptFriendRequest(userId, FriendRequestStatus.ACCEPTED);
        APIResponse apiResponse = APIResponse.builder()
                .message(APIResponseMessage.SUCCESSFULLY_UPDATED.name())
                .status(HttpStatus.CREATED.value())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @DeleteMapping("/{userId}/delete-request")
    ResponseEntity<?> deleteRequest(@PathVariable("userId") String userId){
        this.friendRequestService.deleteFriendRequest(userId);
        APIResponse apiResponse = APIResponse.builder()
                .message(APIResponseMessage.SUCCESSFULLY_DELETED.name())
                .status(HttpStatus.NO_CONTENT.value())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.NO_CONTENT);
    }
}
