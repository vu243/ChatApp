package com.nhv.chatapp.service.impl;

import com.nhv.chatapp.dto.response.FriendRequestResponse;
import com.nhv.chatapp.entity.Contact;
import com.nhv.chatapp.entity.Friendrequest;
import com.nhv.chatapp.entity.User;
import com.nhv.chatapp.entity.enums.FriendRequestStatus;
import com.nhv.chatapp.exception.BadRequestException;
import com.nhv.chatapp.exception.ResourceNotFoundException;
import com.nhv.chatapp.repository.ContactRepository;
import com.nhv.chatapp.repository.FriendRequestRepository;
import com.nhv.chatapp.repository.UserRepository;
import com.nhv.chatapp.service.FriendRequestService;
import com.nhv.chatapp.service.UserService;
import com.nhv.chatapp.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FriendRequestServiceImpl implements FriendRequestService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ContactRepository contactRepository;
    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Override
    @Transactional
    public FriendRequestResponse sendFriendRequest(String userId) {
        Authentication authentication = SecurityUtils.getAuthentication();
        User currentUser = this.userRepository.findByUsername(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User recipient = this.userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (currentUser.getId().equals(recipient.getId())) {
            throw new ResourceNotFoundException("Cannot send friend request to yourself");
        }
        if(this.contactRepository.findByUserIdAndContactUserId(currentUser.getId(), userId) != null) {
            throw new BadRequestException("Already being friend");
        }
        if(this.friendRequestRepository.findByRequesterIdAndRecipientIdAndStatus(currentUser.getId(), userId, FriendRequestStatus.PENDING).isPresent()){
            throw new BadRequestException("Friend request already send");
        }
        if(this.friendRequestRepository.findByRequesterIdAndRecipientIdAndStatus(userId, currentUser.getId(), FriendRequestStatus.PENDING).isPresent()){
            throw new BadRequestException("This user has already sent you a friend request");
        }

        Friendrequest friendrequest = Friendrequest.builder()
                .requester(currentUser)
                .recipient(recipient)
                .status(FriendRequestStatus.PENDING)
                .build();

        friendrequest = this.friendRequestRepository.save(friendrequest);

        return FriendRequestResponse.builder()
                .requesterId(currentUser.getId())
                .requesterName(currentUser.getName())
                .recipientId(recipient.getId())
                .recipientName(recipient.getName())
                .status(friendrequest.getStatus().name())
                .createdAt(friendrequest.getCreateAt())
                .build();
    }

    @Override
    public void acceptFriendRequest(String userId, FriendRequestStatus status) {
        Authentication authentication = SecurityUtils.getAuthentication();
        User currenUser = this.userRepository.findByUsername(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        System.out.println(currenUser.getUsername());
        System.out.println(this.userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found")).getUsername());
        Friendrequest friendrequest = this.friendRequestRepository.findByRequesterIdAndRecipientId(userId, currenUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Friendrequest not found"));

        if(!friendrequest.getRecipient().getUsername().equals(authentication.getName())) {
            throw new BadRequestException("You can only update requests sent to you");
        }
        System.out.println(friendrequest.getStatus().name());
        if(!friendrequest.getStatus().equals(FriendRequestStatus.PENDING)) {
            throw new BadRequestException("Friend request is not pending");
        }
        friendrequest.setStatus(status);
        this.friendRequestRepository.save(friendrequest);
        if(friendrequest.getStatus().equals(FriendRequestStatus.ACCEPTED)) {
            Contact requester = Contact.builder()
                    .user(friendrequest.getRequester())
                    .contactUser(friendrequest.getRecipient())
                    .build();
            Contact recipient = Contact.builder()
                    .user(friendrequest.getRecipient())
                    .contactUser(friendrequest.getRequester())
                    .build();
            this.contactRepository.save(requester);
            this.contactRepository.save(recipient);
        }
    }

    @Override
    public List<FriendRequestResponse> getRequesters() {
        Authentication authentication =  SecurityUtils.getAuthentication();
        User user = this.userRepository.findByUsername(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<Friendrequest> requesters = this.friendRequestRepository.findByRecipientIdAndStatus(user.getId(), FriendRequestStatus.PENDING);
        return requesters.stream().map(requester -> FriendRequestResponse.builder()
                .id(requester.getId())
                .recipientId(user.getId())
                .recipientName(user.getName())
                .recipientAvatar(user.getAvatar())
                .requesterId(requester.getRequester().getId())
                .requesterName(requester.getRequester().getName())
                .requesterAvatar(requester.getRequester().getAvatar())
                .createdAt(requester.getCreateAt())
                .status(requester.getStatus().name())
                .build()).collect(Collectors.toList());
    }

    @Override
    public List<FriendRequestResponse> getRecipients() {
        Authentication authentication =  SecurityUtils.getAuthentication();
        User user = this.userRepository.findByUsername(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<Friendrequest> recipients = this.friendRequestRepository.findByRequesterIdAndStatus(user.getId(), FriendRequestStatus.PENDING);
        return recipients.stream().map(recipient -> FriendRequestResponse.builder()
                .id(recipient.getId())
                .requesterId(user.getId())
                .requesterName(user.getName())
                .requesterAvatar(user.getAvatar())
                .recipientId(recipient.getRecipient().getId())
                .recipientName(recipient.getRecipient().getName())
                .recipientAvatar(recipient.getRecipient().getAvatar())
                .createdAt(recipient.getCreateAt())
                .status(recipient.getStatus().name())
                .build()).collect(Collectors.toList());
    }

    @Override
    public void deleteFriendRequest(String userId) {
        Authentication authentication =  SecurityUtils.getAuthentication();
        User currentUser  = this.userRepository.findByUsername(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User user = this.userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Optional<Friendrequest> friendrequest= this.friendRequestRepository.findByRequesterIdAndRecipientId(currentUser.getId(), user.getId());
        if(friendrequest.isPresent()) {
            this.friendRequestRepository.delete(friendrequest.get());
        }
        else{
            friendrequest = this.friendRequestRepository.findByRequesterIdAndRecipientId(user.getId(), currentUser.getId());
            friendrequest.ifPresent(this.friendRequestRepository::delete);
        }

    }


}
