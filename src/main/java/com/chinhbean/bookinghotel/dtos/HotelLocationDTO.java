    package com.chinhbean.bookinghotel.dtos;

    import com.fasterxml.jackson.annotation.JsonProperty;
    import jakarta.validation.constraints.NotBlank;
    import jakarta.validation.constraints.NotNull;
    import jakarta.validation.constraints.Size;
    import lombok.AllArgsConstructor;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public class HotelLocationDTO {

        @JsonProperty("address")
        @NotBlank(message = "Address is required")
        @Size(min = 5, max = 200, message = "Address must be between 5 and 200 characters")
        private String address;

        @JsonProperty("province")
        @NotBlank(message = "Province is required")
        private String province;

        @JsonProperty("latitude")
        @NotNull(message = "Latitude is required")
        private Double latitude;

        @JsonProperty("longitude")
        @NotNull(message = "Longitude is required")
        private Double longitude;
    }
