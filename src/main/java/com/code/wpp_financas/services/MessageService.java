package com.code.wpp_financas.services;

import com.code.wpp_financas.models.ConversationState;
import com.code.wpp_financas.models.User;
import com.code.wpp_financas.repositories.ConversationStateRepository;
import com.code.wpp_financas.repositories.UserRepository;
import com.code.wpp_financas.utils.TextUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.function.Function;

@Service
public class MessageService {

    private static final String NOT_REGISTRED = "Você ainda não tem cadastro. Envie !novousuario para começar.";

    private final UserRepository userRepository;
    private final ConversationStateRepository stateRepository;
    private final OnboardingService onboardingService;
    private final BalanceService balanceService;

    public MessageService(
            UserRepository userRepository, ConversationStateRepository stateRepository, OnboardingService onboardingService, BalanceService balanceService
    ){
        this.userRepository = userRepository;
        this.stateRepository = stateRepository;
        this.onboardingService = onboardingService;
        this.balanceService = balanceService;
    }

    // devolve o texto da resposta: texto vazio significa "não responder nada"
    public String handle(String phone, String text){
        String body = text == null ? "" : text.trim();

        // no meio do cadastro: a mensagem é resposta à pergunta atual
        Optional<ConversationState> state = stateRepository.findByPhone(phone);
        if(state.isPresent()){
            String command = TextUtils.normalize(body);
            if(command.equals("!cancelar")){
                stateRepository.delete(state.get());
                return "Cadastro cancelado. Envie !novousuario para recomeçar.";
            }
            if(command.equals("!novousuario")){
                return onboardingService.start(phone);
            }
            return onboardingService.answer(state.get(), body);
        }

        // conversa solta, sem comando
        if(!body.startsWith("!")) {
            return "Se quiser conversar procure um amigo, se não tem nenhum, *eu não quero ser o primeiro*!";
        }

        String[] parts = body.split("\\s+", 2);
        String command = TextUtils.normalize(parts[0]);
        String args = parts.length > 1 ? parts[1].trim() : ""; // vamos usar no !gasto

        return switch (command){
            cade "!ajuda" -> help();
            case "!novousuario" -> onboardingService.start(phone);
            case "!saldo" -> withUser(phone, user -> "Saldo disponível: " + TextUtils.formatMoney(balanceService.spendableBalance(user.getId())));
            case "!reserva" -> withUser(phone, user -> "Reserva de emergência: " + TextUtils.formatMoney(balanceService.emergencyFund(user.getId())));
        };
    }

    private String withUser(String phone, Function<User, String> action){
        return userRepository.findByPhone(phone).map(action).orElse(NOT_REGISTRED);
    }

    private String help(){
        return """
            Comandos:
            !novousuario - criar seu cadastro
            !saldo - quanto você pode gastar
            !reserva - saldo da reserva de emergência
            !ajuda - esta lista
            (em breve: !gasto, !receita, !fatura, !relatorio)""";
    }
}
