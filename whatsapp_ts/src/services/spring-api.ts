const API_URL = "http://localhost:8080";

export async function sendToSpring(phone: string, text: string): Promise<string> {
    const response = await fetch(`${API_URL}/messages`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },         
        body: JSON.stringify({phone, text}),
    });

    if (!response.ok) {
        throw new Error('Spring respondeu com status ${response.status}');
    }

    const data = (await response.json()) as {reply: string};
    return data.reply;
}