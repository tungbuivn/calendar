// Dọn toàn văn cho dễ đọc thành tiếng (text-to-speech):
// 1) dòng "(3 lần)" -> viết hẳn thành 3 dòng lặp lại;
// 2) bỏ mọi chú thích trong ngoặc "(...)" vì TTS sẽ đọc cả chữ trong ngoặc.
// Chạy trên corpus/*.md, idempotent.
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = path.dirname(fileURLToPath(import.meta.url));
const CORPUS = path.join(ROOT, "corpus");

// Những chỗ bỏ ngoặc trần sẽ mất nghĩa, nên viết lại câu cho liền mạch.
const REWRITES = [
  ["mất (ngày {{ngay_mat}} tháng {{thang_mat}} năm {{nam_mat}}) đến nay",
   "mất ngày {{ngay_mat}} tháng {{thang_mat}} năm {{nam_mat}}, đến nay"],
  ["điều lo ({{dieu_lo}})", "điều lo là {{dieu_lo}}"],
  ["làm lễ cải cát (sang mộ)", "làm lễ cải táng"],
  ["vị tổ của nghề (thuốc, dạy, thợ) ở xứ này", "vị tổ nghề của xứ này"],
  // Mấy chỗ này bỏ ngoặc sẽ thành nói sai cho một trong hai trường hợp, nên viết trung tính.
  ["làm lễ cưới cho con trai (con gái) là {{ten_nguoi_than}}", "làm lễ cưới cho {{ten_nguoi_than}}"],
  ["đương làm lễ tiểu tường (đại tường)", "đương làm lễ hết tang"],
  ["làm lễ tiểu tường (đại tường) cho {{huong_linh}}", "làm lễ hết tang cho {{huong_linh}}"],
  ["Một năm (hai năm) tang chế", "Tròn hạn tang chế"],
  [", tức giỗ đầu (giỗ thứ hai)", ""],
];

const REPEAT = /^(\S.*?[^.\s])\s*\((\d+) lần\)(.*)$/;
const NOTE = /\s*\([^()]*\)/g;

const touched = { repeat: 0, note: 0, rewrite: 0 };
let leftover = [];

for (const file of fs.readdirSync(CORPUS).filter((f) => f.endsWith(".md")).sort()) {
  const lines = fs.readFileSync(path.join(CORPUS, file), "utf8").replace(/\r\n?/g, "\n").split("\n");
  const out = [];
  for (const line of lines) {
    if (line.startsWith("===")) { out.push(line); continue; }
    const m = REPEAT.exec(line);
    if (m) {
      const punct = (/^[.!?…]*/.exec(m[3]) || [""])[0];
      const rest = m[3].slice(punct.length).trim();
      for (let i = 0; i < Number(m[2]); i++) out.push(m[1] + (punct || "."));
      if (rest) out.push(rest);
      touched.repeat++;
      continue;
    }
    let text = line;
    for (const [from, to] of REWRITES) {
      if (text.includes(from)) { text = text.split(from).join(to); touched.rewrite++; }
    }
    const cleaned = text.replace(NOTE, "").replace(/ {2,}/g, " ").replace(/\s+([,.;:!?])/g, "$1");
    if (cleaned !== text) touched.note++;
    if (/\(/.test(cleaned)) leftover.push(file + ": " + cleaned.trim());
    out.push(cleaned);
  }
  fs.writeFileSync(path.join(CORPUS, file), out.join("\n"), "utf8");
}

console.log("dòng lặp đã bung: " + touched.repeat + " | chú thích đã bỏ: " + touched.note +
  " | câu viết lại: " + touched.rewrite);
if (leftover.length) {
  console.log("CÒN NGOẶC (kiểm tra lại):");
  for (const l of leftover) console.log("  " + l);
}
