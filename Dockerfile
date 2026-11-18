FROM node:24-bookworm-slim AS build
WORKDIR /app
COPY package*.json tsconfig.json ./
RUN npm ci
COPY services ./services
RUN npm run build
FROM node:24-bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends zip docker.io && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/dist ./dist
COPY *.mjs package.json index.html ./
COPY public ./public
COPY apps ./apps
COPY services ./services
RUN mkdir /app/data && chown node:node /app/data
USER node
ENV HOST=0.0.0.0 PORT=3000 NODE_ENV=production DATA_DIR=/app/data
VOLUME ["/app/data"]
EXPOSE 3000
CMD ["node","server.mjs"]
