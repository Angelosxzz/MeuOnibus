CREATE TABLE suggestions (id text PRIMARY KEY NOT NULL, payload text NOT NULL, status text NOT NULL, client_hash text NOT NULL, created_at text NOT NULL, reviewed_at text, review_token text);
--> statement-breakpoint
CREATE INDEX suggestions_status_created ON suggestions(status,created_at);
--> statement-breakpoint
CREATE INDEX suggestions_client_created ON suggestions(client_hash,created_at);
--> statement-breakpoint
CREATE TABLE suggestion_settings (id integer PRIMARY KEY NOT NULL, enabled integer NOT NULL, updated_at text NOT NULL);
