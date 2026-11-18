// The domain gateway only resolves IDs already registered by the builder.
import http from "node:http";
import { spawn } from "node:child_process";
import { Duplex } from "node:stream";
const relay = `import os,socket,threading
s=socket.create_connection(("127.0.0.1",8080),5)
s.settimeout(55)
def send():
 try:
  while True:
   b=os.read(0,65536)
   if not b: break
   s.sendall(b)
 finally: s.shutdown(socket.SHUT_WR)
threading.Thread(target=send,daemon=True).start()
while True:
 b=s.recv(65536)
 if not b: break
 os.write(1,b)`;
let active = 0;
export function proxy(req, res, db) {
  const suffix = "." + process.env.PREVIEW_DOMAIN,
    host = (req.headers.host || "").split(":")[0];
  if (!process.env.PREVIEW_DOMAIN || !host.endsWith(suffix)) return false;
  const id = host.slice(0, -suffix.length);
  if (
    !/^[a-f0-9-]{36}$/.test(id) ||
    !db
      .prepare(
        "SELECT 1 FROM code_versions WHERE preview=? UNION SELECT 1 FROM releases WHERE alias=?",
      )
      .get(id, id)
  ) {
    res.writeHead(404).end();
    return true;
  }
  const route =
    db.prepare("SELECT preview FROM releases WHERE alias=?").get(id)?.preview ||
    id;
  if (active >= 48) {
    res.writeHead(503).end();
    return true;
  }
  active++;
  const child = spawn(
    "docker",
    ["exec", "-i", "studio-live-" + route, "python3", "-c", relay],
    { stdio: ["pipe", "pipe", "ignore"] },
  );
  const socket = Duplex.from({ readable: child.stdout, writable: child.stdin });
  const finish = () => {
    child.kill("SIGKILL");
    socket.destroy();
  };
  const timer = setTimeout(finish, 60000);
  child.on("close", () => {
    active--;
    clearTimeout(timer);
  });
  child.on("error", () => socket.destroy(Error("Runner unavailable")));
  const agent = new http.Agent({ keepAlive: false });
  agent.createConnection = () => socket;
  const headers = { host: "localhost:8080" };
  for (const k of [
    "accept",
    "content-type",
    "content-length",
    "cookie",
    "authorization",
    "accept-language",
    "if-none-match",
    "range",
  ])
    if (req.headers[k]) headers[k] = req.headers[k];
  const upstream = http.request(
    {
      host: "localhost",
      port: 8080,
      method: req.method,
      path: req.url,
      headers,
      agent,
    },
    (incoming) => {
      const h = {};
      for (const k of [
        "content-type",
        "content-length",
        "content-encoding",
        "cache-control",
        "etag",
        "location",
        "set-cookie",
      ])
        if (incoming.headers[k]) h[k] = incoming.headers[k];
      if (h["set-cookie"])
        h["set-cookie"] = h["set-cookie"].map((value) =>
          value.replace(/;\s*domain=[^;]*/gi, ""),
        );
      res.writeHead(incoming.statusCode, h);
      incoming.pipe(res);
    },
  );
  upstream.on("error", () => {
    if (!res.headersSent) res.writeHead(502);
    res.end();
  });
  res.once("close", () => {
    upstream.destroy();
    finish();
  });
  req.pipe(upstream);
  return true;
}
