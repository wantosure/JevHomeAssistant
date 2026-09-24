import { integer, real, sqliteTable, text, index } from "drizzle-orm/sqlite-core";

export const commandRuns = sqliteTable("command_runs", {
  id: text("id").primaryKey(),
  clientId: text("client_id").notNull(),
  createdAt: text("created_at").notNull(),
  inputText: text("input_text").notNull(),
  status: text("status").notNull(),
  intent: text("intent"),
  target: text("target"),
  costUsd: real("cost_usd").notNull().default(0),
  elapsedMs: integer("elapsed_ms").notNull().default(0),
  eventsJson: text("events_json").notNull().default("[]"),
}, table => [index("idx_command_runs_client_created").on(table.clientId, table.createdAt)]);

export const apiUsage = sqliteTable("api_usage", {
  id: integer("id").primaryKey({ autoIncrement: true }),
  runId: text("run_id").notNull().references(() => commandRuns.id),
  createdAt: text("created_at").notNull(),
  inputTokens: integer("input_tokens").notNull(),
  outputTokens: integer("output_tokens").notNull(),
  costUsd: real("cost_usd").notNull(),
});
