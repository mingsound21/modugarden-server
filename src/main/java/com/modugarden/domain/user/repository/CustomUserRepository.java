package com.modugarden.domain.user.repository;

import com.modugarden.domain.user.entity.User;

import java.util.List;
import java.util.Optional;

public interface CustomUserRepository {

    Optional<User> readUserInfo(Long userId);

    List<String> readUserInterestCategory(Long userId);
}
