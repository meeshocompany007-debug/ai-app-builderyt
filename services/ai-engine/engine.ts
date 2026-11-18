import { ask } from "./model.js";
import {
  plan,
  apply,
  context,
  required,
  type Files,
  type Target,
} from "./source.js";
import { compile, docker } from "../sandbox/runner.js";
import { launch } from "../publisher/runtime.js";
import type { DatabaseSync } from "node:sqlite";
export async function generate(
  db: DatabaseSync,
  target: Target,
  prompt: string,
  base: Files,
  log: (s: string) => void,
) {
  const rules =
    target === "web"
      ? "Build a complete Next.js/React Tailwind application. Implement API routes and database if needed. npm run build and npm start on port 8080 must work."
      : "Build complete Flutter Material 3 native Android and browser compatible app with Gradle sources, AndroidManifest, pubspec and Dart services. The runner supplies Flutter SDK Android and web launchers and binary Gradle wrapper. Use package com.example.generated_app and name generated_app. Do not emit binary files.";
  log("Planning");
  const architecture = plan(
    await ask(
      db,
      "Plan the requested application. Output {targetPlatform,techStack,databaseSchema,apiEndpoints,fileTree}. All fields except targetPlatform are arrays of strings. fileTree lists all final source files. " +
        rules,
      { targetPlatform: target, prompt, existing: context(base) },
    ),
    target,
  );
  let files = base;
  const format =
    'Return a JSON array of targeted edits: {op:"write"|"delete",path,before,content}. before is the exact supplied sha256 of the current file, or null for new files. Writes contain complete real source. Preserve unrelated features, data, routes and files. ' +
    rules;
  log("Generating files");
  const edits = await ask(db, format, {
    prompt,
    architecture,
    existing: context(base),
  });
  files = apply(base, edits);
  for (let attempt = 0; ; attempt++) {
    let built: Awaited<ReturnType<typeof compile>> | undefined;
    try {
      required(files, target);
      for (const p of architecture.fileTree)
        if (!Object.hasOwn(files, p)) throw Error("Missing planned file: " + p);
      built = await compile(files, target, log);
      log("Starting preview");
      const preview = await launch(built.tag, target);
      return { files, architecture, ...built, preview };
    } catch (err) {
      if (built) await docker(["image", "rm", built.tag]).catch(() => {});
      if (attempt === 3) throw err;
      const error = String(err).slice(-24000);
      log("Self-healing " + (attempt + 1) + "/3: " + error);
      files = apply(
        files,
        await ask(db, format, {
          prompt,
          architecture,
          error,
          files: context(files),
        }),
      );
    }
  }
}
