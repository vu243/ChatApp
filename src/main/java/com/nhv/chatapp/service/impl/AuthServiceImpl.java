package com.nhv.chatapp.service.impl;

import com.nhv.chatapp.dto.UserAuthDTO;
import com.nhv.chatapp.dto.request.ChangePasswordRequest;
import com.nhv.chatapp.dto.request.LoginRequest;
import com.nhv.chatapp.dto.request.RegisterRequest;
import com.nhv.chatapp.entity.User;
import com.nhv.chatapp.exception.BadRequestException;
import com.nhv.chatapp.repository.UserRepository;
import com.nhv.chatapp.service.AuthService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthServiceImpl implements AuthService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JWTService jwtService;


    @Override
    public void register(RegisterRequest registerRequest) {
        User user = new User();
        if(this.userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new BadCredentialsException("Username is already exists");
        }
        if(this.userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new BadCredentialsException("Email is already exists");
        }
        user.setUsername(registerRequest.getUsername());
        user.setAvatar("https://res.cloudinary.com/dcrsia5sh/image/upload/v1758529587/avatar-facebook-1280x720_p4insi.jpg");
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEmail(registerRequest.getEmail());
        user.setName(registerRequest.getName());

        userRepository.save(user);
    }

    @Override
    public void changePassword(ChangePasswordRequest changePasswordRequest) {

    }

    @Override
    public UserAuthDTO login(LoginRequest loginRequest) {
        User user = this.userRepository.findByUsername(loginRequest.getUsername().trim()).orElseThrow(() -> new BadRequestException("User not found"));
        if(!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new BadRequestException("Wrong password");
        }
        var token = this.jwtService.generateToken(user.getUsername());
        return UserAuthDTO.builder()
                .token(token)
                .avatar(user.getAvatar())
                .name(user.getName())
                .build();
    }
}
