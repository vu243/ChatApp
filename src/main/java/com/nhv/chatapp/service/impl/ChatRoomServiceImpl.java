package com.nhv.chatapp.service.impl;

import com.nhv.chatapp.dto.ChatRoomMemberDTO;
import com.nhv.chatapp.dto.UserProfileDTO;
import com.nhv.chatapp.dto.request.AddMemberRequest;
import com.nhv.chatapp.dto.request.CreateChatRoomRequest;
import com.nhv.chatapp.dto.response.ChatRoomResponse;
import com.nhv.chatapp.dto.response.MessageResponse;
import com.nhv.chatapp.entity.Chatroom;
import com.nhv.chatapp.entity.Contact;
import com.nhv.chatapp.entity.User;
import com.nhv.chatapp.entity.Userchatroom;
import com.nhv.chatapp.entity.enums.RoomRole;
import com.nhv.chatapp.entity.enums.RoomType;
import com.nhv.chatapp.exception.BadRequestException;
import com.nhv.chatapp.exception.ResourceNotFoundException;
import com.nhv.chatapp.repository.*;
import com.nhv.chatapp.service.ChatRoomService;
import com.nhv.chatapp.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatRoomServiceImpl implements ChatRoomService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    @Autowired
    private UserChatRoomRepository userChatRoomRepository;
    @Autowired
    private ContactRepository contactRepository;


    @Override
    public ChatRoomResponse createChatRoom(CreateChatRoomRequest createChatRoomRequest) {
        Authentication authentication = SecurityUtils.getAuthentication();
        User currentUser = this.userRepository.findByUsername(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<String> memberIds = createChatRoomRequest.getMemberIds();
        if (!memberIds.contains(currentUser.getId())) {
            memberIds.add(currentUser.getId());
        }

        List<User> members = this.userRepository.findAllById(memberIds);
        if (members.size() != memberIds.size()) {
            throw new ResourceNotFoundException("Some user do not exist");
        }

        boolean isPrivate = memberIds.size() == 2;
        RoomType roomType = isPrivate ? RoomType.PRIVATE : RoomType.PUBLIC;
        User recipient = null;

         if (isPrivate) {
            recipient = this.userRepository.findById(memberIds.stream().filter(memberId -> !memberId.equals(currentUser.getId())).findFirst()
                            .orElseThrow(() -> new BadRequestException("Recipient not found")))
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            if (this.chatRoomRepository.existsPrivateChatroomByUserIds(currentUser.getId(), recipient.getId())) {
                throw new BadRequestException("Private chatroom already exists");
            }
        }else if (!isPrivate) {
            if (createChatRoomRequest.getGroupName() == null || createChatRoomRequest.getGroupName().trim().isEmpty()) {
                throw new BadRequestException("Group name cannot be empty");
            }
        }


        Chatroom chatRoom = Chatroom.builder()
                .type(roomType)
                .groupName(!isPrivate ? createChatRoomRequest.getGroupName() : recipient.getName())
                .groupAvatar(!isPrivate ? "https://res.cloudinary.com/dcrsia5sh/image/upload/v1758529252/group-profile-avatar-icon-default-social-media-forum-profile-photo-vector_fwzlht.jpg" : recipient.getAvatar())
                .createdBy(currentUser)
                .build();
        chatRoom = this.chatRoomRepository.save(chatRoom);
        List<Userchatroom> userchatrooms = new ArrayList<>();
        for (User member : members) {
            Userchatroom userchatroom = Userchatroom.builder()
                    .user(member)
                    .chatRoom(chatRoom)
                    .role((isPrivate | member.getId().equals(currentUser.getId())) ? RoomRole.ADMIN : RoomRole.MEMBER)
                    .build();
            if (!isPrivate && !member.getId().equals(currentUser.getId())) {
                userchatroom.setRole(RoomRole.MEMBER);
            }
            userchatrooms.add(userchatroom);
        }
        this.userChatRoomRepository.saveAll(userchatrooms);
        List<ChatRoomMemberDTO> chatRoomMembers = userchatrooms.stream()
                .map(u -> ChatRoomMemberDTO.builder()
                        .userId(u.getUser().getId())
                        .username(u.getUser().getUsername())
                        .name(u.getUser().getName())
                        .roomRole(u.getRole().name())
                        .build()).collect(Collectors.toList());
        return ChatRoomResponse.builder()
                .chatRoomId(chatRoom.getId())
                .chatRoomName(chatRoom.getGroupName())
                .chatRoomAvatar(chatRoom.getGroupAvatar())
                .createdBy(chatRoom.getCreatedBy().getName())
                .createdAt(chatRoom.getCreateAt())
                .members(chatRoomMembers)
                .build();
    }

    @Override
    public List<ChatRoomResponse> getChatRooms() {
        Authentication authentication = SecurityUtils.getAuthentication();
        User currentUser = this.userRepository.findByUsername(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Userchatroom> userchatrooms = this.userChatRoomRepository.findByUserIdOrderByJoinedAtDesc(currentUser.getId());
        List<String> privateRoomIds = userchatrooms.stream()
                .filter(ucr -> ucr.getChatRoom().getType()==RoomType.PRIVATE)
                .map(ucr -> ucr.getChatRoom().getId()).toList();
        Map<String, User> otherMembersMap = new HashMap<>();
        List<String> otherMemberId = new ArrayList<>();
        if(!privateRoomIds.isEmpty()) {
            List<Userchatroom> privateMembers= this.userChatRoomRepository.findByChatRoomIds(privateRoomIds);
            for(Userchatroom userchatroom : privateMembers) {
                if(!userchatroom.getUser().getId().equals(currentUser.getId())) {
                    otherMembersMap.put(userchatroom.getChatRoom().getId(), userchatroom.getUser());
                    otherMemberId.add(userchatroom.getUser().getId());
                }
            }
        }

        Map<String, Contact> contactMap = new HashMap<>();
        if(!otherMemberId.isEmpty()) {
            List<Contact> contacts = this.contactRepository.findByUserIdAndContactUserIds(currentUser.getId(),otherMemberId);
            for(Contact contact : contacts) {
                contactMap.put(contact.getContactUser().getId(), contact);
            }
        }
        return userchatrooms.stream()
                .map(userchatroom -> {
                    Chatroom chatroom = userchatroom.getChatRoom();
                    int memberCount = this.userChatRoomRepository.countByChatRoomId(chatroom.getId());

                    String chatRoomName;
                    String chatRoomAvatar;
                    String memberId = null;
                    UserProfileDTO userProfileDTO = null;
                    if(otherMemberId.contains(chatroom.getId())) {
                        memberId = otherMembersMap.get(chatroom.getId()).getId();
                    }
                    if(chatroom.getType()==RoomType.PRIVATE) {
                        User otherMember = otherMembersMap.get(chatroom.getId());
                        if(otherMember != null) {
                            memberId = otherMember.getId();
                            User userz = this.userRepository.findById(memberId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
                            userProfileDTO = UserProfileDTO.builder()
                                    .id(userz.getId())
                                    .lastSeen(userz.getLastSeen())
                                    .isOnline(userz.getOnline())
                                    .build();
                            chatRoomAvatar = otherMember.getAvatar();
                            Contact contact = contactMap.get(otherMember.getId());
                            if(contact != null && contact.getAlias() != null) {
                                chatRoomName = contact.getAlias();
                            }
                            else  {
                                chatRoomName = otherMember.getName();
                            }
                        }
                        else  {
                            chatRoomName = chatroom.getGroupName();
                            chatRoomAvatar = chatroom.getGroupAvatar();
                        }
                    }
                    else {
                        chatRoomName = chatroom.getGroupName();
                        chatRoomAvatar = chatroom.getGroupAvatar();
                    }
                    MessageResponse lastMessage = null;
                    if (chatroom.getLastMessage() != null) {
                        lastMessage = MessageResponse.builder()
                                .messageId(chatroom.getLastMessage().getId())
                                .content(chatroom.getLastMessage().getContent())
                                .senderName(chatroom.getLastMessage().getSender().getName())
                                .sentAt(chatroom.getLastMessage().getSentAt())
                                .messageType(chatroom.getLastMessage().getType().name())
                                .isLastRead((userchatroom.getLastReadMessage() != null && chatroom.getLastMessage().getId().equals(userchatroom.getLastReadMessage().getId()))? true:false)
                                .build();
                    }
                    return ChatRoomResponse.builder()
                            .chatRoomId(chatroom.getId())
                            .chatRoomName(chatRoomName)
                            .chatRoomAvatar(chatRoomAvatar)
                            .lastMessage(lastMessage)
                            .roomType(chatroom.getType())
                            .updatedAt(chatroom.getUpdateAt())
                            .memberCount(memberCount)
                            .member(userProfileDTO)
                            .build();
                }).collect(Collectors.toList());
    }

    @Override
    public List<ChatRoomResponse> getChatRooms(String type, String keyword) {
        Authentication authentication = SecurityUtils.getAuthentication();
        User currentUser = this.userRepository.findByUsername(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if(type == null && (type.equals(RoomType.PUBLIC.name()) || type.equals(RoomType.PRIVATE.name()))) {
            throw new BadRequestException("RoomType not found");
        }
        List<Userchatroom> userchatrooms = this.userChatRoomRepository.findByUserIdAndRoomType(currentUser.getId(), RoomType.PUBLIC, keyword);
        return userchatrooms.stream().map(
                userchatroom ->{
                    Chatroom chatroom = userchatroom.getChatRoom();
                    int memberCount = this.userChatRoomRepository.countByChatRoomId(chatroom.getId());
                    return ChatRoomResponse.builder()
                            .chatRoomId(chatroom.getId())
                            .chatRoomName(chatroom.getGroupName())
                            .chatRoomAvatar(chatroom.getGroupAvatar())
                            .roomType(RoomType.PUBLIC)
                            .updatedAt(chatroom.getUpdateAt())
                            .memberCount(memberCount)
                            .lastMessage(chatroom.getLastMessage() != null ? MessageResponse.builder()
                                    .messageId(chatroom.getLastMessage().getId())
                                    .content(chatroom.getLastMessage().getContent())
                                    .senderName(chatroom.getLastMessage().getSender().getName())
                                    .sentAt(chatroom.getLastMessage().getSentAt())
                                    .messageType(chatroom.getLastMessage().getType().name())
                                    .isLastRead(chatroom.getLastMessage() != null && userchatroom.getLastReadMessage().getId().equals(chatroom.getLastMessage().getId()) ? true : false)
                                    .build() : null)
                            .build();
                }
        ).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ChatRoomMemberDTO> addMemberToChatRoom(String chatRoomId, AddMemberRequest addMemberRequest) {
        Authentication authentication = SecurityUtils.getAuthentication();
        User currentUser = this.userRepository.findByUsername(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Chatroom chatroom = this.chatRoomRepository.findById(chatRoomId).orElseThrow(() -> new ResourceNotFoundException("Chat room not found"));
        Userchatroom userchatroom = this.userChatRoomRepository.findByUserIdAndChatRoomId(currentUser.getId(), chatRoomId);
        if (userchatroom == null) {
            throw new AccessDeniedException("You are not a member of the chat room");
        }
        if (chatroom.getType() != RoomType.PUBLIC) {
            throw new BadRequestException("Chat room type is not public");
        }
        //Get all member id (for checking member is null in database)
        List<User> member = this.userRepository.findAllById(addMemberRequest.getMemberIds());
        Map<String, User> memberIds = member.stream().collect(Collectors.toMap(User::getId, user -> user));

        Set<String> existingMemberIds = this.userChatRoomRepository.findUserIdsByChatRoomId(chatRoomId, addMemberRequest.getMemberIds());
        Set<String> contacts = this.contactRepository.findAcceptedFriendIds(currentUser.getId(), addMemberRequest.getMemberIds());

        List<ChatRoomMemberDTO> addedMembers = new ArrayList<>();
        List<Userchatroom> userchatrooms = new ArrayList<>();
        for (String id : addMemberRequest.getMemberIds()) {
            if(!contacts.contains(id)) {
                throw new BadRequestException("You must be friends with this user to add them to the group");
            }

            if (memberIds.get(id) == null) {
                throw new BadRequestException("User id not found");
            }

            if (existingMemberIds.contains(id)) {
                throw new BadRequestException("User id already a member of chat room");
            }
            userchatrooms.add(Userchatroom.builder()
                            .user(memberIds.get(id))
                            .chatRoom(chatroom)
                            .role(RoomRole.MEMBER)
                    .build());
            addedMembers.add(ChatRoomMemberDTO.builder()
                            .userId(id)
                            .username(memberIds.get(id).getUsername())
                            .name(memberIds.get(id).getName())
                            .roomRole(RoomRole.MEMBER.name())
                    .build());

        }
        if(!userchatrooms.isEmpty()) {
            this.userChatRoomRepository.saveAll(userchatrooms);
        }
        return addedMembers;
    }

    @Override
    public List<ChatRoomMemberDTO> getChatRoomMembers(String chatRoomId) {
        if(this.userChatRoomRepository.findByChatRoomId(chatRoomId) == null) {
            throw new ResourceNotFoundException("Chat room not found");
        }
        List<Userchatroom> userchatrooms = this.userChatRoomRepository.findByChatRoomId(chatRoomId);
        return userchatrooms.stream().map(ucr -> ChatRoomMemberDTO.builder()
                .userId(ucr.getUser().getId())
                .username(ucr.getUser().getUsername())
                .name(ucr.getUser().getName())
                .roomRole(ucr.getRole().name())
                .build()).collect(Collectors.toList());
    }

    @Override
    public ChatRoomResponse getChatRoomId(String username1, String username2) {
        return ChatRoomResponse.builder()
                .chatRoomId(this.chatRoomRepository.findChatroomIdByUserPair(username1, username2).orElseThrow(() -> new ResourceNotFoundException("Chat room not found")))
                .build();
    }
}
