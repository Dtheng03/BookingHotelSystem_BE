package com.chinhbean.bookinghotel.configurations;

import com.chinhbean.bookinghotel.entities.User;
import com.chinhbean.bookinghotel.services.oauth2.IOAuth2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Configuration
@RequiredArgsConstructor
public class OAuth2Config {
    private final IOAuth2Service oAuth2Service;

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService() {
        return new CustomOAuth2UserService(oAuth2Service);
    }

    public static class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
        private final IOAuth2Service oAuth2Service;

        public CustomOAuth2UserService(IOAuth2Service oAuth2Service) {
            this.oAuth2Service = oAuth2Service;
        }

        @Override
        public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
            DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
            OAuth2User oauth2User = delegate.loadUser(userRequest);

            String registrationId = userRequest.getClientRegistration().getRegistrationId();
            String email = oauth2User.getAttribute("email");
            String name = oauth2User.getAttribute("name");

            User user;
            if ("google".equals(registrationId)) {
                String googleId = oauth2User.getAttribute("sub");
                user = oAuth2Service.processGoogleUser(email, name, googleId);
            } else if ("facebook".equals(registrationId)) {
                String facebookId = oauth2User.getAttribute("id");
                user = oAuth2Service.processFacebookUser(email, name, facebookId);
            } else {
                throw new OAuth2AuthenticationException("Login with " + registrationId + " is not supported");
            }

            return new OAuth2UserDetails(user, oauth2User.getAttributes());
        }
    }
}