import qrcode from "qrcode-terminal";
import { client } from "./config/wpp-client.ts";
import { fetchPending, markSent, sendToSpring } from "./services/spring-api.ts";

let ready = false;
let delivering = false;

client.on("qr", (qr) => {
  qrcode.generate(qr, { small: true });
});

client.on("authenticated", () => console.log("Autenticado"));
client.on("auth_failure", (msg) => console.error("Falha de autenticação:", msg));
client.on("disconnected", (reason) => {
  ready = false;
  console.error("Desconectado:", reason);
});

client.once("ready", () => {
  ready = true;
  console.log("Client is ready!");
});

client.on("message", async (msg) => {
  try {
    if (msg.from.endsWith("@g.us") || msg.from === "status@broadcast") return;

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