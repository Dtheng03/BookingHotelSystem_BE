package com.chinhbean.bookinghotel.responses.user;

import com.chinhbean.bookinghotel.enums.PackageStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {
    @JsonProperty("id")
    private Long id;

    @NotBlank(message = "email is required")
    private String email;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("address")
    private String address;

    @JsonProperty("is_active")
    private boolean active;

    @JsonProperty("date_of_birth")
    private Date dateOfBirth;

    @JsonProperty("facebook_account_id")
    private String facebookAccountId;

    @JsonProperty("google_account_id")
    private String googleAccountId;

    private String city;

    @JsonProperty("avatar")
    private String avatar;

    @JsonProperty("package_id")
    private Long packageId;

    @Enumerated(EnumType.STRING)
    private PackageStatus status;

    @JsonProperty("role")
    private com.chinhbean.bookinghotel.entities.Role role;

    public static UserResponse fromUser(com.chinhbean.bookinghotel.entities.User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .active(user.isActive())
                .dateOfBirth(user.getDateOfBirth())
                .facebookAccountId(user.getFacebookAccountId())
                .googleAccountId(user.getGoogleAccountId())
                .role(user.getRole())
                .avatar(user.getAvatar())
                .packageId(user.getServicePackage() != null ? user.getServicePackage().getId() : null)
                .status(user.getStatus())
                .build();
    }
}
