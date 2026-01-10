import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import fs from "node:fs";
import path from "node:path";

export default defineConfig({
  publicDir: "javaAssets",
  server: {
    port: 5173,
    open: false
  },
  preview: {
    port: 5174
  },
  plugins: [
    react(),
    {
      name: "virtual-java-assets-index",
      resolveId(id) {
        if (id === "virtual:java-assets-index") return id;
        return null;
      },
      load(id) {
        if (id !== "virtual:java-assets-index") return null;
        const dir = path.resolve(process.cwd(), "javaAssets");
        const files = fs.readdirSync(dir).filter((f) => f.toLowerCase().endsWith(".png"));
        const map: Record<string, string> = {};
        for (const f of files) {
          const base = f.replace(/\.png$/i, "");
          const m = base.match(/^(.*)_\d\d$/);
          const key = (m ? m[1] : base).toLowerCase();
          if (!map[key]) map[key] = f;
        }
        return `export default ${JSON.stringify(map)};`;
      }
    }
  ]
});

