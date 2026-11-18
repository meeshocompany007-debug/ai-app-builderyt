import test from "node:test";
import assert from "node:assert/strict";
import { DatabaseSync } from "node:sqlite";
import { randomUUID } from "node:crypto";
import {
  apply,
  plan,
  context,
  required,
} from "../dist/services/ai-engine/source.js";
import { api, init } from "../services/api.mjs";
test("targeted changes preserve unrelated source and reject stale hashes and paths", () => {
  const initial = { "a.ts": "original", "b.ts": "retained" },
    [a] = context(initial);
  const edited = apply(initial, [
    { path: "a.ts", op: "write", before: a.sha256, content: "changed" },
  ]);
  assert.equal(edited["b.ts"], "retained");
  assert.equal(initial["a.ts"], "original");
  for (const path of ["../a", "/a", "constructor", "a.ts"])
    assert.throws(() =>
      apply(initial, [{ path, op: "write", before: null, content: "x" }]),
    );
  assert.throws(() => required({}, "android"));
  assert.throws(() => plan({ targetPlatform: "android" }, "web"));
});
test("durable rollback preserves snapshots and blocks a different owner", async () => {
  const db = new DatabaseSync(":memory:");
  init(db);
  const id = randomUUID();
  db.prepare("INSERT INTO code_projects VALUES(?,?,?,?)").run(
    id,
    "owner",
    "web",
    "example",
  );
  for (let v = 1; v <= 2; v++)
    db.prepare("INSERT INTO code_versions VALUES(?,?,?,?,?,?,?)").run(
      id,
      v,
      JSON.stringify({ a: String(v) }),
      "{}",
      randomUUID(),
      null,
      "edit",
    );
  const call = (uid, base) =>
    api({
      req: {},
      res: {},
      p: "/api/code/" + id + "/rollback",
      m: "POST",
      db,
      uid,
      body: async () => ({ base, version: 1 }),
      json: () => {},
      data: ".",
    });
  await assert.rejects(call("other", 2), { status: 404 });
  await assert.rejects(call("owner", 1), { status: 409 });
  await call("owner", 2);
  assert.deepEqual(
    db
      .prepare("SELECT source FROM code_versions ORDER BY v")
      .all()
      .map((r) => r.source),
    ['{"a":"1"}', '{"a":"2"}', '{"a":"1"}'],
  );
  db.close();
});
test("studio entry, authenticated project list and unavailable-provider response over HTTP", async () => {
  const { mkdtemp, rm } = await import("node:fs/promises");
  const { tmpdir } = await import("node:os");
  const { join } = await import("node:path");
  const { createServer } = await import("node:http");
  const dir = await mkdtemp(join(tmpdir(), "studio-http-"));
  process.env.STUDIO_TEST = "1";
  process.env.DATA_DIR = dir;
  process.env.LLM_PROVIDER = "unconfigured";
  const { handler } = await import("../server.mjs");
  const server = createServer(handler);
  await new Promise((r) => server.listen(0, "127.0.0.1", r));
  try {
    const url = "http://127.0.0.1:" + server.address().port;
    assert.match(await (await fetch(url)).text(), /Run build/);
    assert.equal((await fetch(url + "/api/code")).status, 401);
    const headers = { "Content-Type": "application/json" };
    const login = await fetch(url + "/api/register", {
      method: "POST",
      headers,
      body: JSON.stringify({ name: "tester", password: randomUUID() }),
    });
    headers.cookie = login.headers.get("set-cookie").split(";")[0];
    assert.deepEqual(
      await (await fetch(url + "/api/code", { headers })).json(),
      [],
    );
    const result = await fetch(url + "/api/code", {
      method: "POST",
      headers,
      body: JSON.stringify({ target: "web", prompt: "Track museum visits" }),
    });
    assert.equal(result.status, 503);
    assert.match((await result.json()).error, /LLM provider/);
  } finally {
    await new Promise((r) => server.close(r));
    await rm(dir, { recursive: true, force: true });
  }
});
