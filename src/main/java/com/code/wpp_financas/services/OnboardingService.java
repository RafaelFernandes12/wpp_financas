package com.code.wpp_financas.services;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.code.wpp_financas.dtos.CreateAccountRequest;
import com.code.wpp_financas.models.Account;
import com.code.wpp_financas.models.AccountType;
import com.code.wpp_financas.models.ConversationState;
import com.code.wpp_financas.models.OnboardingStep;
import com.code.wpp_financas.models.User;
import com.code.wpp_financas.repositories.AccountRepository;
import com.code.wpp_financas.repositories.ConversationStateRepository;
import com.code.wpp_financas.repositories.UserRepository;
import com.code.wpp_financas.utils.TextUtils;

@Service
public class OnboardingService {

    private static final String YES_OR_NO = "Responda com sim ou não.";

    private final ConversationStateRepository stateRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final UserService userService;
    private final AccountService accountService;

    public OnboardingService(
            ConversationStateRepository stateRepository,
            UserRepository userRepository,
            AccountRepository accountRepository,
            UserService userService,
            AccountService accountService) {
        this.stateRepository = stateRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.userService = userService;
        this.accountService = accountService;
    }

    @Transactional
    public String start(String phone) {
        // recomeçar no meio do cadastro: apaga o que já tinha sido criado
        Optional<ConversationState> existing = stateRepository.findByPhone(phone);
        existing.ifPresent(this::cleanup);

        if (userRepository.findByPhone(phone).isPresent()) {
            return "Você já tem cadastro. Envie !ajuda para ver os comandos.";
        }

        ConversationState state = existing.orElseGet(ConversationState::new);
        state.setPhone(phone);
        state.setStep(OnboardingStep.NAME);
        state.setUserId(null);
        state.setDraftName(null);
        state.setDraftClosingDay(null);
        stateRepository.save(state);
        return "Vamos criar seu cadastro! (envie !cancelar para sair)\nQual é o seu nome completo?";
    }

    // !cancelar no meio do cadastro: apaga o usuário e as contas criadas até aqui
    @Transactional
    public void cancel(ConversationState state) {
        cleanup(state);
        stateRepository.delete(state);
    }

    @Transactional
    public String answer(ConversationState state, String text) {
        return switch (state.getStep()) {
            case NAME -> onName(state, text);
            case BANK_NAME -> onBankName(state, text);
            case BANK_BALANCE -> onBankBalance(state, text);
            case MORE_BANKS -> onMoreBanks(state, text);
            case RESERVE -> onReserve(state, text);
            case HAS_CARD -> onHasCard(state, text);
            case CARD_NAME -> onCardName(state, text);
            case CLOSING_DAY -> onClosingDay(state, text);
            case DUE_DAY -> onDueDay(state, text);
            case MORE_CARDS -> onMoreCards(state, text);
        };
    }

    private String onName(ConversationState state, String text) {
        if (text.trim().length() < 3) {
            return "Não entendi o nome. Pode enviar seu nome completo?";
        }
        User user = userService.create(text.trim(), state.getPhone());
        state.setUserId(user.getId());
        state.setStep(OnboardingStep.BANK_NAME);
        stateRepository.save(state);
        return "Prazer, " + text.trim().split("\\s+")[0] + "! Qual é o nome do seu banco? (ex.: Nubank, Banco do Brasil)";
    }

    private String onBankName(ConversationState state, String text) {
        String name = text.trim();
        if (name.length() < 2) {
            return "Não entendi o nome do banco. Pode enviar de novo?";
        }
        if (accountService.nameTaken(state.getUserId(), AccountType.CHECKING, name)) {
            return "Esse banco já foi cadastrado. Envie o nome de outro banco.";
        }
        state.setDraftName(name);
        state.setStep(OnboardingStep.BANK_BALANCE);
        stateRepository.save(state);
        return "Quanto você tem hoje no " + name + "? (ex.: 1500,50)";
    }

    private String onBankBalance(ConversationState state, String text) {
        BigDecimal value = TextUtils.parseMoney(text);
        if (value == null) {
            return "Não entendi o valor. Envie só o número, por exemplo 1500,50";
        }
        accountService.create(new CreateAccountRequest(
                state.getUserId(), state.getDraftName(), AccountType.CHECKING, value, null, null, null));
        state.setDraftName(null);
        state.setStep(OnboardingStep.MORE_BANKS);
        stateRepository.save(state);
        return "Anotado! Você usa mais algum banco? (sim ou não)";
    }

    private String onMoreBanks(ConversationState state, String text) {
        if (TextUtils.isYes(text)) {
            state.setStep(OnboardingStep.BANK_NAME);
            stateRepository.save(state);
            return "Qual é o nome do outro banco?";
        }
        if (TextUtils.isNo(text)) {
            state.setStep(OnboardingStep.RESERVE);
            stateRepository.save(state);
            return "E quanto você tem de reserva de emergência? Envie 0 se não tiver.";
        }
        return YES_OR_NO;
    }

    private String onReserve(ConversationState state, String text) {
        BigDecimal value = TextUtils.parseMoney(text);
        if (value == null || value.signum() < 0) {
            return "Não entendi o valor. Envie o valor da reserva, ou 0 se não tiver.";
        }
        accountService.create(new CreateAccountRequest(
                state.getUserId(), "Reserva de emergência", AccountType.EMERGENCY_FUND, value, null, null, null));
        state.setStep(OnboardingStep.HAS_CARD);
        stateRepository.save(state);
        return "Você tem cartão de crédito? (sim ou não)";
    }

    private String onHasCard(ConversationState state, String text) {
        if (TextUtils.isYes(text)) {
            state.setStep(OnboardingStep.CARD_NAME);
            stateRepository.save(state);
            return "Qual é o nome do cartão? (ex.: Nubank, Inter)";
        }
        if (TextUtils.isNo(text)) {
            return finish(state);
        }
        return YES_OR_NO;
    }

    private String onCardName(ConversationState state, String text) {
        String name = text.trim();
        if (name.length() < 2) {
            return "Não entendi o nome do cartão. Pode enviar de novo?";
        }
        if (accountService.nameTaken(state.getUserId(), AccountType.CREDIT_CARD, name)) {
            return "Esse cartão já foi cadastrado. Envie o nome de outro cartão.";
        }
        state.setDraftName(name);
        state.setStep(OnboardingStep.CLOSING_DAY);
        stateRepository.save(state);
        return "Em que dia do mês a fatura do " + name + " fecha? (1 a 31)";
    }

    private String onClosingDay(ConversationState state, String text) {
        Integer day = TextUtils.parseDay(text);
        if (day == null) {
            return "Envie um número de 1 a 31.";
        }
        state.setDraftClosingDay(day);
        state.setStep(OnboardingStep.DUE_DAY);
        stateRepository.save(state);
        return "E em que dia essa fatura vence? (1 a 31)";
    }

    private String onDueDay(ConversationState state, String text) {
        Integer day = TextUtils.parseDay(text);
        if (day == null) {
            return "Envie um número de 1 a 31.";
        }
        accountService.create(new CreateAccountRequest(
                state.getUserId(), state.getDraftName(), AccountType.CREDIT_CARD,
                BigDecimal.ZERO, null, state.getDraftClosingDay(), day));
        state.setDraftName(null);
        state.setDraftClosingDay(null);
        state.setStep(OnboardingStep.MORE_CARDS);
        stateRepository.save(state);
        return "Anotado! Você tem mais algum cartão? (sim ou não)";
    }

    private String onMoreCards(ConversationState state, String text) {
        if (TextUtils.isYes(text)) {
            state.setStep(OnboardingStep.CARD_NAME);
            stateRepository.save(state);
            return "Qual é o nome do outro cartão?";
        }
        if (TextUtils.isNo(text)) {
            return finish(state);
        }
        return YES_OR_NO;
    }

    private String finish(ConversationState state) {
        Long userId = state.getUserId();
        User user = userService.getById(userId);

        StringBuilder summary = new StringBuilder("Cadastro concluído, ")
                .append(user.getName().split("\\s+")[0]).append("!\n\n*Bancos*\n");
        for (Account bank : accountService.banks(userId)) {
            summary.append("- ").append(bank.getName()).append(": ")
                    .append(TextUtils.formatMoney(bank.getInitialBalance())).append("\n");
        }

        for (Account reserve : accountService.ofType(userId, AccountType.EMERGENCY_FUND)) {
            summary.append("\n*Reserva*\n- ").append(TextUtils.formatMoney(reserve.getInitialBalance())).append("\n");
        }

        var cards = accountService.cards(userId);
        if (!cards.isEmpty()) {
            summary.append("\n*Cartões*\n");
            for (Account card : cards) {
                summary.append("- ").append(card.getName())
                        .append(" (fecha dia ").append(card.getClosingDay())
                        .append(", vence dia ").append(card.getDueDay()).append(")\n");
            }
        }

        stateRepository.delete(state);
        summary.append("\nDica: defina uma meta de gasto por categoria com !meta, por exemplo: !meta alimentação 300");
        summary.append("\nEnvie !ajuda para ver os comandos.");
        return summary.toString();
    }

    // apaga o usuário e as contas de um cadastro que não terminou
    private void cleanup(ConversationState state) {
        if (state.getUserId() != null) {
            accountRepository.deleteAll(accountRepository.findByUserId(state.getUserId()));
            userRepository.deleteById(state.getUserId());
        }
    }
}