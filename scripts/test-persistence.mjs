import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { DatabaseSync } from "node:sqlite";
import worker from "../worker/index.js";

const db = new DatabaseSync(":memory:");
const migration = readFileSync(new URL("../drizzle/0000_condemned_alex_power.sql", import.meta.url), "utf8");
for (const statement of migration.split("--> statement-breakpoint").map(s => s.trim()).filter(Boolean)) db.exec(statement);
const D1 = { prepare(sql) {
  let args = [];
  return {
    bind(...values) { args = values; return this; },
    first() { return db.prepare(sql).get(...args) ?? null; },
    all() { return { results: db.prepare(sql).all(...args) }; },
    run() { return db.prepare(sql).run(...args); },
  };
} };
const nativeFetch = globalThis.fetch;
let calls = 0;
globalThis.fetch = async (url, options) => {
  if (String(url) !== "https://api.typesafe.ai/v1/systemone") return nativeFetch(url, options);
  calls++;
  const request = JSON.parse(options.body);
  const answers = request.questions.intent
    ? { intent: { choice: "home_control", confidence: 0.99 } }
    : { device: { choice: "MJ-001", confidence: 0.99 }, set_power: { choice: "1", confidence: 0.99 } };
  return new Response(JSON.stringify({ model: "jev-latest", answers, usage: { input_tokens: 100, output_tokens: 10 } }), { headers: { "content-type": "application/json" } });
};
const env = { DB: D1, JEV_API_KEY: "test-only" };
const clientA = "a0000000-0000-4000-8000-000000000001";
const clientB = "b0000000-0000-4000-8000-000000000002";
const get = (path, id) => worker.fetch(new Request("https://example.test" + path, { headers: id ? { "X-Client-Id": id } : {} }), env);
try {
  const post = await worker.fetch(new Request("https://example.test/api/process", {
    method: "POST", headers: { "Content-Type": "application/json", "X-Client-Id": clientA, "CF-Connecting-IP": "persistence-test" },
    body: JSON.stringify({ text: "开主卧主灯", room: "主卧", threshold: 0.35 }),
  }), env);
  assert.equal(post.status, 200);
  const events = (await post.text()).trim().split("\n").map(JSON.parse);
  assert.equal(events.find(e => e.kind === "result")?.status, "success");
  assert.equal(calls, 2);

  const costs = await (await get("/api/costs")).json();
  assert.equal(costs.summary.request_count, 2);
  assert.equal(costs.summary.instruction_count, 1);
  assert.equal(costs.summary.cumulative_usd, 200 * 0.042 / 1e6);
  assert.equal(costs.summary.average_per_instruction_usd, costs.summary.cumulative_usd);
  const a = await (await get("/api/history", clientA)).json();
  assert.equal(a.items.length, 1);
  assert.equal(a.items[0].input_text, "开主卧主灯");
  assert.equal(a.items[0].status, "success");
  assert(a.items[0].events.some(e => e.kind === "decision" && e.stage === 2));
  assert(!a.items[0].events.some(e => e.kind === "debug"));
  const b = await (await get("/api/history", clientB)).json();
  assert.equal(b.items.length, 0);
  assert.equal((await get("/api/history")).status, 400);
  console.log("D1 history, visitor scoping, and persisted cost totals passed");
} finally { globalThis.fetch = nativeFetch; db.close(); }
