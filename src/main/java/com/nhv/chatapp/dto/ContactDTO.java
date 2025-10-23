package com.nhv.chatapp.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
public class ContactDTO {
    private String contactId;
    private String userId;
    private String username;
    private String name;
    private String avatar;
    private boolean isOnline;
    private Instant lastSeen;
}
