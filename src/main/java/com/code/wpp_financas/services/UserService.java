package com.code.wpp_financas.services;

import com.code.wpp_financas.models.User;
import com.code.wpp_financas.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User create(String name, String phone){
        if(name == null || name.isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome é obrigatório");
        }

        //guarda só os dígitos
        String digits = phone == null ? "" : phone.replaceAll("\\D", "");
        if(digits.length() < 10 || digits.length() > 15){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Telefone inválido");
        }

        if (userRepository.findByPhone(digits).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Telefone já cadastrado");
        }

        User user = new User();
        user.setName(name.trim());
        user.setPhone(digits);
        return userRepository.save(user);
    }

    public User getById(Long id){
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }
}
