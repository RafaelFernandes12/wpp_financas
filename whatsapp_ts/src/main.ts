import { client } from "./config/wpp-client.ts";
import qrcode from "qrcode-terminal";

client.once("ready", () => {
  console.log("Client is ready!");
});

client.on("qr", (qr) => {
  qrcode.generate(qr, { small: true });
});

// Start your client
client.initialize();
