package com.code.wpp_financas.controllers;

import com.code.wpp_financas.dtos.CreateUserRequest;
import com.code.wpp_financas.dtos.UserResponse;
import com.code.wpp_financas.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@RequestBody CreateUserRequest request){
        return UserResponse.from(userService.create(request.name(), request.phone()));
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id){
        return UserResponse.from(userService.getById(id));
    }
}
