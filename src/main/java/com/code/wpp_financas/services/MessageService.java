package com.code.wpp_financas.services;

import org.springframework.stereotype.Service;

@Service
public class MessageService {

    public String handle(String phone, String text){
        String command = text == null ? "" : text.trim().toLowerCase();

        if(command.equals("!ajuda")){
            return "Comandos disponíveis (em breve): !novousuario, !gasto, !saldo, !fatura, !relatorio";
        }

        return "Recebi sua mensagem: " + text;
    }
}
