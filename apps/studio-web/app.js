const $ = (s) => document.querySelector(s);
let id = "",
  detail = null,
  stream;
const message = (x) => ($("#status").textContent = x);
async function api(path, body) {
  const res = await fetch(
    path,
    body === undefined
      ? {}
      : {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
        },
  );
  const data = await res.json();
  if (!res.ok) throw Error(data.error || "Request failed");
  return data;
}
const run = (fn) => async (e) => {
  e?.preventDefault();
  try {
    await fn(e);
  } catch (err) {
    message(err.message);
  }
};
function tabs(name) {
  for (const item of ["preview", "source", "history"])
    $("#" + item).hidden = item !== name;
  for (const b of document.querySelectorAll("[data-tab]"))
    b.classList.toggle("active", b.dataset.tab === name);
}
for (const b of document.querySelectorAll("[data-tab]"))
  b.onclick = () => tabs(b.dataset.tab);
function watch(job) {
  stream?.close();
  $("#activity").textContent = "";
  const s = new EventSource("/api/code/" + id + "/events?job=" + job);
  stream = s;
  s.onmessage = (e) =>
    ($("#activity").textContent += JSON.parse(e.data) + "\n");
  s.addEventListener(
    "done",
    run(async (e) => {
      s.close();
      message(
        e.data === "ready"
          ? "Build ready"
          : "Build failed. See build activity.",
      );
      await show(id, false);
    }),
  );
}
async function show(next, follow = true) {
  stream?.close();
  id = next;
  detail = id ? await api("/api/code/" + id) : null;
  const latest = detail?.versions[0],
    frame = $("iframe");
  $("#project").value = id;
  $("#target").disabled = !!id;
  if (id) $("#target").value = detail.project.target;
  $("#promptLabel").textContent = id
    ? "Describe your changes"
    : "Describe your application";
  frame.hidden = !latest;
  if (latest) frame.src = "https://" + latest.preview + "." + detail.domain;
  else frame.removeAttribute("src");
  $("#empty").hidden = !!latest;
  $("#apk").hidden = !latest?.artifact;
  if (latest?.artifact)
    $("#apk").href = "/api/code/" + id + "/apk?v=" + latest.v;
  $("#publish").disabled = !latest || detail.project.target !== "web";
  $("#link").hidden = true;
  const paths = $("#paths");
  paths.replaceChildren(
    ...Object.keys(detail?.files || {})
      .sort()
      .map((p) => new Option(p, p)),
  );
  paths.onchange = () =>
    ($("#code").textContent = detail?.files[paths.value] || "");
  paths.onchange();
  $("#history").replaceChildren();
  for (const v of detail?.versions || []) {
    const a = document.createElement("article"),
      b = document.createElement("button");
    a.textContent = "v" + v.v + " · " + v.prompt + " ";
    b.textContent = "Restore";
    b.disabled = v.v === latest.v;
    b.onclick = run(async () => {
      await api("/api/code/" + id + "/rollback", {
        base: latest.v,
        version: v.v,
      });
      await show(id);
    });
    a.append(b);
    $("#history").append(a);
  }
  if (follow) {
    $("#activity").textContent = "";
    if (detail?.latestJob) watch(detail.latestJob.id);
  }
}
async function list(next = "") {
  const rows = await api("/api/code");
  $("#project").replaceChildren(
    new Option("+ New project", ""),
    ...rows.map((r) => new Option(r.title, r.id)),
  );
  $("#auth").hidden = true;
  $("#workspace").hidden = false;
  $("#logout").hidden = false;
  await show(next);
}
$("#login").onsubmit = run(async (e) => {
  await api(
    "/api/" + e.submitter.value,
    Object.fromEntries(new FormData(e.target)),
  );
  e.target.reset();
  message("");
  await list();
});
$("#logout").onclick = run(async () => {
  await api("/api/logout", {});
  location.reload();
});
$("#project").onchange = run(() => show($("#project").value));
$("#request").onsubmit = run(async () => {
  const b = $("#request button");
  b.disabled = true;
  try {
    const r = await api("/api/code" + (id ? "/" + id + "/edit" : ""), {
      base: detail?.versions[0]?.v || 0,
      prompt: $("#prompt").value,
      target: $("#target").value,
    });
    $("#prompt").value = "";
    message("Build accepted");
    await list(r.project);
  } finally {
    b.disabled = false;
  }
});
$("#publish").onclick = run(async () => {
  const r = await api("/api/code/" + id + "/publish", {
    base: detail.versions[0].v,
  });
  $("#link").href = r.url;
  $("#link").hidden = false;
});
tabs("preview");
run(async () => {
  if ((await api("/api/status")).signedIn) await list();
})();
