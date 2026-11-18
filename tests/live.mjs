import test from "node:test";
import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import { config } from "../dist/services/ai-engine/model.js";
test(
  "real provider → web build → published feature → stateful edit → Flutter build and APK",
  { timeout: 3600000 },
  async () => {
    config();
    const origin = process.env.STUDIO_TEST_URL;
    if (!origin) throw Error("Set STUDIO_TEST_URL to the running studio");
    let cookie = "";
    async function call(path, b) {
      const r = await fetch(origin + path, {
        method: b ? "POST" : "GET",
        headers: { cookie, "Content-Type": "application/json" },
        body: b ? JSON.stringify(b) : undefined,
      });
      assert.ok(r.ok, "HTTP " + r.status + " " + path);
      cookie = r.headers.get("set-cookie")?.split(";")[0] || cookie;
      return r;
    }
    await call("/api/register", {
      name: "e2e-" + randomUUID().slice(0, 12),
      password: randomUUID(),
    });
    async function build(p, b) {
      const j = await (await call(p, b)).json();
      const events = await (
        await call("/api/code/" + j.project + "/events?job=" + j.job)
      ).text();
      assert.match(events, /event: done\ndata: ready/, events.slice(-8000));
      return (await call("/api/code/" + j.project)).json();
    }
    const marker = randomUUID(),
      path = "/api/code";
    const one = await build(path, {
      target: "web",
      prompt:
        "Build a Next.js plant diary. Implement GET /api/identity returning JSON with field marker equal to " +
        marker +
        ". Show the diary at /.",
    });
    assert.ok(one.files["package.json"]);
    const base = path + "/" + one.project.id;
    const two = await build(base + "/edit", {
      base: 1,
      prompt:
        'Add GET /api/count returning JSON {"count":3}. Preserve the existing identity endpoint and exact marker.',
    });
    assert.equal(two.versions.length, 2);
    const { url } = await (await call(base + "/publish", { base: 2 })).json();
    assert.equal(
      (await (await fetch(url + "/api/identity")).json()).marker,
      marker,
    );
    assert.equal((await (await fetch(url + "/api/count")).json()).count, 3);
    const android = await build(path, {
      target: "android",
      prompt:
        "Build a Flutter Material 3 app for recording plant watering dates with a working add button and persistent records.",
    });
    assert.ok(android.files["lib/main.dart"]);
    assert.ok(android.files["pubspec.yaml"]);
    const apk = await (
      await call(path + "/" + android.project.id + "/apk?v=1")
    ).arrayBuffer();
    assert.equal(Buffer.from(apk).readUInt32LE(), 0x04034b50);
  },
);
