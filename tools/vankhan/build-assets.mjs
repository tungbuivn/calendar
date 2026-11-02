import fs from "node:fs";
import path from "node:path";

/**
 * Ghép nội dung tự soạn (tools/vankhan/corpus/*.md) vào app/src/main/assets/vankhan/.
 * Corpus: mỗi bài mở đầu bằng dòng "=== key ===", phần còn lại là toàn văn,
 * các chỗ người cúng điền viết dạng marker {{field}} (xem mark-slots.mjs).
 * Tiêu đề lấy theo index.json; nhãn ngày lấy theo when.json; danh mục trường lấy theo fields.json.
 * Kết quả: items/<key>.txt giữ nguyên marker + index.json có slots/when cho từng bài.
 */
const ROOT = "D:/android/calendar";
const CORPUS = path.join(ROOT, "tools/vankhan/corpus");
const ASSETS = path.join(ROOT, "app/src/main/assets/vankhan");
const ITEMS = path.join(ASSETS, "items");

const index = JSON.parse(fs.readFileSync(path.join(ASSETS, "index.json"), "utf8"));
const fields = JSON.parse(fs.readFileSync(path.join(ROOT, "tools/vankhan/fields.json"), "utf8"));
const when = JSON.parse(fs.readFileSync(path.join(ROOT, "tools/vankhan/when.json"), "utf8"));

const fieldIds = new Set(fields.fields.map((f) => f.id));
const titles = new Map();
for (const part of index.parts) {
  for (const group of part.groups) {
    for (const item of group.items) titles.set(item.key, item.title);
  }
}

const bodies = new Map();
for (const file of fs.readdirSync(CORPUS).filter((f) => f.endsWith(".md")).sort()) {
  const lines = fs.readFileSync(path.join(CORPUS, file), "utf8").replace(/\r\n?/g, "\n").split("\n");
  let key = null;
  const buf = [];
  const flush = () => {
    if (key) bodies.set(key, buf.join("\n").trim());
  };
  for (const line of lines) {
    const m = /^===\s*([\w.-]+)\s*===$/.exec(line);
    if (m) {
      flush();
      buf.length = 0;
      key = m[1];
    } else if (key) {
      buf.push(line);
    }
  }
  flush();
}

const missing = [...titles.keys()].filter((k) => !bodies.has(k));
for (const extraKey of [...bodies.keys()].filter((k) => !titles.has(k))) {
  console.log("THUA (không có trong index):", extraKey);
}
for (const key of Object.keys(when.items)) {
  if (!titles.has(key)) console.log("WHEN SAI KEY:", key);
}

const slotsOf = new Map();
for (const [key, body] of bodies) {
  // Toàn văn để đọc bằng text-to-speech không còn chú thích trong ngoặc (chạy clean-tts.mjs).
  if (body.includes("(")) console.log("CÒN CHÚ THÍCH ở", key + ":", body.slice(Math.max(0, body.indexOf("(") - 40), body.indexOf(")") + 1));
  const found = [];
  for (const m of body.matchAll(/\{\{(\w+)\}\}/g)) {
    if (!fieldIds.has(m[1])) console.log("FIELD LẠ ở", key + ":", m[1]);
    if (!found.includes(m[1])) found.push(m[1]);
  }
  slotsOf.set(key, found);
}

fs.mkdirSync(ITEMS, { recursive: true });
let written = 0;
let slotRuns = 0;
for (const part of index.parts) {
  for (const group of part.groups) {
    for (const item of group.items) {
      const body = bodies.get(item.key);
      if (!body) continue;
      fs.writeFileSync(path.join(ITEMS, item.key + ".txt"), "# " + item.title + "\n\n" + body + "\n", "utf8");
      item.slots = slotsOf.get(item.key) || [];
      item.when = when.items[item.key] || [];
      slotRuns += item.slots.length;
      written++;
    }
  }
}
index.fields = fields;

const tagged = index.parts.reduce((n, p) => n + p.groups.reduce((m, g) => m + g.items.filter((i) => i.when.length).length, 0), 0);
const withSlots = index.parts.reduce((n, p) => n + p.groups.reduce((m, g) => m + g.items.filter((i) => i.slots.length).length, 0), 0);

// index.json gọn: ít byte hơn ~4 lần nên đọc + parse trên máy chậm cũng chỉ vài ms.
fs.writeFileSync(path.join(ASSETS, "index.json"), JSON.stringify(index), "utf8");

console.log("bài: " + written + "/" + titles.size + " | còn thiếu nội dung: " + missing.length);
console.log("bài có chỗ điền: " + withSlots + " (" + slotRuns + " field) | bài có nhãn ngày: " + tagged);
for (const key of missing) console.log("  THIEU:", key, "|", titles.get(key));
