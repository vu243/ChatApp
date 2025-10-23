package com.nhv.chatapp.service;

import com.nhv.chatapp.dto.response.ContactsResponse;

public interface ContactService {
    ContactsResponse getContacts(String keyword);
}
