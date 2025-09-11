package com.nhv.chatapp.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfileDTO {
    String name;
    String username;
    String email;
    String bio;
    String avatar;
    boolean isContact;
}
