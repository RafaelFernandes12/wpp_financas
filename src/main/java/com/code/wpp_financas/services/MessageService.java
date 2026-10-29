package com.code.wpp_financas.services;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.code.wpp_financas.models.ConversationState;
import com.code.wpp_financas.models.PendingExpense;
import com.code.wpp_financas.models.TransactionType;
import com.code.wpp_financas.models.User;
import com.code.wpp_financas.repositories.ConversationStateRepository;
import com.code.wpp_financas.repositories.UserRepository;
import com.code.wpp_financas.utils.TextUtils;

@Service
public class MessageService {

    private static final String NOT_REGISTERED =  "Você ainda não tem cadastro.\nEnvie !novousuario para começar.";

    private static final String GENERAL_HELP = """
      Olá! Eu te ajudo a controlar suas finanças.

      *Comandos disponíveis*
      !novousuario - criar seu cadastro
      !gasto - registrar um gasto
      !receita - registrar dinheiro que entrou
      !saldo - quanto você pode gastar agora
      !reserva - saldo da sua reserva de emergência
      !editar - corrigir seus dados (nome, dias da fatura, limite)
      !ajuda - esta lista

      *Em breve*
      !fatura e !relatorio

      Para ver os detalhes de um comando, envie por exemplo:\n!ajuda gasto""";

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
          Mostra quanto você tem guardado na reserva de emergência. Ela fica separada do !saldo de propósito, para você não gastar sem querer o que é reserva.""",
            "gasto", """
          *!gasto*
          Registra um gasto. Formato: !gasto valor

          Exemplos:
          !gasto 25,90
          !gasto 120 cartão

          Depois de enviar, eu pergunto a categoria e mostro a lista numerada. É só responder com o número (ou o nome).

          - Por padrão o gasto sai da conta corrente e diminui o seu !saldo.
          - Com a palavra "cartão" no final, o gasto vai para o cartão de crédito e entra na fatura, sem mexer no !saldo.
          - Se quiser, escreva uma descrição depois do valor para se lembrar do gasto: !gasto 25,90 almoço
          - Quer pular a pergunta? Escreva a categoria junto, com #: !gasto 40 #transporte
          - Para desistir antes de escolher a categoria, envie !cancelar.""",
            "receita", """
          *!receita*
          Registra dinheiro que entrou na conta corrente. Formato: !receita valor

          Exemplo: !receita 1500 salário

          Aumenta o seu !saldo. Não vale para o cartão de crédito.""",
            "editar", """
          *!editar*
          Corrige dados do cadastro. Formato: !editar campo valor

          Exemplos:
          !editar nome Maria Silva
          !editar fechamento 12 (dia em que a fatura fecha)
          !editar vencimento 19 (dia em que a fatura vence)
          !editar limite 3000 (limite do cartão)""");

    private final UserRepository userRepository;
    private final ConversationStateRepository stateRepository;
    private final OnboardingService onboardingService;
    private final BalanceService balanceService;
    private final TransactionCommandService transactionCommandService;
    private final ProfileCommandService profileCommandService;

    public MessageService(
            UserRepository userRepository,
            ConversationStateRepository stateRepository,
            OnboardingService onboardingService,
            BalanceService balanceService,
            TransactionCommandService transactionCommandService,
            ProfileCommandService profileCommandService) {
        this.userRepository = userRepository;
        this.stateRepository = stateRepository;
        this.onboardingService = onboardingService;
        this.balanceService = balanceService;
        this.transactionCommandService = transactionCommandService;
        this.profileCommandService = profileCommandService;
    }

    // devolve o texto da resposta; texto vazio significa "não responder nada"
    public String handle(String phone, String text) {
        try {
            return route(phone, text);
        } catch (ResponseStatusException e) {
            // erro de validação vira a própria resposta no WhatsApp
            return e.getReason() != null ? e.getReason() : "Não consegui concluir essa ação.";
        }
    }

    private String route(String phone, String text) {
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

        // gasto esperando a escolha da categoria
        Optional<PendingExpense> pending = transactionCommandService.findPending(phone);
        if (pending.isPresent()) {
            if (TextUtils.normalize(body).equals("!cancelar")) {
                transactionCommandService.discard(pending.get());
                return "Gasto cancelado.";
            }
            if (!body.startsWith("!")) {
                return withUser(phone, user ->
                        transactionCommandService.answerPending(user, pending.get(), body));
            }
            // outro comando no meio: descarta o gasto pendente e segue o fluxo normal
            transactionCommandService.discard(pending.get());
        }

        // conversa solta, sem comando
        if (!body.startsWith("!")) {
            return userRepository.findByPhone(phone).isPresent() ? "" : "Olá! Eu sou um bot de controle de finanças. Envie !novousuario para criar seu cadastro ou !ajuda para ver o que eu faço.";
        }

        String[] parts = body.split("\\s+", 2);
        String command = TextUtils.normalize(parts[0]);
        String args = parts.length > 1 ? parts[1].trim() : "";

        return switch (command) {
            case "!ajuda" -> help(args);
            case "!novousuario" -> onboardingService.start(phone);
            case "!saldo" -> withUser(phone, user ->
                    "Saldo disponível: " + TextUtils.formatMoney(balanceService.spendableBalance(user.getId())));
            case "!reserva" -> withUser(phone, user ->
                    "Reserva de emergência: " + TextUtils.formatMoney(balanceService.emergencyFund(user.getId())));
            case "!gasto" -> withUser(phone, user ->
                    transactionCommandService.register(user, TransactionType.EXPENSE, args));
            case "!receita" -> withUser(phone, user ->
                    transactionCommandService.register(user, TransactionType.INCOME, args));
            case "!editar" -> withUser(phone, user -> profileCommandService.edit(user, args));
            default -> "Não conheço esse comando. Envie !ajuda para ver a lista.";
        };
    }

    private String withUser(String phone, Function<User, String> action) {
        return userRepository.findByPhone(phone).map(action).orElse(NOT_REGISTERED);
    }

    private String help(String topic) {
        String key = TextUtils.normalize(topic).replace("!", "");
        if (key.isEmpty()) {
            return GENERAL_HELP;
        }
        return HELP_TOPICS.getOrDefault(
                key, "Não encontrei ajuda para \"" + topic + "\". Envie !ajuda para ver a lista.");
    }
}