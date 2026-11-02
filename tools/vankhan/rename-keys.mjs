import fs from "node:fs";
import path from "node:path";

/**
 * Đổi tên toàn bộ key bài văn khấn sang dạng kebab-case đầy đủ từ
 * (vk... → van-khan-...). Chạy được nhiều lần: key đã đổi sẽ được bỏ qua.
 * Ảnh hưởng: corpus/*.md, when.json, index.json. Sau đó chạy build-assets.mjs.
 */
const ROOT = "D:/android/calendar";
const CORPUS = path.join(ROOT, "tools/vankhan/corpus");
const ASSETS = path.join(ROOT, "app/src/main/assets/vankhan");

const map = JSON.parse(fs.readFileSync(path.join(ROOT, "tools/vankhan/rename.json"), "utf8"));
const renamed = new Set(Object.values(map));
const HEADER = /^===\s*([\w.-]+)\s*===$/;

function target(key) {
  if (map[key]) return map[key];
  if (renamed.has(key)) return key;
  throw new Error("key chưa có trong rename.json: " + key);
}

const files = fs.readdirSync(CORPUS).filter((f) => f.endsWith(".md")).sort();
const seen = new Map();
let changed = 0;

for (const file of files) {
  const full = path.join(CORPUS, file);
  const lines = fs.readFileSync(full, "utf8").replace(/\r\n?/g, "\n").split("\n");
  let touched = false;
  for (let i = 0; i < lines.length; i++) {
    const m = HEADER.exec(lines[i]);
    if (!m) continue;
    const next = target(m[1]);
    if (seen.has(next)) throw new Error("trùng key sau đổi: " + next + " (" + seen.get(next) + " và " + file + ")");
    seen.set(next, file);
    if (next !== m[1]) {
      lines[i] = "=== " + next + " ===";
      touched = true;
    }
  }
  if (touched) {
    fs.writeFileSync(full, lines.join("\n"), "utf8");
    changed++;
  }
}

const whenPath = path.join(ROOT, "tools/vankhan/when.json");
const when = JSON.parse(fs.readFileSync(whenPath, "utf8"));
const whenItems = {};
for (const [key, rules] of Object.entries(when.items)) whenItems[target(key)] = rules;
when.items = whenItems;
fs.writeFileSync(whenPath, JSON.stringify(when, null, 1) + "\n", "utf8");

const indexPath = path.join(ASSETS, "index.json");
const index = JSON.parse(fs.readFileSync(indexPath, "utf8"));
let indexKeys = 0;
for (const part of index.parts) {
  for (const group of part.groups) {
    for (const item of group.items) {
      const next = target(item.key);
      if (next !== item.key) {
        item.key = next;
        indexKeys++;
      }
    }
  }
}
fs.writeFileSync(indexPath, JSON.stringify(index, null, 1) + "\n", "utf8");

console.log("corpus bài: " + seen.size + " | file sửa: " + changed + " | index sửa key: " + indexKeys);
