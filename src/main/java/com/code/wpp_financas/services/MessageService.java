package com.code.wpp_financas.services;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.code.wpp_financas.models.ConversationState;
import com.code.wpp_financas.models.User;
import com.code.wpp_financas.repositories.ConversationStateRepository;
import com.code.wpp_financas.repositories.UserRepository;
import com.code.wpp_financas.utils.TextUtils;

@Service
public class MessageService {

    private static final String NOT_REGISTERED =
            "Você ainda não tem cadastro. Envie !novousuario para começar.";

    private final UserRepository userRepository;
    private final ConversationStateRepository stateRepository;
    private final OnboardingService onboardingService;
    private final BalanceService balanceService;

    public MessageService(
            UserRepository userRepository,
            ConversationStateRepository stateRepository,
            OnboardingService onboardingService,
            BalanceService balanceService) {
        this.userRepository = userRepository;
        this.stateRepository = stateRepository;
        this.onboardingService = onboardingService;
        this.balanceService = balanceService;
    }

    // devolve o texto da resposta; texto vazio significa "não responder nada"
    public String handle(String phone, String text) {
        String body = text == null ? "" : text.trim();

        // no meio do cadastro: a mensagem é resposta à pergunta atual
        Optional<ConversationState> state = stateRepository.findByPhone(phone);
        if (state.isPresent()) {
            String command = TextUtils.normalize(body);
            if (command.equals("!cancelar")) {
                stateRepository.delete(state.get());
                return "Cadastro cancelado. Envie !novousuario para recomeçar.";
            }
            if (command.equals("!novousuario")) {
                return onboardingService.start(phone);
            }
            return onboardingService.answer(state.get(), body);
        }

        // conversa solta, sem comando: o bot fica quieto
        if (!body.startsWith("!")) {
            return "";
        }

        String[] parts = body.split("\\s+", 2);
        String command = TextUtils.normalize(parts[0]);
        String args = parts.length > 1 ? parts[1].trim() : ""; // vamos usar no !gasto

        return switch (command) {
            case "!ajuda" -> help(args);
            case "!novousuario" -> onboardingService.start(phone);
            case "!saldo" -> withUser(phone, user ->
                    "Saldo disponível: " + TextUtils.formatMoney(balanceService.spendableBalance(user.getId())));
            case "!reserva" -> withUser(phone, user ->
                    "Reserva de emergência: " + TextUtils.formatMoney(balanceService.emergencyFund(user.getId())));
            default -> "Não conheço esse comando. Envie !ajuda para ver a lista.";
        };
    }

    private String withUser(String phone, Function<User, String> action) {
        return userRepository.findByPhone(phone).map(action).orElse(NOT_REGISTERED);
    }

    private static final String GENERAL_HELP = """
    Olá! Eu te ajudo a controlar suas finanças.

    *Comandos disponíveis*
    !novousuario - criar seu cadastro
    !saldo - quanto você pode gastar agora
    !reserva - saldo da sua reserva de emergência
    !ajuda - esta lista

    *Em breve*
    !gasto, !receita, !fatura e !relatorio

    Para ver os detalhes de um comando, envie por exemplo: !ajuda saldo""";

    private static final Map<String, String> HELP_TOPICS = Map.of(
            "novousuario", """
        *!novousuario*
        Cria o seu cadastro. Eu pergunto o seu nome, quanto você tem hoje na conta corrente, quanto tem de reserva de emergência e se você tem cartão de crédito (com o dia em que a fatura fecha e o dia em que vence).
        Para desistir no meio, envie !cancelar.""",
            "saldo", """
        *!saldo*
        Mostra quanto você tem disponível para gastar: o saldo da conta corrente, mais as receitas, menos os gastos.
        A reserva de emergência e os gastos no cartão não entram nessa conta.""",
            "reserva", """
        *!reserva*
        Mostra quanto você tem guardado na reserva de emergência. Ela fica separada do !saldo de propósito, para você não gastar sem querer o que é reserva."""
    );

    private String help(String topic) {
        String key = TextUtils.normalize(topic).replace("!", "");
        if (key.isEmpty()) {
            return GENERAL_HELP;
        }
        return HELP_TOPICS.getOrDefault(
                key, "Não encontrei ajuda para \"" + topic + "\". Envie !ajuda para ver a lista.");
    }
}