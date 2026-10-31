package com.code.wpp_financas.services;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.code.wpp_financas.models.ConversationState;
import com.code.wpp_financas.models.PendingAction;
import com.code.wpp_financas.models.PendingTransaction;
import com.code.wpp_financas.models.TransactionType;
import com.code.wpp_financas.models.User;
import com.code.wpp_financas.repositories.ConversationStateRepository;
import com.code.wpp_financas.repositories.UserRepository;
import com.code.wpp_financas.utils.TextUtils;

@Service
public class MessageService {

    private static final String NOT_REGISTERED =
            "Você ainda não tem cadastro. Envie !novousuario para começar.";

    private static final String GENERAL_HELP = """
      Olá! Eu te ajudo a controlar suas finanças.

      *Comandos disponíveis*
      !novousuario - criar seu cadastro
      !gasto - registrar um gasto (à vista ou parcelado no cartão)
      !ganho - registrar dinheiro que entrou
      !saldo - quanto você pode gastar agora
      !reserva - saldo da sua reserva de emergência
      !guardar - mover dinheiro de um banco para a reserva
      !resgatar - mover dinheiro da reserva para um banco
      !fatura - quanto você deve no cartão e quando fecha
      !pagarfatura - registrar o pagamento da fatura
      !extrato - seus últimos lançamentos
      !desfazer - apagar o último lançamento
      !meta - definir quanto gastar por mês em cada categoria
      !metas - ver como estão as suas metas
      !relatorio - gastos do mês (ou do ano) por categoria
      !contas - seus bancos, cartões e reserva
      !novobanco - cadastrar outro banco
      !novocartao - cadastrar outro cartão
      !editar - corrigir seus dados (nome, dias da fatura, limite)
      !ajuda - esta lista

      Para ver os detalhes de um comando, envie por exemplo: !ajuda gasto""";

    private static final Map<String, String> HELP_TOPICS = Map.ofEntries(
            Map.entry("novousuario", """
          *!novousuario*
          Cria o seu cadastro. Eu pergunto o seu nome, o nome de cada banco que você usa e quanto tem em cada um, quanto tem de reserva de emergência e os seus cartões de crédito (com o dia em que a fatura fecha e o dia em que vence).
          Para desistir no meio, envie !cancelar."""),
            Map.entry("saldo", """
          *!saldo*
          Mostra quanto você tem disponível para gastar: o saldo dos seus bancos, mais os ganhos, menos os gastos.
          Se você tem mais de um banco, mostro cada um e o total.

          Exemplos:
          !saldo
          !saldo geral (só o total)
          !saldo nubank (só um banco)

          A reserva de emergência e os gastos no cartão não entram nessa conta."""),
            Map.entry("reserva", """
          *!reserva*
          Mostra quanto você tem guardado na reserva de emergência. Ela fica separada do !saldo de propósito, para você não gastar sem querer o que é reserva."""),
            Map.entry("gasto", """
          *!gasto*
          Registra um gasto. Formato: !gasto valor

          Exemplos:
          !gasto 25,90
          !gasto 120 cartão
          !gasto 600 cartão 3x

          Depois de enviar, eu pergunto a categoria e mostro a lista numerada. É só responder com o número (ou o nome).

          - Por padrão o gasto sai de um banco e diminui o seu !saldo. Se você tem mais de um banco, eu pergunto de qual.
          - Com a palavra "cartão" no final, o gasto vai para um cartão de crédito e entra na fatura, sem mexer no !saldo. Se você tem mais de um cartão, eu pergunto qual.
          - Parcelado: coloque 3x, 6x etc. junto com "cartão" (de 2 a 24 parcelas). Crio uma parcela por mês, cada uma na fatura certa. O !desfazer apaga a compra inteira.
          - Se quiser, escreva uma descrição depois do valor para se lembrar do gasto: !gasto 25,90 almoço
          - Quer pular a pergunta da categoria? Escreva a categoria junto, com #: !gasto 40 #transporte
          - Se a categoria tem meta (!meta), eu aviso quando você chegar perto do limite.
          - Para desistir antes de terminar, envie !cancelar."""),
            Map.entry("ganho", """
          *!ganho*
          Registra dinheiro que entrou em um banco. Formato: !ganho valor

          Exemplo: !ganho 1500 salário

          Se você tem mais de um banco, eu pergunto em qual entrou. Aumenta o seu !saldo. Não vale para cartão de crédito.
          (!receita também funciona.)"""),
            Map.entry("editar", """
          *!editar*
          Corrige dados do cadastro. Formato: !editar campo valor

          Exemplos:
          !editar nome Maria Silva
          !editar fechamento 12 inter (dia em que a fatura fecha)
          !editar vencimento 19 inter (dia em que a fatura vence)
          !editar limite 3000 inter (limite do cartão)

          O nome do cartão no final só é preciso se você tem mais de um."""),
            Map.entry("fatura", """
          *!fatura*
          Mostra quanto você deve no cartão de crédito. Se você tem mais de um cartão, mostro cada um e o total.

          Exemplos:
          !fatura
          !fatura geral (uma linha por cartão e o total)
          !fatura inter (só um cartão)

          - Fatura aberta: o que você já gastou e ainda vai fechar, com as datas de fechamento e de vencimento.
          - Fatura fechada: aparece até o dia do vencimento, mostrando só o que falta pagar. Para registrar o pagamento, use !pagarfatura.
          - Compras feitas até o dia do fechamento entram na fatura que fecha; as de depois vão para a seguinte.
          - Se você cadastrou um limite, mostro também o limite disponível (sem contar as parcelas futuras).
          - Errou o dia de fechamento ou de vencimento? Use !editar."""),
            Map.entry("relatorio", """
          *!relatorio*
          Mostra os seus gastos por categoria, com a porcentagem de cada uma, o total de gastos, os ganhos e o resultado.

          Exemplos:
          !relatorio (mês atual)
          !relatorio 9 (setembro deste ano)
          !relatorio 9/2026
          !relatorio ano (o ano atual, com o resumo mês a mês)
          !relatorio ano 2025

          No relatório do mês, as categorias com meta mostram o valor da meta.
          Os gastos no cartão entram pela data da compra (compras parceladas, pela data de cada parcela). Transferências (!guardar, !resgatar e !pagarfatura) não entram no relatório."""),
            Map.entry("contas", """
          *!contas*
          Lista os seus bancos (com o saldo de cada um), os seus cartões (com o dia de fechamento e de vencimento) e a sua reserva de emergência."""),
            Map.entry("novobanco", """
          *!novobanco*
          Cadastra outro banco. Formato: !novobanco nome saldo

          Exemplo: !novobanco Nubank 1500,50

          O saldo é quanto você tem nele hoje."""),
            Map.entry("novocartao", """
          *!novocartao*
          Cadastra outro cartão de crédito. Formato: !novocartao nome fechamento vencimento

          Exemplo: !novocartao Inter 10 17

          Para definir o limite depois: !editar limite 3000 inter"""),
            Map.entry("guardar", """
          *!guardar*
          Move dinheiro de um banco para a reserva de emergência. Formato: !guardar valor

          Exemplo: !guardar 500

          Se você tem mais de um banco, eu pergunto de qual sai. Isso não é um gasto: diminui o !saldo, aumenta a !reserva e não entra no !relatorio.
          Se o saldo do banco ficar negativo, eu aviso, mas registro mesmo assim."""),
            Map.entry("resgatar", """
          *!resgatar*
          Move dinheiro da reserva de emergência para um banco. Formato: !resgatar valor

          Exemplo: !resgatar 200

          Se você tem mais de um banco, eu pergunto em qual entra. Diminui a !reserva e aumenta o !saldo, sem entrar no !relatorio."""),
            Map.entry("pagarfatura", """
          *!pagarfatura*
          Registra o pagamento da fatura fechada do cartão, tirando o dinheiro de um banco.

          Exemplos:
          !pagarfatura (paga a fatura fechada inteira)
          !pagarfatura 300 (pagamento parcial)
          !pagarfatura inter (escolhe o cartão)

          Se você tem mais de um cartão ou mais de um banco, eu pergunto qual.
          O pagamento diminui o !saldo do banco e o que falta na !fatura, e não entra no !relatorio (o gasto já foi contado quando você comprou no cartão).
          Só vale para fatura já fechada, e o valor não pode passar do que falta pagar."""),
            Map.entry("desfazer", """
          *!desfazer*
          Apaga o último lançamento (gasto, ganho, transferência ou compra parcelada). Eu mostro o que vai ser apagado e peço sim ou não antes.
          Nas transferências (!guardar, !resgatar e !pagarfatura), as duas pontas são desfeitas juntas. Nas compras parceladas, todas as parcelas."""),
            Map.entry("extrato", """
          *!extrato*
          Lista os seus últimos lançamentos.

          Exemplos:
          !extrato (últimos 10)
          !extrato 20 (até 30)
          !extrato nubank (só dessa conta; se houver banco e cartão com o mesmo nome, mostro os dois)

          Cada linha mostra a data, o valor (− saiu, + entrou), a categoria e a conta. Parcelas futuras aparecem com a data delas."""),
            Map.entry("meta", """
          *!meta*
          Define quanto você quer gastar, no máximo, por mês em uma categoria.

          Exemplos:
          !meta alimentação 300
          !meta alimentação 0 (remove a meta)
          !metas (mostra quanto você já gastou de cada meta neste mês)

          Depois de cada gasto nessa categoria, eu aviso quando você chegar a 80% da meta, quando bater o limite e quando passar dele.
          A conta considera todos os seus gastos do mês (bancos e cartão), pela data da compra."""));

    private final UserRepository userRepository;
    private final ConversationStateRepository stateRepository;
    private final OnboardingService onboardingService;
    private final BalanceService balanceService;
    private final BalanceCommandService balanceCommandService;
    private final TransactionCommandService transactionCommandService;
    private final ProfileCommandService profileCommandService;
    private final AccountCommandService accountCommandService;
    private final ActionCommandService actionCommandService;
    private final StatementService statementService;
    private final InvoiceService invoiceService;
    private final ReportService reportService;
    private final BudgetService budgetService;

    public MessageService(
            UserRepository userRepository,
            ConversationStateRepository stateRepository,
            OnboardingService onboardingService,
            BalanceService balanceService,
            BalanceCommandService balanceCommandService,
            TransactionCommandService transactionCommandService,
            ProfileCommandService profileCommandService,
            AccountCommandService accountCommandService,
            ActionCommandService actionCommandService,
            StatementService statementService,
            InvoiceService invoiceService,
            ReportService reportService,
            BudgetService budgetService) {
        this.userRepository = userRepository;
        this.stateRepository = stateRepository;
        this.onboardingService = onboardingService;
        this.balanceService = balanceService;
        this.balanceCommandService = balanceCommandService;
        this.transactionCommandService = transactionCommandService;
        this.profileCommandService = profileCommandService;
        this.accountCommandService = accountCommandService;
        this.actionCommandService = actionCommandService;
        this.statementService = statementService;
        this.invoiceService = invoiceService;
        this.reportService = reportService;
        this.budgetService = budgetService;
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
                onboardingService.cancel(state.get());
                return "Cadastro cancelado. Envie !novousuario para recomeçar.";
            }
            if (command.equals("!novousuario")) {
                return onboardingService.start(phone);
            }
            return onboardingService.answer(state.get(), body);
        }

        // gasto, ganho ou ação (guardar, pagar fatura, desfazer...) esperando resposta
        Optional<PendingTransaction> pendingTransaction = transactionCommandService.findPending(phone);
        Optional<PendingAction> pendingAction = actionCommandService.findPending(phone);
        if (pendingTransaction.isPresent() || pendingAction.isPresent()) {
            if (TextUtils.normalize(body).equals("!cancelar")) {
                pendingTransaction.ifPresent(transactionCommandService::discard);
                pendingAction.ifPresent(actionCommandService::discard);
                return "Cancelado.";
            }
            if (!body.startsWith("!")) {
                if (pendingTransaction.isPresent()) {
                    return withUser(phone, user ->
                            transactionCommandService.answerPending(user, pendingTransaction.get(), body));
                }
                return withUser(phone, user ->
                        actionCommandService.answer(user, pendingAction.get(), body));
            }
            // outro comando no meio: descarta o que estava pendente e segue o fluxo normal
            pendingTransaction.ifPresent(transactionCommandService::discard);
            pendingAction.ifPresent(actionCommandService::discard);
        }

        // conversa solta, sem comando: só responde a quem ainda não tem cadastro
        if (!body.startsWith("!")) {
            return userRepository.findByPhone(phone).isPresent()
                    ? ""
                    : "Olá! Eu sou um bot de controle de finanças. Envie !novousuario para criar seu cadastro ou !ajuda para ver o que eu faço.";
        }

        String[] parts = body.split("\\s+", 2);
        String command = TextUtils.normalize(parts[0]);
        String args = parts.length > 1 ? parts[1].trim() : "";

        return switch (command) {
            case "!ajuda" -> help(args);
            case "!novousuario" -> onboardingService.start(phone);
            case "!saldo" -> withUser(phone, user -> balanceCommandService.balance(user, args));
            case "!reserva" -> withUser(phone, user ->
                    "Reserva de emergência: " + TextUtils.formatMoney(balanceService.emergencyFund(user.getId())));
            case "!gasto" -> withUser(phone, user ->
                    transactionCommandService.register(user, TransactionType.EXPENSE, args));
            case "!ganho", "!receita" -> withUser(phone, user ->
                    transactionCommandService.register(user, TransactionType.INCOME, args));
            case "!guardar" -> withUser(phone, user -> actionCommandService.saveToReserve(user, args));
            case "!resgatar" -> withUser(phone, user -> actionCommandService.withdrawFromReserve(user, args));
            case "!pagarfatura" -> withUser(phone, user -> actionCommandService.payInvoice(user, args));
            case "!desfazer" -> withUser(phone, actionCommandService::undo);
            case "!extrato" -> withUser(phone, user -> statementService.statement(user, args));
            case "!meta" -> withUser(phone, user -> budgetService.setGoal(user, args));
            case "!metas" -> withUser(phone, budgetService::list);
            case "!editar" -> withUser(phone, user -> profileCommandService.edit(user, args));
            case "!fatura" -> withUser(phone, user -> invoiceService.invoice(user, args));
            case "!relatorio" -> withUser(phone, user -> reportService.report(user, args));
            case "!contas" -> withUser(phone, accountCommandService::list);
            case "!novobanco" -> withUser(phone, user -> accountCommandService.newBank(user, args));
            case "!novocartao" -> withUser(phone, user -> accountCommandService.newCard(user, args));
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
        if (key.equals("receita")) {
            key = "ganho";
        }
        if (key.equals("metas")) {
            key = "meta";
        }
        return HELP_TOPICS.getOrDefault(
                key, "Não encontrei ajuda para \"" + topic + "\". Envie !ajuda para ver a lista.");
    }
}