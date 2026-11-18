FROM node:24-bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends zip && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --chown=node:node *.mjs package.json index.html ./
COPY --chown=node:node public ./public
RUN mkdir -p /app/data && chown node:node /app/data
USER node
ENV HOST=0.0.0.0 PORT=3000 NODE_ENV=production DATA_DIR=/app/data
VOLUME ["/app/data"]
EXPOSE 3000
CMD ["node","server.mjs"]
