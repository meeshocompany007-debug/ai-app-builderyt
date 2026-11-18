import { createHash } from "node:crypto";
export type Target = "web" | "android";
export type Files = Record<string, string>;
export type Plan = {
  targetPlatform: Target;
  techStack: string[];
  databaseSchema: string[];
  apiEndpoints: string[];
  fileTree: string[];
};
export const sha = (text: string) =>
  createHash("sha256").update(text).digest("hex");
export function safePath(value: unknown): string {
  if (
    typeof value !== "string" ||
    value.length > 200 ||
    !value
      .split("/")
      .every(
        (s) =>
          /^[a-zA-Z0-9_@()[\].+-]+$/.test(s) &&
          ![
            ".",
            "..",
            ".git",
            "node_modules",
            "__proto__",
            "constructor",
            "prototype",
            ".npmrc",
            ".env",
          ].includes(s),
      )
  )
    throw Error("Invalid source path");
  return value;
}
function strings(x: unknown, max = 160): string[] {
  if (
    !Array.isArray(x) ||
    x.length > max ||
    x.some((s) => typeof s !== "string" || !s.length || s.length > 4000)
  )
    throw Error("Invalid plan list");
  return x;
}
export function plan(x: unknown, target: Target): Plan {
  if (
    !x ||
    typeof x !== "object" ||
    !("targetPlatform" in x) ||
    x.targetPlatform !== target
  )
    throw Error("Planner returned wrong target");
  const p = x as Plan;
  const fileTree = strings(p.fileTree).map(safePath);
  if (!fileTree.length || new Set(fileTree).size !== fileTree.length)
    throw Error("Invalid file tree");
  return {
    targetPlatform: target,
    techStack: strings(p.techStack),
    databaseSchema: strings(p.databaseSchema),
    apiEndpoints: strings(p.apiEndpoints),
    fileTree,
  };
}
export function apply(files: Files, raw: unknown): Files {
  if (!Array.isArray(raw) || raw.length === 0 || raw.length > 160)
    throw Error("Expected file changes");
  const out: Files = Object.assign(Object.create(null), files),
    seen = new Set();
  for (const edit of raw) {
    const path = safePath(edit.path);
    if (seen.has(path)) throw Error("Duplicate change: " + path);
    seen.add(path);
    const exists = Object.hasOwn(out, path);
    if (edit.before !== (exists ? sha(out[path]) : null))
      throw Error("File changed: " + path);
    if (edit.op === "delete" && exists) delete out[path];
    else if (
      edit.op === "write" &&
      typeof edit.content === "string" &&
      !edit.content.includes("\0")
    )
      out[path] = edit.content;
    else throw Error("Invalid edit: " + path);
  }
  if (
    Object.keys(out).length > 160 ||
    Buffer.byteLength(JSON.stringify(out)) > 400000
  )
    throw Error("Project exceeded source limit");
  for (const p of Object.keys(out)) {
    const parts = p.split("/");
    while (parts.length > 1) {
      parts.pop();
      if (Object.hasOwn(out, parts.join("/"))) throw Error("Path collision");
    }
  }
  return out;
}
export function context(files: Files) {
  return Object.entries(files).map(([path, content]) => ({
    path,
    sha256: sha(content),
    content,
  }));
}
export function required(files: Files, target: Target) {
  const names =
    target === "web"
      ? ["package.json"]
      : [
          "pubspec.yaml",
          "lib/main.dart",
          "android/app/src/main/AndroidManifest.xml",
        ];
  for (const path of names)
    if (!files[path]?.trim()) throw Error("Missing " + path);
  if (target === "web") {
    const pkg = JSON.parse(files["package.json"]);
    if (!pkg.scripts?.build || !pkg.scripts?.start)
      throw Error("Missing build/start scripts");
  }
}
