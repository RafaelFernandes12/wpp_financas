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

  try{
    const contact = await message.getContact();
    const reply = await sendToSpring(contact.number, message.body);
    if(reply){
      await client.sendMessage(message.from, reply);
    }
  } catch(error){
    console.error("Erro ao tratar mensagem", error);
    await client.sendMessage(message.from, "Opa, deu um erro aqui. Tente de novo jajá e seja feliz.");
  }
});

// Start your client
client.initialize();