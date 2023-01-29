package com.modugarden.domain.follow.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
@AllArgsConstructor
@Getter
public class FollowingsResponseDto {
    private Long userId;
    private String nickname;
    private String profileImage;
    private List<String> categories;
    private boolean isFollow;
}
