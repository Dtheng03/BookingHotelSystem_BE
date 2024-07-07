package com.chinhbean.bookinghotel.services.oauth2;

import com.chinhbean.bookinghotel.dtos.DataMailDTO;
import com.chinhbean.bookinghotel.entities.Role;
import com.chinhbean.bookinghotel.entities.User;
import com.chinhbean.bookinghotel.repositories.IRoleRepository;
import com.chinhbean.bookinghotel.repositories.IUserRepository;
import com.chinhbean.bookinghotel.services.sendmails.MailService;
import com.chinhbean.bookinghotel.utils.MailTemplate;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuth2Service implements IOAuth2Service {
    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private static final Logger logger = LoggerFactory.getLogger(OAuth2Service.class);

    @Transactional
    @Override
    public User processGoogleUser(String email, String name, String googleId) {
        return userRepository.findByEmail(email)
                .map(this::updateGoogleUser)
                .orElseGet(() -> createOAuth2User(email, name, googleId, true));
    }

    @Transactional
    @Override
    public User processFacebookUser(String email, String name, String facebookId) {
        return userRepository.findByEmail(email)
                .map(this::updateFacebookUser)
                .orElseGet(() -> createOAuth2User(email, name, facebookId, false));
    }

    private User updateGoogleUser(User user) {
        if (user.getGoogleAccountId() == null) {
            user.setGoogleAccountId(user.getEmail());
            return userRepository.save(user);
        }
        return user;
    }

    private User updateFacebookUser(User user) {
        if (user.getFacebookAccountId() == null) {
            user.setFacebookAccountId(user.getEmail());
            return userRepository.save(user);
        }
        return user;
    }

    private User createOAuth2User(String email, String name, String accountId, boolean isGoogle) {
        String randomPassword = UUID.randomUUID().toString().substring(0, 8);
        User newUser = User.builder()
                .email(email)
                .fullName(name)
                .password(passwordEncoder.encode(randomPassword))
                .active(true)
                .role(roleRepository.findByRoleName(Role.CUSTOMER))
                .build();

        if (isGoogle) {
            newUser.setGoogleAccountId(accountId);
        } else {
            newUser.setFacebookAccountId(accountId);
        }

        User savedUser = userRepository.save(newUser);
        sendPasswordEmail(email, randomPassword, name);
        return savedUser;
    }


    private void sendPasswordEmail(String email, String password, String name) {
        Map<String, Object> props = new HashMap<>();
        props.put("email", email);
        props.put("password", password);
        props.put("name", name);

        DataMailDTO mailData = new DataMailDTO(email, MailTemplate.SEND_MAIL_SUBJECT.NEW_PASSWORD, "", props);

        try {
            mailService.sendHtmlMail(mailData, MailTemplate.SEND_MAIL_TEMPLATE.NEW_PASSWORD);
        } catch (MessagingException e) {
            logger.error("Error sending new password", e);
        }
    }
}
