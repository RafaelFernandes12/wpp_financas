package com.code.wpp_financas.controllers;

import com.code.wpp_financas.dtos.IncomingMessageRequest;
import com.code.wpp_financas.dtos.MessageReply;
import com.code.wpp_financas.services.MessageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public MessageReply receive(@RequestBody IncomingMessageRequest request){
        return new MessageReply(messageService.handle(request.phone(), request.text()));
    }
}
