import { randomUUID } from "node:crypto";
import { mkdir, writeFile, readFile } from "node:fs/promises";
import { join } from "node:path";
import { config } from "../dist/services/ai-engine/model.js";
import { generate } from "../dist/services/ai-engine/engine.js";
import { settings, docker } from "../dist/services/sandbox/runner.js";
import { domain } from "../dist/services/publisher/runtime.js";
export function init(db) {
  db.exec(`CREATE TABLE IF NOT EXISTS code_usage(day TEXT PRIMARY KEY,n INTEGER);
 CREATE TABLE IF NOT EXISTS code_projects(id TEXT PRIMARY KEY,owner TEXT,target TEXT,title TEXT);
 CREATE TABLE IF NOT EXISTS code_versions(project TEXT,v INTEGER,source TEXT,plan TEXT,preview TEXT,artifact TEXT,prompt TEXT,PRIMARY KEY(project,v));
 CREATE TABLE IF NOT EXISTS jobs(id TEXT PRIMARY KEY,project TEXT,state TEXT,prompt TEXT);
 CREATE TABLE IF NOT EXISTS events(job TEXT,n INTEGER PRIMARY KEY AUTOINCREMENT,message TEXT);
 CREATE TABLE IF NOT EXISTS releases(alias TEXT PRIMARY KEY,project TEXT UNIQUE,preview TEXT);
 INSERT INTO events(job,message) SELECT id,'Build interrupted during server restart' FROM jobs WHERE state='running';
 UPDATE jobs SET state='failed' WHERE state='running';`);
}
const reject = (status, message) => {
  throw Object.assign(Error(message), { status });
};
const active = new Set();
export async function api({ req, res, p, m, db, uid, body, json, data }) {
  const q = (s) => db.prepare(s),
    parts = p.split("/"),
    id = parts[3],
    action = parts[4];
  const projects = () =>
    q(
      "SELECT id,title,target FROM code_projects WHERE owner=? ORDER BY rowid DESC",
    ).all(uid);
  if (!id && m === "GET") return json(res, projects());
  let project = id
    ? q("SELECT * FROM code_projects WHERE owner=? AND id=?").get(uid, id)
    : null;
  if (id && !project) reject(404, "Project unavailable");
  const versions = () =>
    q(
      "SELECT v,prompt,preview,artifact FROM code_versions WHERE project=? ORDER BY v DESC",
    ).all(id);
  if (action === "events" && m === "GET") {
    const job = new URL(req.url, "http://local").searchParams.get("job");
    if (!q("SELECT 1 FROM jobs WHERE id=? AND project=?").get(job, id))
      reject(404, "Job unavailable");
    res.writeHead(200, {
      "Content-Type": "text/event-stream",
      "X-Accel-Buffering": "no",
      "Cache-Control": "no-store",
    });
    let cursor = Number(req.headers["last-event-id"]) || 0;
    const poll = () => {
      for (const row of q(
        "SELECT * FROM events WHERE job=? AND n>? ORDER BY n",
      ).all(job, cursor)) {
        cursor = row.n;
        res.write(
          "id: " + cursor + "\ndata: " + JSON.stringify(row.message) + "\n\n",
        );
      }
      const { state } = q("SELECT state FROM jobs WHERE id=?").get(job);
      if (state !== "running") {
        res.end("event: done\ndata: " + state + "\n\n");
        return true;
      }
    };
    if (!poll()) {
      const timer = setInterval(poll, 750);
      res.on("close", () => clearInterval(timer));
    }
    return;
  }
  const head = project
    ? q(
        "SELECT * FROM code_versions WHERE project=? ORDER BY v DESC LIMIT 1",
      ).get(id)
    : null;
  if (project && !action && m === "GET")
    return json(res, {
      project,
      versions: versions(),
      files: JSON.parse(head?.source || "{}"),
      latestJob: q(
        "SELECT id,state FROM jobs WHERE project=? ORDER BY rowid DESC LIMIT 1",
      ).get(id),
      domain: process.env.PREVIEW_DOMAIN,
    });
  if (action === "apk" && m === "GET") {
    const v = Number(new URL(req.url, "http://local").searchParams.get("v"));
    const row = q(
      "SELECT artifact FROM code_versions WHERE project=? AND v=?",
    ).get(id, v);
    if (!row?.artifact) reject(404, "APK unavailable");
    res.setHeader(
      "Content-Disposition",
      'attachment; filename="application-debug.apk"',
    );
    res.end(await readFile(join(data, "apk", row.artifact)));
    return;
  }
  if (m !== "POST") reject(404, "Unknown endpoint");
  const b = await body(req);
  if (project && b.base !== (head?.v || 0))
    reject(409, "Project has changed. Reload it.");
  if (active.has(uid))
    reject(409, "A build is already running for this account");
  if (action === "rollback" && head) {
    if (head.v >= 50) reject(409, "Project reached its snapshot limit");
    const old = q("SELECT * FROM code_versions WHERE project=? AND v=?").get(
      id,
      b.version,
    );
    if (!old) reject(404, "Version unavailable");
    q("INSERT INTO code_versions VALUES(?,?,?,?,?,?,?)").run(
      id,
      head.v + 1,
      old.source,
      old.plan,
      old.preview,
      old.artifact,
      "Restore version " + old.v,
    );
    return json(res, { ok: true });
  }
  if (action === "publish" && head) {
    const alias =
      q("SELECT alias FROM releases WHERE project=?").get(id)?.alias ||
      randomUUID();
    q(
      "INSERT INTO releases VALUES(?,?,?) ON CONFLICT(project) DO UPDATE SET preview=excluded.preview",
    ).run(alias, id, head.preview);
    return json(res, { url: "https://" + alias + "." + domain() });
  }
  if (action && action !== "edit") reject(404, "Unknown action");
  const prompt = b.prompt?.trim?.(),
    target = project?.target || b.target;
  if (!prompt || prompt.length > 8000 || !["web", "android"].includes(target))
    reject(400, "Choose a target and enter a prompt (up to 8000 characters)");
  if (project && head?.v >= 50)
    reject(409, "Project reached its snapshot limit");
  if (!project && projects().length >= 30)
    reject(409, "Account reached its project limit");
  if (active.size >= 2) reject(429, "Build workers are busy");
  active.add(uid);
  try {
    config();
    domain();
    await settings(target);
  } catch (e) {
    active.delete(uid);
    reject(503, e.message);
  }
  const job = randomUUID(),
    pid = project?.id || randomUUID();
  try {
    db.exec("BEGIN IMMEDIATE");
    if (!project)
      q("INSERT INTO code_projects VALUES(?,?,?,?)").run(
        pid,
        uid,
        target,
        prompt.slice(0, 70),
      );
    q("INSERT INTO jobs VALUES(?,?,?,?)").run(job, pid, "running", prompt);
    db.exec("COMMIT");
  } catch (e) {
    db.exec("ROLLBACK");
    active.delete(uid);
    throw e;
  }
  const log = (text) =>
    q("INSERT INTO events(job,message) VALUES(?,?)").run(job, text);
  void (async () => {
    let built;
    try {
      built = await generate(
        db,
        target,
        prompt,
        JSON.parse(head?.source || "{}"),
        log,
      );
      const artifact = built.apk ? job + ".apk" : null;
      if (artifact) {
        await mkdir(join(data, "apk"), { recursive: true });
        await writeFile(join(data, "apk", artifact), built.apk);
      }
      db.exec("BEGIN IMMEDIATE");
      try {
        q("INSERT INTO code_versions VALUES(?,?,?,?,?,?,?)").run(
          pid,
          (head?.v || 0) + 1,
          JSON.stringify(built.files),
          JSON.stringify(built.architecture),
          built.preview,
          artifact,
          prompt,
        );
        log("Ready");
        q("UPDATE jobs SET state='ready' WHERE id=?").run(job);
        db.exec("COMMIT");
      } catch (e) {
        db.exec("ROLLBACK");
        throw e;
      }
    } catch (e) {
      if (built)
        await docker(["rm", "-f", "studio-live-" + built.preview]).catch(
          () => {},
        );
      log(String(e.message).slice(-32000));
      q("UPDATE jobs SET state='failed' WHERE id=?").run(job);
    } finally {
      if (built) await docker(["image", "rm", built.tag]).catch(() => {});
      active.delete(uid);
    }
  })();
  res.statusCode = 202;
  json(res, { project: pid, job });
}
