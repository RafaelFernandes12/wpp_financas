FROM node:24-bookworm-slim

RUN apt-get update \
 && apt-get install -y --no-install-recommends chromium fonts-liberation ca-certificates \
 && rm -rf /var/lib/apt/lists/*

ENV PUPPETEER_SKIP_DOWNLOAD=true \
    PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium

WORKDIR /app
COPY package*.json ./
RUN npm ci --omit=dev
COPY src ./src

# remove travas do Chromium que sobram se o container foi encerrado à força
CMD ["sh", "-c", "rm -f /data/wwebjs_auth/session/Singleton* && exec node src/main.ts"]