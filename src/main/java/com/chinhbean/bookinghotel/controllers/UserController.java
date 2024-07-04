package com.chinhbean.bookinghotel.controllers;


import com.chinhbean.bookinghotel.components.JwtTokenUtils;
import com.chinhbean.bookinghotel.components.LocalizationUtils;
import com.chinhbean.bookinghotel.dtos.ChangePasswordDTO;
import com.chinhbean.bookinghotel.dtos.RefreshTokenDTO;
import com.chinhbean.bookinghotel.dtos.UserDTO;
import com.chinhbean.bookinghotel.dtos.UserLoginDTO;
import com.chinhbean.bookinghotel.entities.Token;
import com.chinhbean.bookinghotel.entities.User;
import com.chinhbean.bookinghotel.exceptions.DataNotFoundException;
import com.chinhbean.bookinghotel.repositories.IUserRepository;
import com.chinhbean.bookinghotel.responses.ResponseObject;
import com.chinhbean.bookinghotel.responses.user.LoginResponse;
import com.chinhbean.bookinghotel.responses.user.UserListResponse;
import com.chinhbean.bookinghotel.responses.user.UserResponse;
import com.chinhbean.bookinghotel.services.oauth2.IOAuth2Service;
import com.chinhbean.bookinghotel.services.token.ITokenService;
import com.chinhbean.bookinghotel.services.user.IUserService;
import com.chinhbean.bookinghotel.utils.MessageKeys;
import com.chinhbean.bookinghotel.utils.ValidationUtils;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api.prefix}/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")

public class UserController {
    private final IUserService userService;
    private final ITokenService tokenService;
    private final JwtTokenUtils jwtTokenUtils;
    private final LocalizationUtils localizationUtils;
    private final IUserRepository IUserRepository;
    private final IOAuth2Service oAuth2Service;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @GetMapping("/generate-secret-key")
    public ResponseEntity<?> generateSecretKey() {
        return ResponseEntity.ok(jwtTokenUtils.generateSecretKey());
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseObject> registerUser(
            @Valid @RequestBody UserDTO userDTO,
            BindingResult result
    ) throws Exception {
        if (result.hasErrors()) {
            List<String> errorMessages = result.getFieldErrors()
                    .stream()
                    .map(FieldError::getDefaultMessage)
                    .toList();

            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .data(null)
                    .message(errorMessages.toString())
                    .build());
        }
        if (userDTO.getEmail() == null || userDTO.getEmail().trim().isBlank()) {
            if (userDTO.getPhoneNumber() == null || userDTO.getPhoneNumber().trim().isBlank()) {
                return ResponseEntity.badRequest().body(ResponseObject.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .data(null)
                        .message("At least email or phone number is required")
                        .build());

            } else {
                if (!ValidationUtils.isValidPhoneNumber(userDTO.getPhoneNumber())) {
                    throw new Exception("Invalid phone number");
                }
            }
        } else {
            if (!ValidationUtils.isValidEmail(userDTO.getEmail())) {
                throw new Exception("Invalid email");
            }
        }
        if (IUserRepository.existsByPhoneNumber(userDTO.getPhoneNumber())) {
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .data(null)
                    .message(localizationUtils.getLocalizedMessage(MessageKeys.PHONE_NUMBER_ALREADY_EXISTS))
                    .build());
        }
        if (!userDTO.getPassword().equals(userDTO.getRetypePassword())) {
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .data(null)
                    .message(localizationUtils.getLocalizedMessage(MessageKeys.PASSWORD_NOT_MATCH))
                    .build());
        }
        User user = userService.registerUser(userDTO);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.CREATED)
                .data(UserResponse.fromUser(user))
                .message(MessageKeys.REGISTER_SUCCESSFULLY)
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseObject> login(
            @Valid @RequestBody UserLoginDTO userLoginDTO,
            HttpServletRequest request
    ) throws Exception {
        String token = userService.login(userLoginDTO);
        String userAgent = request.getHeader("User-Agent");

        User userDetail = userService.getUserDetailsFromToken(token);

        Token jwtToken = tokenService.addToken(userDetail, token, isMobileDevice(userAgent));

        LoginResponse loginResponse = LoginResponse.builder()
                .message(MessageKeys.LOGIN_SUCCESSFULLY)
                .token(jwtToken.getToken())
                .tokenType(jwtToken.getTokenType())
                .refreshToken(jwtToken.getRefreshToken())
                .fullName(userDetail.getFullName())
                .email(userDetail.getEmail())
                .phoneNumber(userDetail.getPhoneNumber())
                .roles(userDetail.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .id(userDetail.getId())
                .build();
        return ResponseEntity.ok().body(ResponseObject.builder()
                .message(MessageKeys.LOGIN_SUCCESSFULLY)
                .data(loginResponse)
                .status(HttpStatus.OK)
                .build());
    }

    @PostMapping("/logout")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_PARTNER','ROLE_CUSTOMER')")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        try {
            String authorizationHeader = request.getHeader("Authorization");
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                tokenService.deleteToken(token);
                return ResponseEntity.ok().body("Logout successful.");
            } else {
                return ResponseEntity.badRequest().body("No token provided.");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("An error occurred during logout: " + e.getMessage());
        }
    }

    private boolean isMobileDevice(String userAgent) {
        return userAgent.toLowerCase().contains("mobile");
    }

    //@PreAuthorize("hasAnyAuthority('ADMIN', 'PARTNER', 'CUSTOMER')")
    @PutMapping(value = "/update-avatar/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseObject> updateUserAvatar(@PathVariable long id,
                                                           @RequestParam("avatar") MultipartFile avatar) {
        User user = userService.updateUserAvatar(id, avatar);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .data(UserResponse.fromUser(user))
                .message(MessageKeys.UPDATE_AVATAR_SUCCESSFULLY)
                .build());
    }

    @PutMapping("/update-password/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_PARTNER', 'ROLE_CUSTOMER')")

    public ResponseEntity<ResponseObject> changePassword(
            @PathVariable long id,
            @Valid @RequestBody ChangePasswordDTO changePasswordDTO) {
        try {
            userService.changePassword(id, changePasswordDTO);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message(MessageKeys.CHANGE_PASSWORD_SUCCESSFULLY)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseObject.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message(e.getMessage())
                    .build());
        }
    }

    @PutMapping("/block-or-enable/{userId}/{active}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")

    public ResponseEntity<String> blockOrEnable(
            @Valid @PathVariable long userId,
            @Valid @PathVariable int active) {
        try {
            userService.blockOrEnable(userId, active > 0);
            String message = active > 0 ? MessageKeys.ENABLE_USER_SUCCESSFULLY : MessageKeys.BLOCK_USER_SUCCESSFULLY;
            return ResponseEntity.ok().body(message);
        } catch (DataNotFoundException e) {
            return ResponseEntity.badRequest().body(MessageKeys.USER_NOT_FOUND);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/get-all-user")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_PARTNER','ROLE_CUSTOMER')")
    public ResponseEntity<UserListResponse> getAllUsers(
            @RequestParam(defaultValue = "") String keyword,
            @NonNull @RequestParam("page") int page,
            @RequestParam("limit") int limit) {
        try {
            PageRequest pageRequest = PageRequest.of(page, limit, Sort.by("fullName").ascending());
            Page<UserResponse> userPage = userService.getAllUsers(keyword, pageRequest);

            int totalPages = userPage.getTotalPages();
            List<UserResponse> users = userPage.getContent();

            UserListResponse userListResponse = UserListResponse.builder()
                    .users(users)
                    .totalPages(totalPages)
                    .message("Success")
                    .build();

            return ResponseEntity.ok(userListResponse);
        } catch (IllegalArgumentException e) {
            UserListResponse errorResponse = UserListResponse.builder()
                    .users(Collections.emptyList())
                    .totalPages(0)
                    .message("Invalid parameters")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errorResponse);
        } catch (Exception e) {
            UserListResponse errorResponse = UserListResponse.builder()
                    .users(Collections.emptyList())
                    .totalPages(0)
                    .message("Failed to retrieve users")
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    @GetMapping("/get-user/{id}")
    public ResponseEntity<UserResponse> getUser(@Valid @PathVariable Long id) {
        try {
            User user = userService.getUser(id);
            return ResponseEntity.ok(UserResponse.fromUser(user));
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Transactional
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable("id") Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok("Delete User Successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred");
        }
    }

    @Transactional
    @PutMapping("/update-profile")
    public ResponseEntity<?> updateUser(@RequestBody UserDTO userDTO) {
        try {
            userService.updateUser(userDTO);
            return ResponseEntity.ok("Update Successfully");
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update user: " + e.getMessage());
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<LoginResponse> refreshToken(
            @Valid @RequestBody RefreshTokenDTO refreshTokenDTO
    ) {
        try {
            User userDetail = userService.getUserDetailsFromRefreshToken(refreshTokenDTO.getRefreshToken());
            Token jwtToken = tokenService.refreshToken(refreshTokenDTO.getRefreshToken(), userDetail);
            return ResponseEntity.ok(LoginResponse.builder()
                    .message(MessageKeys.LOGIN_SUCCESSFULLY)
                    .token(jwtToken.getToken())
                    .tokenType(jwtToken.getTokenType())
                    .refreshToken(jwtToken.getRefreshToken())
                    .fullName(userDetail.getFullName())
                    .email(userDetail.getEmail())
                    .phoneNumber(userDetail.getPhoneNumber())
                    .roles(userDetail.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                    .id(userDetail.getId())
                    .build());

        } catch (Exception e) {
            String errorMessage = "Error occurred during token refresh: " + e.getMessage();
            LoginResponse errorResponse = LoginResponse.builder()
                    .message(errorMessage)
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        }

    }

    @GetMapping("/get-all-users/{roleId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getUsers(@PathVariable Long roleId) {
        try {
            List<UserResponse> users = userService.getAllUsers(roleId);
            return ResponseEntity.ok(users);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid roleId: " + roleId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving users: " + e.getMessage());
        }
    }

    @PostMapping("/oauth2/facebook")
    public ResponseEntity<LoginResponse> handleFacebookLogin(@RequestParam String accessToken, HttpServletRequest request) {
        try {
            String facebookGraphApiUrl = "https://graph.facebook.com/me?fields=id,name,email&access_token=" + accessToken;
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    facebookGraphApiUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );
            Map<String, Object> userAttributes = response.getBody();

            if (userAttributes != null && userAttributes.containsKey("id")) {
                String facebookId = (String) userAttributes.get("id");
                String email = (String) userAttributes.get("email");
                String name = (String) userAttributes.get("name");

                User user = oAuth2Service.processFacebookUser(email, name, facebookId);

                String userAgent = request.getHeader("User-Agent");
                String jwtToken = jwtTokenUtils.generateToken(user);
                Token savedToken = tokenService.addToken(user, jwtToken, isMobileDevice(userAgent));

                LoginResponse loginResponse = LoginResponse.builder()
                        .message("Facebook login successful")
                        .token(savedToken.getToken())
                        .tokenType(savedToken.getTokenType())
                        .refreshToken(savedToken.getRefreshToken())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .phoneNumber(user.getPhoneNumber())
                        .roles(user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                        .id(user.getId())
                        .build();
                return ResponseEntity.ok(loginResponse);
            } else {
                return ResponseEntity.badRequest().body(LoginResponse.builder()
                        .message("Invalid Facebook access token")
                        .build());
            }
        } catch (Exception e) {
            logger.error("Error during Facebook login: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(LoginResponse.builder()
                    .message("Facebook login failed: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/oauth2/google")
    public ResponseEntity<LoginResponse> handleGoogleLogin(@RequestParam String token, HttpServletRequest request) {
        try {
            if (token == null || token.isEmpty()) {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }

            if (token == null || token.isEmpty()) {
                return ResponseEntity.badRequest().body(LoginResponse.builder().message("Token is required").build());
            }
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            logger.info("Attempting to verify token with Google...");
            GoogleIdToken idToken = verifier.verify(token);

            if (idToken != null) {
                logger.info("Token verified successfully");
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name");
                String googleId = payload.getSubject();

                User user = oAuth2Service.processGoogleUser(email, name, googleId);

                String userAgent = request.getHeader("User-Agent");
                String jwtToken = jwtTokenUtils.generateToken(user);
                Token savedToken = tokenService.addToken(user, jwtToken, isMobileDevice(userAgent));

                LoginResponse loginResponse = LoginResponse.builder()
                        .message("Google login successful")
                        .token(savedToken.getToken())
                        .tokenType(savedToken.getTokenType())
                        .refreshToken(savedToken.getRefreshToken())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .phoneNumber(user.getPhoneNumber())
                        .roles(user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                        .id(user.getId())
                        .build();
                return ResponseEntity.ok(loginResponse);
            } else {
                logger.error("Invalid ID token. Token could not be verified.");
                LoginResponse errorResponse = LoginResponse.builder()
                        .message("Invalid ID token")
                        .build();
                return ResponseEntity.badRequest().body(errorResponse);
            }
        } catch (Exception e) {
            logger.error("Error during Google login: ", e);
            LoginResponse errorResponse = LoginResponse.builder()
                    .message("Google login failed: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}