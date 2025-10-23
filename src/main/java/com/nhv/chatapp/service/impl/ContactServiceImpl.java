package com.nhv.chatapp.service.impl;

import com.nhv.chatapp.dto.ContactDTO;
import com.nhv.chatapp.dto.response.ContactsResponse;
import com.nhv.chatapp.entity.Contact;
import com.nhv.chatapp.entity.User;
import com.nhv.chatapp.exception.ResourceNotFoundException;
import com.nhv.chatapp.repository.ContactRepository;
import com.nhv.chatapp.repository.UserRepository;
import com.nhv.chatapp.service.ContactService;
import com.nhv.chatapp.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContactServiceImpl implements ContactService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ContactRepository contactRepository;
    @Override
    public ContactsResponse getContacts(String keyword) {
        Authentication authentication = SecurityUtils.getAuthentication();
        User currentUser = this.userRepository.findByUsername(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Contact> contacts = this.contactRepository.findByUserIdAndContactUserNameContainingIgnoreCase(currentUser.getId(), keyword);
        List<ContactDTO> contactInfos = contacts.stream()
                .map(contact -> {
                    User contactUser = contact.getUser().getId().equals(currentUser.getId())
                            ? contact.getContactUser() : contact.getUser();
                    return ContactDTO.builder()
                            .contactId(contact.getId())
                            .userId(contactUser.getId())
                            .username(contactUser.getUsername())
                            .name(contactUser.getName())
                            .avatar(contactUser.getAvatar())
                            .isOnline(contactUser.getOnline())
                            .lastSeen(contactUser.getLastSeen())
                            .build();
                }).collect(Collectors.toList());
        return ContactsResponse.builder()
                .contacts(contactInfos)
                .totalContacts(contacts.size())
                .build();
    }
}
