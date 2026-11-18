import { randomUUID } from "node:crypto";
import { docker, isolation } from "../sandbox/runner.js";
import type { Target } from "../ai-engine/source.js";
export function domain() {
  const host = process.env.PREVIEW_DOMAIN;
  if (!host || !/^([a-z0-9-]+\.)+[a-z]{2,}$/.test(host))
    throw Error("Configure separate wildcard PREVIEW_DOMAIN");
  return host;
}
export async function launch(tag: string, target: Target) {
  const id = randomUUID(),
    name = "studio-live-" + id;
  domain();
  try {
    await docker([
      "run",
      "-d",
      "--name",
      name,
      ...isolation,
      "--restart=unless-stopped",
      "--network=none",
      "-w",
      "/workspace",
      "-e",
      "PORT=8080",
      "-e",
      "HOSTNAME=127.0.0.1",
      "--entrypoint",
      target === "web" ? "npm" : "python3",
      tag,
      ...(target === "web"
        ? ["start"]
        : [
            "-m",
            "http.server",
            "8080",
            "--bind",
            "127.0.0.1",
            "--directory",
            "build/web",
          ]),
    ]);
    for (let n = 0; n < 25; n++) {
      try {
        await docker([
          "exec",
          name,
          "curl",
          "-fsS",
          "-o",
          "/dev/null",
          "--max-time",
          "3",
          "http://127.0.0.1:8080/",
        ]);
        return id;
      } catch {
        await new Promise((r) => setTimeout(r, 500));
      }
    }
    const logs = await docker(["logs", "--tail", "40", name]);
    throw Error("Preview startup failed: " + logs.toString().slice(-4000));
  } catch (e) {
    await docker(["rm", "-f", name]).catch(() => {});
    throw e;
  }
}
