import { client } from "./config/wpp-client.ts";
import { sendToSpring } from "./services/spring-api.ts";
import qrcode from "qrcode-terminal";

client.once("ready", () => {
  console.log("Client is ready!");
});

client.on("qr", (qr) => {
  qrcode.generate(qr, { small: true });
});

client.on("message", async (message) => {
  // ignora grupos e status
  if (message.from.endsWith("@g.us") || message.from === "status@broadcast") return;

  // só reage a comandos
  if (!message.body.startsWith("!")) return;

  try {
    const contact = await message.getContact();
    const reply = await sendToSpring(contact.number, message.body);
    await client.sendMessage(message.from, reply);
  } catch (error) {
    console.error("Erro ao tratar mensagem:", error);
    await client.sendMessage(message.from, "Ops, deu um erro aqui. Tente de novo em instantes.");
  }
});

// Start your client
client.initialize();