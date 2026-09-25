import { readFileSync } from "node:fs";
import { DatabaseSync } from "node:sqlite";

export function createTestD1() {
  const sqlite = new DatabaseSync(":memory:");
  const migration = readFileSync(new URL("../drizzle/0000_condemned_alex_power.sql", import.meta.url), "utf8");
  for (const sql of migration.split("--> statement-breakpoint").map(s => s.trim()).filter(Boolean)) sqlite.exec(sql);
  return { prepare(sql) {
    let args = [];
    return {
      bind(...values) { args = values; return this; },
      first() { return sqlite.prepare(sql).get(...args) ?? null; },
      all() { return { results: sqlite.prepare(sql).all(...args) }; },
      run() { return sqlite.prepare(sql).run(...args); },
    };
  } };
}
