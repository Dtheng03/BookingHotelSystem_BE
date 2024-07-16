package com.chinhbean.bookinghotel.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RoomTypeDTO {

    @JsonProperty("hotel_id")
    @NotNull(message = "Hotel ID is required")
    private Long hotelId;

    @JsonProperty("description")
    @NotBlank(message = "Description is required")
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @JsonProperty("number_of_rooms")
    @NotNull(message = "Number of rooms is required")
    @Min(value = 1, message = "There must be at least one room")
    private Integer numberOfRooms;

    @JsonProperty("capacity_per_room")
    @Min(value = 1, message = "Capacity per room must be at least 1")
    @Max(value = 10, message = "Capacity per room cannot exceed 10")
    private int capacityPerRoom;

    @JsonProperty("room_price")
    @NotNull(message = "Room price is required")
    @DecimalMin(value = "1000", message = "Room price must be greater than 1000")
    private Double roomPrice;

    @JsonProperty("room_type_name")
    @NotBlank(message = "Room type name is required")
    private String roomTypeName;

    @JsonProperty("types")
    @NotNull(message = "Room type details are required")
    private TypeRoomDTO types;

    @JsonProperty("conveniences")
    @NotEmpty(message = "At least one convenience is required")
    private Set<ConvenienceRoomDTO> conveniences;
}
