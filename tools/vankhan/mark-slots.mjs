import fs from "node:fs";
import path from "node:path";

/**
 * Đánh dấu mọi chỗ người cúng phải điền trong corpus bằng marker {{field}}.
 * Luật xét phần chữ ngay trước khoảng chấm; luật cụ thể đặt trước luật chung.
 * Chạy: node tools/vankhan/mark-slots.mjs [--dry]
 */
const CORPUS = "D:/android/calendar/tools/vankhan/corpus";
const DRY = process.argv.includes("--dry");

const RULES = [
  // Ngày mất (bộ ba "mất ngày … tháng … năm …")
  [/mất\s*\(?\s*ngày\s*$/i, "ngay_mat"],
  [/mất\s*\(?\s*ngày\s+…{2,}\s*tháng\s*$/i, "thang_mat"],
  [/mất\s*\(?\s*ngày\s+…{2,}\s*tháng\s+…{2,}\s*năm\s*$/i, "nam_mat"],
  // Người đã khuất (đặt trước luật "là" chung)
  [/người thân của chúng con là\s*$/i, "huong_linh"],
  [/hương linh\s*$/i, "huong_linh"],
  [/hương hồn\s*$/i, "huong_linh"],
  [/linh vị\s*$/i, "huong_linh"],
  [/tính từ khi\s*$/i, "huong_linh"],
  // Người đứng lễ
  [/(?:tín chủ|gia chủ|chúng con)( con)? là\s*$/i, "ho_ten"],
  [/(?:^|\s)tuổi\s*$/i, "tuoi"],
  [/(làm|hành) nghề\s*$/i, "nghe_nghiep"],
  // Ngày / tháng / năm âm lịch của buổi lễ
  [/(?:^|\s)ngày\s*$/i, "ngay_am"],
  [/(?:^|\s)tháng\s*$/i, "thang_am"],
  [/(?:^|\s)năm\s*$/i, "nam_am"],
  // Người thân và hoàn cảnh buổi lễ
  [/(?:^|\s)cháu\s*$/i, "ten_nguoi_than"],
  [/con \(cháu\) là\s*$/i, "ten_nguoi_than"],
  [/\(con gái\) là\s*$/i, "ten_nguoi_than"],
  [/có người là\s*$/i, "ten_nguoi_than"],
  [/mắc bệnh\s*$/i, "ten_benh"],
  [/con của\s*$/i, "ten_cha"],
  [/(?:^|\s)và\s*$/i, "ten_me"],
  [/kết duyên cùng\s*$/i, "ten_doi_theo"],
  [/đang học tại\s*$/i, "noi_hoc"],
  [/(mở cửa hàng|công ty)(\s*\([^)]*\))?\s*$/i, "ten_co_so"],
  [/điều lo\s*\(?\s*$/i, "dieu_lo"],
  [/đã để\s*$/i, "so_nam"],
  // Địa điểm
  [/an táng tại\s*$/i, "an_tang_tai"],
  [/(cải táng|động đến đất|thửa đất|đất mới) tại\s*$/i, "noi_dat"],
  [/(chùa|điện|đền|phủ|đình|miếu)(\s*\([^)]*\))?\s*$/i, "ten_chua_den"],
  [/(?:^|\s)cho\s*$/i, "huong_linh"],
  [/(?:^|\s)của\s*$/i, "huong_linh"],
  [/(?:^|\s)tại\s*$/i, "dia_chi"],
];

const report = { byField: new Map(), unmatched: [], contexts: new Map() };
const AUDIT = process.argv.includes("--audit");

function classify(before) {
  const tail = before.slice(-60);
  for (const [re, field] of RULES) {
    if (re.test(tail)) return field;
  }
  return null;
}

let changed = 0;
let runs = 0;
for (const file of fs.readdirSync(CORPUS).filter((f) => f.endsWith(".md")).sort()) {
  const full = path.join(CORPUS, file);
  const src = fs.readFileSync(full, "utf8");
  runs += (src.match(/…{2,}/g) || []).length;
  const dst = src.replace(/…{2,}/g, (s, offset, whole) => {
    const field = classify(whole.slice(Math.max(0, offset - 60), offset));
    if (!field) {
      const tail = whole.slice(Math.max(0, offset - 44), offset).replace(/\n/g, " ");
      const head = whole.slice(offset + s.length, offset + s.length + 16).replace(/\n/g, " ");
      report.unmatched.push(file + " | ..." + tail + " «RUN» " + head);
      return s;
    }
    report.byField.set(field, (report.byField.get(field) || 0) + 1);
    const tail = whole.slice(Math.max(0, offset - 34), offset).replace(/\s*\n\s*/g, " ").trim();
    report.contexts.set(field, (report.contexts.get(field) || new Set()).add(tail));
    return "{{" + field + "}}";
  });
  if (dst !== src) {
    changed++;
    if (!DRY) fs.writeFileSync(full, dst, "utf8");
  }
}

console.log(DRY ? "[dry-run] không ghi file" : "đã ghi " + changed + " file corpus");
console.log("tổng khoảng chấm:", runs, "| đã gắn field:", [...report.byField.values()].reduce((a, b) => a + b, 0));
console.log("--- theo field ---");
[...report.byField.entries()].sort((a, b) => b[1] - a[1]).forEach(([f, n]) => console.log(String(n).padStart(4), f));
console.log("--- chưa khớp (" + report.unmatched.length + ") ---");
report.unmatched.forEach((u) => console.log("  ", u));
if (AUDIT) {
  console.log("--- ngữ cảnh theo field ---");
  for (const [field, set] of [...report.contexts.entries()].sort((a, b) => b[1].size - a[1].size)) {
    console.log("## " + field + " (" + report.byField.get(field) + ")");
    [...set].sort().forEach((c) => console.log("    …" + c));
  }
}
