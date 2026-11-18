import { spawn } from "node:child_process";
import { randomUUID } from "node:crypto";
import type { Files, Target } from "../ai-engine/source.js";

export function docker(
  args: string[],
  input?: Buffer,
  timeout = 60000,
  max = 160000,
): Promise<Buffer> {
  return new Promise((resolve, reject) => {
    const proc = spawn("docker", args, { stdio: ["pipe", "pipe", "pipe"] });
    let size = 0;
    const chunks: Buffer[] = [];
    let errors = "";
    const timer = setTimeout(() => proc.kill("SIGKILL"), timeout);
    proc.stdout.on("data", (chunk: Buffer) => {
      size += chunk.length;
      if (size > max) {
        proc.kill("SIGKILL");
        return;
      }
      chunks.push(chunk);
    });
    proc.stderr.on("data", (chunk: Buffer) => {
      errors = (errors + chunk.toString()).slice(-32000);
    });
    proc.on("error", reject);
    proc.stdin.on("error", () => {});
    proc.stdin.end(input);
    proc.once("close", (code) => {
      clearTimeout(timer);
      if (code === 0 && size <= max) resolve(Buffer.concat(chunks));
      else
        reject(
          Error(
            (
              errors +
              "\n" +
              Buffer.concat(chunks).toString().slice(-16000)
            ).trim() || "Sandbox process failed or exceeded its limit",
          ),
        );
    });
  });
}
export const isolation = [
  "--runtime=runsc",
  "--cap-drop=ALL",
  "--security-opt=no-new-privileges",
  "--pids-limit=256",
  "--memory=3g",
  "--cpus=2",
  "--storage-opt",
  "size=8G",
  "--user=1000:1000",
];
export async function settings(target: Target) {
  const image =
      process.env[target === "web" ? "WEB_BUILD_IMAGE" : "ANDROID_BUILD_IMAGE"],
    network = process.env.BUILD_NETWORK;
  if (
    !image ||
    !/@sha256:[a-f0-9]{64}$/.test(image) ||
    !network ||
    !/^[-\w]+$/.test(network)
  )
    throw Error(
      "Configure digest-pinned build image and internal mirror network",
    );
  const internal = (
    await docker(["network", "inspect", "-f", "{{.Internal}}", network])
  )
    .toString()
    .trim();
  if (internal !== "true") throw Error("Build network must be internal");
  return { image, network };
}
export async function compile(
  files: Files,
  target: Target,
  log: (s: string) => void,
) {
  const { image, network } = await settings(target),
    name = "studio-build-" + randomUUID(),
    tag = "studio-output:" + randomUUID();
  const exec = (cmd: string[], input?: Buffer, max?: number) =>
    docker(
      ["exec", "-i", "-w", "/workspace", name, ...cmd],
      input,
      720000,
      max,
    );
  try {
    await docker([
      "run",
      "-d",
      "--name",
      name,
      ...isolation,
      "--network",
      network,
      "--entrypoint",
      "sleep",
      image,
      "infinity",
    ]);
    if (target === "android") {
      // Supply SDK-owned platform launchers and binary Gradle wrapper only.
      await exec([
        "flutter",
        "create",
        "--platforms=android,web",
        "--project-name",
        "generated_app",
        "--no-pub",
        "/tmp/platform_seed",
      ]);
      await exec([
        "sh",
        "-c",
        "cp -R /tmp/platform_seed/android /tmp/platform_seed/web . && rm -rf /tmp/platform_seed",
      ]);
    }
    for (const [path, source] of Object.entries(files)) {
      await exec(
        [
          "sh",
          "-c",
          'mkdir -p -- "$(dirname -- "$1")" && cat > "$1"',
          "write",
          "./" + path,
        ],
        Buffer.from(source),
      );
    }
    log("Installing dependencies");
    await exec(
      target === "web"
        ? [
            "npm",
            files["package-lock.json"] ? "ci" : "install",
            "--no-audit",
            "--no-fund",
          ]
        : ["flutter", "pub", "get"],
    );
    log("Building " + target);
    if (target === "web") await exec(["npm", "run", "build"]);
    else {
      await exec(["flutter", "analyze", "--no-fatal-infos"]);
      await exec(["flutter", "build", "apk", "--debug", "--no-pub"]);
      await exec(["flutter", "build", "web", "--no-pub", "--release"]);
    }
    const apk =
      target === "android"
        ? await exec(
            ["cat", "build/app/outputs/flutter-apk/app-debug.apk"],
            undefined,
            200_000_000,
          )
        : null;
    await docker(["commit", name, tag], undefined, 120000);
    return { tag, apk };
  } finally {
    await docker(["rm", "-f", name]).catch(() => {});
  }
}
