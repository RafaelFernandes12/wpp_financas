package com.code.wpp_financas.dtos;

import com.code.wpp_financas.models.User;

public record UserResponse(Long id, String name, String phone) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getPhone());
    }
}