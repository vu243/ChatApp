package com.nhv.chatapp.service.impl;

import com.nhv.chatapp.dto.UserProfileDTO;
import com.nhv.chatapp.dto.UserSummaryDTO;
import com.nhv.chatapp.dto.response.PageResponse;
import com.nhv.chatapp.entity.Contact;
import com.nhv.chatapp.entity.User;
import com.nhv.chatapp.exception.BadRequestException;
import com.nhv.chatapp.exception.ResourceNotFoundException;
import com.nhv.chatapp.repository.ContactRepository;
import com.nhv.chatapp.repository.UserRepository;
import com.nhv.chatapp.service.UserService;
import com.nhv.chatapp.utils.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {
    @Autowired
    UserRepository userRepository;
    @Autowired
    ContactRepository contactRepository;

    @Override
    public PageResponse<UserSummaryDTO> getUsers(String keyword, int page, int size) {
        String username = SecurityUtils.getAuthentication().getName();
        User currentUser = this.userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage;

        if(keyword != null && !keyword.trim().isEmpty()) {
            userPage = this.userRepository.findByNameWithContactPriority(keyword, currentUser.getId(), pageable);
        }
        else return null;

        List<String> userIds = userPage.getContent().stream().map(User::getId).toList();
        Map<String, Contact> contactMap = this.contactRepository.findByUserIdAndContactUserIds(currentUser.getId(), userIds)
                .stream().collect(Collectors.toMap(
                        contact -> contact.getContactUser().getId(),
                        contact -> contact
                ));
        List<UserSummaryDTO> userSummaryDTOList = userPage.getContent().stream()
                .map(user -> UserSummaryDTO.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .username(user.getUsername())
                        .avatar(user.getAvatar())
                        .isContact(contactMap.get(user.getId()) != null ? true : false)
                        .isOnline(contactMap.get(user.getId()) != null ? user.getOnline() : null)
                        .build()).collect(Collectors.toList());
        return PageResponse.<UserSummaryDTO>builder()
                .data(userSummaryDTOList)
                .page(page)
                .totalPages(userPage.getTotalPages())
                .totalElements(userPage.getTotalElements())
                .hasNext(userPage.hasNext())
                .hasPrevious(userPage.hasPrevious())
                .build();
    }

    public UserProfileDTO getUserProfile(String userId){
        Authentication authentication = SecurityUtils.getAuthentication();
        User currentUser = this.userRepository.findByUsername(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User user = this.userRepository.findById(userId).orElseThrow(() -> new BadRequestException("User not found"));

        return UserProfileDTO.builder()
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .bio(user.getBio())
                .isContact(this.contactRepository.findByUserIdAndContactUserId(currentUser.getId(), userId) != null ? true : false)
                .avatar(user.getAvatar()).build();
    }

    public UserProfileDTO getUserProfile(){
        Authentication authentication = SecurityUtils.getAuthentication();
//        System.out.println(authentication);
//        String name = authentication.getName();
//        System.out.println(name);
//        Jwt jwt = (Jwt) authentication.getPrincipal();
//        System.out.println(jwt.getClaims());
        User user = this.userRepository.findByUsername(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return UserProfileDTO.builder()
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .bio(user.getBio())
                .avatar(user.getAvatar()).build();
    }

    public UserProfileDTO updateUser(String userId, UserProfileDTO userProfileDTO){
        User user = this.userRepository.findById(userId).orElseThrow(() -> new BadRequestException("User not found"));
        user = User.builder()
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .bio(user.getBio())
                .avatar(user.getAvatar()).build();
        this.userRepository.save(user);
        return userProfileDTO;
    }

    @Override
    public void deleteUser(String userId) {
        this.userRepository.deleteById(userId);
    }
}
