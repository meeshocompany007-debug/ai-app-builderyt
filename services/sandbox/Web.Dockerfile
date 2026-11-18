FROM node:24-bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends python3 curl make g++ ca-certificates && rm -rf /var/lib/apt/lists/* && install -d -o 1000 -g 1000 /workspace
WORKDIR /workspace
USER 1000:1000
