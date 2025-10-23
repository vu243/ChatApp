package com.nhv.chatapp.controller;

import com.nhv.chatapp.dto.response.APIResponse;
import com.nhv.chatapp.dto.response.APIResponseMessage;
import com.nhv.chatapp.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {
    @Autowired
    private ContactService contactService;

    @GetMapping
    ResponseEntity<?> getContacts(@RequestParam(value = "keyword", required = false) String keyword){
        APIResponse apiResponse = APIResponse.builder()
                .message(APIResponseMessage.SUCCESSFULLY_RETRIEVED.name())
                .result(contactService.getContacts(keyword))
                .status(HttpStatus.OK.value())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}
