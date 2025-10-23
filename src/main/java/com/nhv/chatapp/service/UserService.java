package com.nhv.chatapp.service;

import com.nhv.chatapp.dto.UserProfileDTO;
import com.nhv.chatapp.dto.response.PageResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

public interface UserService {
    PageResponse<UserProfileDTO> getUsers(String keyword, int page, int size);
    UserProfileDTO getUserProfile(String userId);
    UserProfileDTO getUserProfile();
    UserProfileDTO updateUser(String userId, UserProfileDTO user);
    void deleteUser(String userId);
    void setOnline();
    void setOffline();
}
