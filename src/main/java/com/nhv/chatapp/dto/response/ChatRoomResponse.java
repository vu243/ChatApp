package com.nhv.chatapp.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nhv.chatapp.dto.ChatRoomMemberDTO;
import com.nhv.chatapp.dto.UserProfileDTO;
import com.nhv.chatapp.entity.enums.RoomType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRoomResponse {
    private String chatRoomId;
    private String chatRoomName;
    private String chatRoomAvatar;
    private RoomType roomType;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer memberCount;
    private MessageResponse lastMessage;
    private List<ChatRoomMemberDTO> members;
    private UserProfileDTO member;
//    private boolean isMuted;
//    private boolean isPinned;
}
