package com.gamingbar.dto.room;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateRoomRequest {

    @NotNull
    private Integer gameId;

    @NotNull
    @Min(2)
    @Max(10)
    private Integer maxPlayer;

    @NotNull
    @Pattern(regexp = "^(instant|scheduled)$")
    private String type;

    private String startTime;
}
