package com.code.wpp_financas.services;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.code.wpp_financas.dtos.CreateAccountRequest;
import com.code.wpp_financas.models.AccountType;
import com.code.wpp_financas.models.ConversationState;
import com.code.wpp_financas.models.OnboardingStep;
import com.code.wpp_financas.models.User;
import com.code.wpp_financas.repositories.ConversationStateRepository;
import com.code.wpp_financas.repositories.UserRepository;
import com.code.wpp_financas.utils.TextUtils;

@Service
public class OnboardingService {

    private final ConversationStateRepository stateRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AccountService accountService;

    public OnboardingService(
            ConversationStateRepository stateRepository,
            UserRepository userRepository,
            UserService userService,
            AccountService accountService) {
        this.stateRepository = stateRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.accountService = accountService;
    }

    public String start(String phone) {
        if (userRepository.findByPhone(phone).isPresent()) {
            return "Você já tem cadastro. Envie !ajuda para ver os comandos.";
        }
        ConversationState state = stateRepository.findByPhone(phone).orElseGet(ConversationState::new);
        state.setPhone(phone);
        state.setStep(OnboardingStep.NAME);
        stateRepository.save(state);
        return "Vamos criar seu cadastro! (envie !cancelar para sair)\nQual é o seu nome completo?";
    }

    @Transactional
    public String answer(ConversationState state, String text) {
        return switch (state.getStep()) {
            case NAME -> onName(state, text);
            case BALANCE -> onBalance(state, text);
            case RESERVE -> onReserve(state, text);
            case HAS_CARD -> onHasCard(state, text);
            case CLOSING_DAY -> onClosingDay(state, text);
            case DUE_DAY -> onDueDay(state, text);
        };
    }

    private String onName(ConversationState state, String text) {
        if (text.trim().length() < 3) {
            return "Não entendi o nome. Pode enviar seu nome completo?";
        }
        state.setName(text.trim());
        state.setStep(OnboardingStep.BALANCE);
        stateRepository.save(state);
        return "Prazer, " + firstName(state) + "! Quanto você tem hoje na conta corrente? (ex.: 1500,50)";
    }

    private String onBalance(ConversationState state, String text) {
        BigDecimal value = TextUtils.parseMoney(text);
        if (value == null) {
            return "Não entendi o valor. Envie só o número, por exemplo 1500,50";
        }
        state.setCheckingBalance(value);
        state.setStep(OnboardingStep.RESERVE);
        stateRepository.save(state);
        return "E quanto você tem de reserva de emergência? Envie 0 se não tiver.";
    }

    private String onReserve(ConversationState state, String text) {
        BigDecimal value = TextUtils.parseMoney(text);
        if (value == null || value.signum() < 0) {
            return "Não entendi o valor. Envie o valor da reserva, ou 0 se não tiver.";
        }
        state.setEmergencyFund(value);
        state.setStep(OnboardingStep.HAS_CARD);
        stateRepository.save(state);
        return "Você tem cartão de crédito? (sim ou não)";
    }

    private String onHasCard(ConversationState state, String text) {
        String answer = TextUtils.normalize(text);
        if (answer.equals("sim") || answer.equals("s")) {
            state.setStep(OnboardingStep.CLOSING_DAY);
            stateRepository.save(state);
            return "Em que dia do mês a fatura do cartão fecha? (1 a 31)";
        }
        if (answer.equals("nao") || answer.equals("n")) {
            return finish(state);
        }
        return "Responda com sim ou não.";
    }

    private String onClosingDay(ConversationState state, String text) {
        Integer day = TextUtils.parseDay(text);
        if (day == null) {
            return "Envie um número de 1 a 31.";
        }
        state.setClosingDay(day);
        state.setStep(OnboardingStep.DUE_DAY);
        stateRepository.save(state);
        return "E em que dia a fatura vence? (1 a 31)";
    }

    private String onDueDay(ConversationState state, String text) {
        Integer day = TextUtils.parseDay(text);
        if (day == null) {
            return "Envie um número de 1 a 31.";
        }
        state.setDueDay(day);
        return finish(state);
    }

    private String finish(ConversationState state) {
        User user = userService.create(state.getName(), state.getPhone());

        accountService.create(new CreateAccountRequest(
                user.getId(), "Conta principal", AccountType.CHECKING,
                state.getCheckingBalance(), null, null, null));

        accountService.create(new CreateAccountRequest(
                user.getId(), "Reserva de emergência", AccountType.EMERGENCY_FUND,
                state.getEmergencyFund(), null, null, null));

        StringBuilder summary = new StringBuilder("Cadastro concluído, " + firstName(state) + "!\n");
        summary.append("Conta corrente: ").append(TextUtils.formatMoney(state.getCheckingBalance())).append("\n");
        summary.append("Reserva: ").append(TextUtils.formatMoney(state.getEmergencyFund())).append("\n");

        if (state.getClosingDay() != null) {
            accountService.create(new CreateAccountRequest(
                    user.getId(), "Cartão de crédito", AccountType.CREDIT_CARD,
                    BigDecimal.ZERO, null, state.getClosingDay(), state.getDueDay()));
            summary.append("Cartão: fecha dia ").append(state.getClosingDay())
                    .append(", vence dia ").append(state.getDueDay()).append("\n");
        }

        stateRepository.delete(state);
        summary.append("Envie !ajuda para ver os comandos.");
        return summary.toString();
    }

    private String firstName(ConversationState state) {
        return state.getName().split("\\s+")[0];
    }
}