package com.nhv.chatapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileDTO {
    String id;
    String name;
    String username;
    String email;
    String bio;
    String avatar;
    Boolean isContact;
    Boolean isOnline;
    Instant lastSeen;
    Boolean isRequester;
    Boolean isRecipient;
    MultipartFile image;
}
