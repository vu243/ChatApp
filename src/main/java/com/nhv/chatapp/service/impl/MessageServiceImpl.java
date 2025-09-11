package com.nhv.chatapp.service.impl;

import com.cloudinary.Cloudinary;
import com.nhv.chatapp.dto.MemberReadDTO;
import com.nhv.chatapp.dto.MessageStatusDTO;
import com.nhv.chatapp.dto.request.CreateChatRoomRequest;
import com.nhv.chatapp.dto.request.SendMessageRequest;
import com.nhv.chatapp.dto.response.MessageResponse;
import com.nhv.chatapp.dto.response.PageResponse;
import com.nhv.chatapp.entity.Chatroom;
import com.nhv.chatapp.entity.Message;
import com.nhv.chatapp.entity.User;
import com.nhv.chatapp.entity.Userchatroom;
import com.nhv.chatapp.entity.enums.MessageStatus;
import com.nhv.chatapp.entity.enums.MessageType;
import com.nhv.chatapp.exception.BadRequestException;
import com.nhv.chatapp.exception.ResourceNotFoundException;
import com.nhv.chatapp.repository.ChatRoomRepository;
import com.nhv.chatapp.repository.MessageRepository;
import com.nhv.chatapp.repository.UserChatRoomRepository;
import com.nhv.chatapp.repository.UserRepository;
import com.nhv.chatapp.service.ChatRoomService;
import com.nhv.chatapp.service.MessageService;
import com.nhv.chatapp.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService {
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    @Autowired
    private UserChatRoomRepository userChatRoomRepository;
    @Autowired
    private CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public MessageResponse sendMessage(SendMessageRequest messageRequest, String chatRoomId) {
        Authentication authentication = SecurityUtils.getAuthentication();
        User currentUser = this.userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Chatroom chatroom = this.chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chatroom not found"));
        
        boolean isMember = this.userChatRoomRepository.existsByUserIdAndChatRoomId(currentUser.getId(), chatRoomId);
        if (!isMember) {
            throw new AccessDeniedException("You are not a member of this chat room");
        }
        
        Message replyToMessage = null;
        if (messageRequest.getReplyToMessageId() != null) {
            replyToMessage = this.messageRepository.findById(messageRequest.getReplyToMessageId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reply message not found"));
            if (!replyToMessage.getChatRoom().getId().equals(chatroom.getId())) {
                throw new BadRequestException("Reply message must be from the same chat room");
            }
        }
        String content = messageRequest.getContent();
        if(messageRequest.getMessageType().equals(MessageType.IMAGE.name()) && messageRequest.getImage() != null) {
            try{
                content = cloudinaryService.uploadImage(messageRequest.getImage());
            }catch(Exception e){
                throw new BadRequestException(e.getMessage());
            }
        }
        Message message = Message.builder()
                .content(content)
                .sender(currentUser)
                .chatRoom(chatroom)
                .replyTo(replyToMessage)
                .type(MessageType.valueOf(messageRequest.getMessageType()))
                .messageStatus(MessageStatus.SENT)
                .build();

        message = this.messageRepository.save(message);
        chatroom.setLastMessage(message);
        this.chatRoomRepository.save(chatroom);

        // Update lastReadMessage cho người gửi (tự động đã đọc tin nhắn của mình)
        this.updateLastReadMessage(currentUser.getId(), chatRoomId, message.getId());
        return MessageResponse.builder()
                .messageId(message.getId())
                .content(message.getContent())
                .senderId(message.getSender().getId())
                .senderUsername(message.getSender().getUsername())
                .senderName(message.getSender().getName())
                .sentAt(message.getSentAt())
                .replyTo(message.getReplyTo() != null ? MessageResponse.ReplyMessage.builder()
                        .messageId(message.getReplyTo().getId())
                        .content(message.getReplyTo().getContent())
                        .senderId(message.getReplyTo().getSender().getId())
                        .senderName(message.getReplyTo().getSender().getName())
                        .build() : null)
                .build();
    }

    @Override
    @Transactional
    public PageResponse<MessageResponse> getMessages(String chatRoomId, int page, int size) {
        Authentication authentication = SecurityUtils.getAuthentication();
        User currentUser = this.userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));


        // Kiểm tra user có trong chatroom không
        boolean isMember = this.userChatRoomRepository.existsByUserIdAndChatRoomId(currentUser.getId(), chatRoomId);
        if (!isMember) {
            throw new AccessDeniedException("You are not a member of this chat room");
        }

        // Lấy userchatroom với lastReadMessage
        Userchatroom userChatroom = this.userChatRoomRepository.findByUserIdAndChatRoomId(currentUser.getId(), chatRoomId);
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("sentAt").descending());
        Page<Message> messagePage = this.messageRepository.findByChatRoomId(chatRoomId, pageRequest);



        // Lấy lastReadMessage ID để so sánh
        String lastReadMessageId = userChatroom != null && userChatroom.getLastReadMessage() != null 
                ? userChatroom.getLastReadMessage().getId() : null;


        // Lấy danh sách messageIds để batch query read info
        List<String> messageIds = messagePage.getContent().stream()
                .map(Message::getId)
                .collect(Collectors.toList());
        if(messageIds.isEmpty()) return null;
        Map<String, List<MemberReadDTO>> readInfoByMessage = this.getReadInfoByLastReadMessage(messageIds, chatRoomId, currentUser.getId());

        List<MessageResponse> messages = messagePage.getContent().stream()
                .map(message -> {
                    // Check xem message này có phải là last read message không
                    boolean isLastRead = lastReadMessageId != null && 
                                       lastReadMessageId.equals(message.getId());
                    
                    // Lấy danh sách ai đã đọc tin nhắn này
                    List<MemberReadDTO> readByList = readInfoByMessage.getOrDefault(message.getId(), new ArrayList<>());
                    
                    return MessageResponse.builder()
                                .messageId(message.getId())
                                .content(message.getContent())
                                .senderId(message.getSender().getId())
                                .senderName(message.getSender().getName())
                                .senderUsername(message.getSender().getUsername())
                                .senderAvatar(message.getSender().getAvatar())
                                .sentAt(message.getSentAt())
                                .messageType(message.getType().name())
                                .messageStatus(message.getMessageStatus().name())
                                .isLastRead(isLastRead)  // Set flag cho last read message
                                .readBy(readByList)      // Danh sách ai đã đọc
                                .readCount(readByList.size())  // Số người đã đọc
                                .replyTo(message.getReplyTo() != null ? MessageResponse.ReplyMessage.builder()
                                        .messageId(message.getReplyTo().getId())
                                        .content(message.getReplyTo().getContent())
                                        .senderId(message.getReplyTo().getSender().getId())
                                        .senderName(message.getReplyTo().getSender().getName())
                                        .build() : null)
                                .build();
                }).collect(Collectors.toList());
        return PageResponse.<MessageResponse>builder()
                .data(messages)
                .page(page)
                .totalPages(messagePage.getTotalPages())
                .totalElements(messagePage.getTotalElements())
                .hasNext(messagePage.hasNext())
                .hasPrevious(messagePage.hasPrevious())
                .build();
    }

    @Override
    @Transactional
    public MessageStatusDTO markReadMessages(String chatRoomId) {
        Authentication authentication = SecurityUtils.getAuthentication();
        User currentUser = this.userRepository.findByUsername(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        boolean isMember = this.userChatRoomRepository.existsByUserIdAndChatRoomId(currentUser.getId(), chatRoomId);
        Chatroom chatroom = this.chatRoomRepository.findById(chatRoomId).orElseThrow(() -> new ResourceNotFoundException("Chat room not found"));
        if(chatroom.getLastMessage() == null) {
            return new MessageStatusDTO();
        }

        if (!isMember) {
            throw new AccessDeniedException("You are not a member of this chat room");
        }

        Userchatroom userchatroom = this.userChatRoomRepository.findByUserIdAndChatRoomId(currentUser.getId(), chatRoomId);
        if(userchatroom.getLastReadMessage() != null && userchatroom.getLastReadMessage().getId().equals(chatroom.getLastMessage().getId())) {
            return new MessageStatusDTO();
        }
        userchatroom.setLastReadMessage(chatroom.getLastMessage());
        userchatroom.setReadAt(Instant.now());
        this.userChatRoomRepository.save(userchatroom);
        return MessageStatusDTO.builder()
                .chatRoomId(chatRoomId)
                .messageId(chatroom.getLastMessage().getId())
                .build();
    }


    @Transactional
    public void updateLastReadMessage(String userId, String chatRoomId, String messageId) {
        try {
            Userchatroom userChatroom = this.userChatRoomRepository.findByUserIdAndChatRoomId(userId, chatRoomId);
            if (userChatroom != null) {
                Message message = this.messageRepository.findById(messageId).orElse(null);
                if (message != null) {
                    userChatroom.setLastReadMessage(message);
                    this.userChatRoomRepository.save(userChatroom);
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating lastReadMessage: " + e.getMessage());
        }
    }

    private Map<String, List<MemberReadDTO>> getReadInfoByLastReadMessage(List<String> messageIds, String chatRoomId, String userId) {

        if (messageIds.isEmpty()) {
            return new HashMap<>();
        }

        // Lấy tất cả messages cần check
        List<Message> messages = this.messageRepository.findAllById(messageIds);

        // Lấy tất cả members với lastReadMessage và lastReadAt
        List<Userchatroom> members = this.userChatRoomRepository.findByChatRoomIdWithUserAndLastRead(chatRoomId);

        Map<String, List<MemberReadDTO>> result = new HashMap<>();

        // Với mỗi message, check xem member nào đã đọc (dựa vào lastReadMessage)
        for (Message message : messages) {
            List<MemberReadDTO> readByUsers = new ArrayList<>();

            for (Userchatroom member : members) {
                // Check xem member này đã đọc message này chưa
                // Logic: lastReadMessage.sentAt >= message.sentAt → đã đọc
                if (!message.getSender().getId().equals(userId) && member.getLastReadMessage() != null && member.getLastReadMessage().getSentAt().compareTo(message.getSentAt()) >= 0) {

                    readByUsers.add(MemberReadDTO.builder()
                            .userId(member.getUser().getId())
                            .name(member.getUser().getName())
                            .readAt(member.getReadAt())
                            .build());
                }
            }

            result.put(message.getId(), readByUsers);
        }

        return result;
    }
}
