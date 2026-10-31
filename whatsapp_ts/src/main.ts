import qrcode from "qrcode-terminal";
import { client } from "./config/wpp-client.ts";
import { fetchPending, markSent, sendToSpring } from "./services/spring-api.ts";

let ready = false;
let delivering = false;

// ids das últimas mensagens já tratadas (evita processar a mesma mensagem duas vezes)
const seen = new Set<string>();

// fila por conversa: as mensagens de uma mesma pessoa são tratadas uma por vez, na ordem
const chains = new Map<string, Promise<void>>();

function enqueue(chatId: string, task: () => Promise<void>) {
  const previous = chains.get(chatId) ?? Promise.resolve();
  const next = previous.then(task).catch(() => {});
  chains.set(chatId, next);
  next.finally(() => {
    if (chains.get(chatId) === next) {
      chains.delete(chatId);
    }
  });
}

client.on("qr", (qr) => {
  qrcode.generate(qr, { small: true });
});

client.on("authenticated", () => console.log("Autenticado"));
client.on("auth_failure", (msg) => console.error("Falha de autenticação:", msg));
client.on("disconnected", (reason) => {
  ready = false;
  console.error("Desconectado:", reason);
});

client.on("ready", () => {
  ready = true;
  console.log("Client is ready!");
});

client.on("message", (msg) => {
  if (msg.from.endsWith("@g.us") || msg.from === "status@broadcast") return;

  // id da mensagem; se a biblioteca não fornecer, usa conversa + horário + texto
  const raw = msg.id as { _serialized?: string; id?: string } | undefined;
  const id = raw?._serialized ?? raw?.id ?? `${msg.from}|${msg.timestamp}|${msg.body}`;

  if (seen.has(id)) {
    console.log(`Ignorando mensagem duplicada: ${id}`);
    return;
  }
  seen.add(id);
  if (seen.size > 500) {
    seen.delete(seen.values().next().value!);
  }
  console.log(`Mensagem recebida: ${id}`);

  enqueue(msg.from, async () => {
    try {
      const contact = await msg.getContact();
      const reply = await sendToSpring(contact.number, msg.body);
      if (reply) {
        await client.sendMessage(msg.from, reply);
      }
    } catch (error) {
      console.error("Erro ao tratar mensagem:", error);
      try {
        await client.sendMessage(msg.from, "Ops, deu um erro. Tente de novo em instantes.");
      } catch {
        // sem o que fazer
      }
    }
  });
});

// ---- entrega das mensagens da caixa de saída (relatório mensal etc.) ----

async function sendTo(phone: string, text: string) {
  try {
    await client.sendMessage(`${phone}@lid`, text);
  } catch {
    await client.sendMessage(`${phone}@c.us`, text);
  }
}

async function deliverOutbox() {
  if (!ready || delivering) return;
  delivering = true;
  try {
    const pending = await fetchPending();
    for (const message of pending) {
      try {
        await sendTo(message.phone, message.text);
        await markSent(message.id);
      } catch (error) {
        console.error(`Falha ao entregar mensagem ${message.id}:`, error);
      }
    }
  } catch (error) {
    console.error("Erro ao consultar a caixa de saída:", error);
  } finally {
    delivering = false;
  }
}

setInterval(deliverOutbox, 15_000);

client.initialize();