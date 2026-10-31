const API_URL = process.env.API_URL ?? "http://localhost:8080";

export type OutboundMessage = {
  id: number;
  phone: string;
  text: string;
};

export async function sendToSpring(phone: string, text: string): Promise<string> {
  const response = await fetch(`${API_URL}/messages`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ phone, text }),
  });
  if (!response.ok) {
    throw new Error(`Spring respondeu ${response.status}`);
  }
  const data = (await response.json()) as { reply: string };
  return data.reply;
}

export async function fetchPending(): Promise<OutboundMessage[]> {
  const response = await fetch(`${API_URL}/outbox/pending`);
  if (!response.ok) {
    throw new Error(`Spring respondeu ${response.status}`);
  }
  return (await response.json()) as OutboundMessage[];
}

export async function markSent(id: number): Promise<void> {
  const response = await fetch(`${API_URL}/outbox/${id}/sent`, { method: "POST" });
  if (!response.ok) {
    throw new Error(`Spring respondeu ${response.status}`);
  }
}