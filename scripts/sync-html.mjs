import { readFile, writeFile } from "node:fs/promises";
import { resolve } from "node:path";

const root = resolve(import.meta.dirname, "..");
const source = resolve(root, "worker/index.js");
const [worker, html] = await Promise.all([
  readFile(source, "utf8"),
  readFile(resolve(root, "index.html")),
]);
const encoded = html.toString("base64");
const updated = worker.replace(/^const HTML_B64 = "[A-Za-z0-9+/=]+";/m, `const HTML_B64 = "${encoded}";`);
if (updated === worker && !worker.includes(`const HTML_B64 = "${encoded}";`)) {
  throw new Error("Worker HTML_B64 placeholder not found");
}
if (updated !== worker) await writeFile(source, updated);
