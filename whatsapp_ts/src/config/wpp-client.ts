import pkg from "whatsapp-web.js";
const { Client, LocalAuth } = pkg;

// Set WA_PROXY to route the browser's traffic through a proxy, e.g.
// `socks5://172.17.0.1:1080`. Left unset, Chromium connects directly and
// behaviour is unchanged.
// const proxyArgs = process.env.WA_PROXY
//   ? [`--proxy-server=${process.env.WA_PROXY}`]
//   : [];

export const client = new Client({
  authStrategy: new LocalAuth(),
  puppeteer: {
    args: ["--no-sandbox", "--disable-setuid-sandbox"],
  },
  // Pinned to a single file, this freezes the browser on one WhatsApp Web build
  // forever: the 2026-08-23 pin stopped syncing once WhatsApp moved on. Bump
  // this when the client links but never reaches `ready`; `type: 'none'` (load
  // whatever WhatsApp serves live) is the fallback if pinning keeps breaking.
  // webVersionCache: {
  //   type: 'remote',
  //   remotePath:
  //     'https://raw.githubusercontent.com/wppconnect-team/wa-version/main/html/2.3000.1046969912-alpha.html',
  // },
});
