import fs from "node:fs/promises";
import path from "node:path";

const assetsDir = path.resolve(process.cwd(), "dist", "assets");
const maxJs = Number(process.env.BUDGET_MAX_JS ?? 300_000);
const maxCss = Number(process.env.BUDGET_MAX_CSS ?? 80_000);

const entries = await fs.readdir(assetsDir, { withFileTypes: true }).catch(() => []);
const files = entries.filter((e) => e.isFile()).map((e) => e.name);

const sizes = await Promise.all(
  files.map(async (name) => {
    const p = path.join(assetsDir, name);
    const st = await fs.stat(p);
    return { name, bytes: st.size };
  })
);

const js = sizes.filter((x) => x.name.endsWith(".js")).sort((a, b) => b.bytes - a.bytes);
const css = sizes.filter((x) => x.name.endsWith(".css")).sort((a, b) => b.bytes - a.bytes);

function fmt(n) {
  return `${(n / 1024).toFixed(2)} kB`;
}

if (js[0]) console.log(`largest js: ${js[0].name} (${fmt(js[0].bytes)})`);
if (css[0]) console.log(`largest css: ${css[0].name} (${fmt(css[0].bytes)})`);

const tooLarge = [
  ...js.filter((x) => x.bytes > maxJs).map((x) => ({ ...x, limit: maxJs, kind: "js" })),
  ...css.filter((x) => x.bytes > maxCss).map((x) => ({ ...x, limit: maxCss, kind: "css" }))
];

if (tooLarge.length) {
  for (const x of tooLarge) console.error(`${x.kind} over budget: ${x.name} (${fmt(x.bytes)} > ${fmt(x.limit)})`);
  process.exit(1);
}

