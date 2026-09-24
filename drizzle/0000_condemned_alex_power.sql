CREATE TABLE `api_usage` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`run_id` text NOT NULL,
	`created_at` text NOT NULL,
	`input_tokens` integer NOT NULL,
	`output_tokens` integer NOT NULL,
	`cost_usd` real NOT NULL,
	FOREIGN KEY (`run_id`) REFERENCES `command_runs`(`id`) ON UPDATE no action ON DELETE no action
);
--> statement-breakpoint
CREATE TABLE `command_runs` (
	`id` text PRIMARY KEY NOT NULL,
	`client_id` text NOT NULL,
	`created_at` text NOT NULL,
	`input_text` text NOT NULL,
	`status` text NOT NULL,
	`intent` text,
	`target` text,
	`cost_usd` real DEFAULT 0 NOT NULL,
	`elapsed_ms` integer DEFAULT 0 NOT NULL,
	`events_json` text DEFAULT '[]' NOT NULL
);
--> statement-breakpoint
CREATE INDEX `idx_command_runs_client_created` ON `command_runs` (`client_id`,`created_at`);