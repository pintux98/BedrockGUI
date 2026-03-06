import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import fs from "node:fs";
import path from "node:path";

function buildJavaAssetsIndex() {
  const dir = path.resolve(process.cwd(), "javaAssets");
  const files = fs.readdirSync(dir).filter((f) => f.toLowerCase().endsWith(".png"));
  const map: Record<string, string> = {};
  for (const f of files) {
    const base = f.replace(/\.png$/i, "");
    const m = base.match(/^(.*)_\d\d$/);
    const key = (m ? m[1] : base).toLowerCase();
    if (!map[key]) map[key] = f;
  }
  return map;
}

export default defineConfig({
  publicDir: "javaAssets",
  server: {
    port: 5173,
    open: false
  },
  preview: {
    port: 5174
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes("node_modules")) return;
          if (id.includes("@dnd-kit")) return "dnd";
          if (id.includes("zod")) return "zod";
          if (id.includes("zustand")) return "zustand";
          if (id.includes("js-yaml")) return "yaml";
          return "vendor";
        }
      }
    }
  },
  plugins: [
    react(),
    {
      name: "java-assets-index-json",
      configureServer(server) {
        server.middlewares.use("/java-assets-index.json", (_req, res) => {
          try {
            const map = buildJavaAssetsIndex();
            res.statusCode = 200;
            res.setHeader("content-type", "application/json; charset=utf-8");
            res.end(JSON.stringify(map));
          } catch (e: any) {
            res.statusCode = 500;
            res.setHeader("content-type", "application/json; charset=utf-8");
            res.end(JSON.stringify({ error: String(e?.message ?? e) }));
          }
        });
      },
      generateBundle() {
        const map = buildJavaAssetsIndex();
        this.emitFile({
          type: "asset",
          fileName: "java-assets-index.json",
          source: JSON.stringify(map)
        });
      }
    }
  ]
});

