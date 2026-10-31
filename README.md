# wpp_financas

Bot de WhatsApp para controle de finanças pessoais. Você conversa com um contato, registra o que gastou e ganhou, e consulta saldo, fatura do cartão e relatórios por categoria. No último dia de cada mês, o relatório chega sozinho.

Exemplo ilustrativo (o texto exato das respostas pode variar):

```
Você: !gasto 25,90
Bot:  Qual a categoria? (lista numerada)
Você: 1
Bot:  Confirmação do gasto e saldo atualizado.
```

## O que ele faz

- **Cadastro guiado** (`!novousuario`): nome, bancos com saldo, reserva de emergência e cartões de crédito.
- **Gastos e ganhos**, à vista, no cartão ou parcelados (de 2x a 24x), sempre perguntando a categoria.
- **Vários bancos e vários cartões**, com saldo e fatura por conta e o total geral.
- **Reserva de emergência** separada do saldo, com `!guardar` e `!resgatar`.
- **Fatura do cartão** calculada pelas datas de fechamento e vencimento, com limite disponível e `!pagarfatura`.
- **Metas por categoria** com alerta em 80% e 100% do limite mensal.
- **Relatórios** mensais e anuais por categoria, e um relatório mensal automático.
- **Ajuda dentro do chat**: `!ajuda` e `!ajuda <comando>`.

## Comandos

| Comando | Exemplo | O que faz |
| --- | --- | --- |
| `!novousuario` | | Cadastro guiado (`!cancelar` desiste) |
| `!gasto` | `!gasto 25,90` · `!gasto 120 cartão` · `!gasto 600 cartão 3x` · `!gasto 40 #transporte` | Registra um gasto |
| `!ganho` (ou `!receita`) | `!ganho 1500 salário` | Registra dinheiro que entrou |
| `!saldo` | `!saldo` · `!saldo geral` · `!saldo nubank` | Saldo dos bancos |
| `!reserva` | | Saldo da reserva de emergência |
| `!guardar` / `!resgatar` | `!guardar 500` | Move dinheiro entre banco e reserva |
| `!fatura` | `!fatura` · `!fatura geral` · `!fatura inter` | Fatura aberta e fechada dos cartões |
| `!pagarfatura` | `!pagarfatura` · `!pagarfatura 300` | Registra o pagamento da fatura fechada |
| `!extrato` | `!extrato 20` · `!extrato nubank` | Últimos lançamentos |
| `!desfazer` | | Apaga o último lançamento, com confirmação |
| `!meta` / `!metas` | `!meta alimentação 300` | Define e mostra metas mensais |
| `!relatorio` | `!relatorio` · `!relatorio 9/2026` · `!relatorio ano` | Gastos por categoria |
| `!contas` | | Bancos, cartões e reserva |
| `!novobanco` | `!novobanco Nubank 1500,50` | Cadastra outro banco |
| `!novocartao` | `!novocartao Inter 10 17` | Cadastra outro cartão (fechamento e vencimento) |
| `!editar` | `!editar fechamento 12 inter` | Corrige nome, dias da fatura ou limite |
| `!ajuda` | `!ajuda gasto` | Lista os comandos ou explica um |

## Como funciona

Três serviços, cada um num container Docker:

```
WhatsApp ⇄ bot (Node + whatsapp-web.js) ⇄ backend (Spring Boot) ⇄ PostgreSQL
```

O bot é "burro": só repassa o texto de cada mensagem para `POST /messages` e devolve a resposta. Toda a lógica fica no Spring. Conversas de vários passos (cadastro, escolha de categoria, confirmações) ficam salvas no banco, e o relatório mensal é entregue por uma caixa de saída que o bot consulta a cada 15 segundos.

## Tecnologias

| Camada | Tecnologia |
| --- | --- |
| Backend | Java 25, Spring Boot 4.1.1, Spring Data JPA, Lombok, Maven |
| Banco | PostgreSQL 18 |
| Bot | Node 24, TypeScript (executado direto), whatsapp-web.js |
| Infra | Docker Compose |
| Testes | JUnit 5, Mockito |

## Como rodar com Docker

Requisitos: Docker Desktop e um chip com WhatsApp para ser o número do bot.

1. Crie o arquivo `.env` na raiz, copiando o `.env.example`, e troque a senha (use só letras e números):

   ```
   DB_NAME=wpp_financas
   DB_USER=postgres
   DB_PASSWORD=sua_senha_aqui
   ```

2. Suba tudo, na pasta raiz do projeto:

   ```bash
   docker compose up -d --build
   ```

3. Veja o QR code do bot e escaneie pelo WhatsApp do chip (Aparelhos conectados):

   ```bash
   docker compose logs -f bot
   ```

4. Quando aparecer `Client is ready!`, mande `!ajuda` para o número do bot.

Comandos úteis:

```bash
docker compose ps                # estado dos serviços
docker compose logs -f backend   # logs do Spring
docker compose down              # para tudo (os dados continuam)
```

> Não use `docker compose down -v`: o `-v` apaga os volumes, ou seja, o banco e a sessão do WhatsApp.

A senha do banco é gravada no volume na primeira subida. Para trocá-la depois, é preciso recriar o volume.

## Como rodar sem Docker (desenvolvimento)

Requisitos: JDK 25, PostgreSQL 18 com um banco chamado `wpp_financas`, Node 24.

```bash
# backend (usa localhost:5432, usuário e senha "postgres" se não houver variáveis)
./mvnw spring-boot:run

# bot, em outro terminal
cd whatsapp_ts
npm install
npm run dev
```

O arquivo `main.http` tem requisições prontas para testar a API pelo IntelliJ.

## Configuração

| Variável | Quem lê | Padrão |
| --- | --- | --- |
| `DB_HOST`, `DB_PORT` | Spring | `localhost`, `5432` |
| `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Spring e Postgres | `wpp_financas`, `postgres`, `postgres` |
| `API_URL` | bot | `http://localhost:8080` |
| `WWEBJS_AUTH_PATH` | bot | `./.wwebjs_auth` |
| `PUPPETEER_EXECUTABLE_PATH` | bot | Chromium do Puppeteer |

## Testes

```bash
./mvnw test
```

São 31 testes: leitura do `!gasto`, ciclo da fatura, saldos, transferências, parcelas e o agendamento do relatório. O teste `contextLoads` precisa de um banco acessível.

Para testar o relatório mensal sem esperar o fim do mês, com tudo rodando:

```http
POST http://localhost:8080/outbox/monthly-reports/run
```

O relatório chega no WhatsApp em até 15 segundos. Esse endpoint é só para teste.

## Estrutura

```
src/main/java/com/code/wpp_financas/
├── config/         categorias padrão
├── controllers/    endpoints HTTP
├── dtos/           entrada e saída da API
├── models/         entidades JPA e enums
├── repositories/   Spring Data (JPQL)
├── services/       regras de negócio e comandos
└── utils/          texto, dinheiro, ciclo da fatura
whatsapp_ts/        bot (Node + TypeScript)
Dockerfile          build do Spring
docker-compose.yml  db + backend + bot
```

A documentação completa (modelo de dados, regras de negócio, fluxos de conversa, decisões e armadilhas) está no documento do projeto.

## Problemas comuns

- **O bot não responde.** Confira se os serviços estão de pé (`docker compose ps`), se a sessão não foi desconectada pelo celular (nesse caso apague o volume `wwebjs_auth` e escaneie o QR de novo) e os logs do bot.
- **`MalformedInputException` ao construir a imagem.** Algum arquivo do projeto não está em UTF-8. Salve em UTF-8 e rode de novo.
- **`docker` não encontrado no WSL.** Ligue a integração com o WSL nas configurações do Docker Desktop.

## Limitações

- Não há autenticação: a API só é publicada em `127.0.0.1` e não deve ser aberta para a internet.
- Usa o WhatsApp Web não oficial (`whatsapp-web.js`), sujeito a quebras e a bloqueio do número. Use um chip dedicado.
- Pagar a fatura por fora do bot não é detectado; o limite do cartão não desconta parcelas futuras.
- As categorias são fixas e iguais para todos.
