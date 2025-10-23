package com.nhv.chatapp.config;

import com.nhv.chatapp.service.UserService;
import com.nhv.chatapp.service.impl.JWTService;
import com.nhv.chatapp.service.impl.UserServiceImpl;
import com.nimbusds.jose.JOSEException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.text.ParseException;
import java.util.Collections;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JWTService jwtService;

    @PostConstruct
    public void init() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins("http://localhost:3000").withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if(StompCommand.CONNECT == accessor.getCommand()) {
                    String authToken = accessor.getFirstNativeHeader("Authorization");
                    if(authToken != null && authToken.startsWith("Bearer ")) {
                        String token = authToken.substring(7);
                        String username = null;
                        try {
                            username = jwtService.extractUsername(token);
                            if(username != null) {
                                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
                                accessor.setUser(authentication);
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                                System.out.println(SecurityContextHolder.getContext().getAuthentication().getName()+ "hehehehe");
                            }
                            else {
                                throw new RuntimeException("Invalid token");
                            }
                        } catch (ParseException e) {
                            throw new RuntimeException(e.getMessage());
                        } catch (JOSEException e) {
                            throw new RuntimeException(e);
                        } catch (RuntimeException e) {
                            throw new RuntimeException("Invalid token");
                        }
                    }
                    else  {
                        throw new RuntimeException("Missing Bearer Token");
                    }
                }else if(StompCommand.DISCONNECT == accessor.getCommand()) {

                }
                if (StompCommand.SUBSCRIBE == accessor.getCommand() || StompCommand.SEND == accessor.getCommand()) {
                    Principal user = accessor.getUser();
                    if(user instanceof UsernamePasswordAuthenticationToken) {
                        SecurityContextHolder.getContext().setAuthentication((UsernamePasswordAuthenticationToken) user);
                        System.out.println(SecurityContextHolder.getContext().getAuthentication().getName()+ "hehehehe");
                    }
                }
                return message;
            }
        });
    }
}
