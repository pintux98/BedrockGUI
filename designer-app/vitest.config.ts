import { defineConfig } from "vitest/config";

export default defineConfig({
  resolve: {
    alias: {
      "virtual:java-assets-index": "/src/tests/stubs/java-assets-index.ts"
    }
  },
  test: {
    include: ["src/tests/**/*.spec.{ts,tsx}"],
    environment: "jsdom",
    setupFiles: ["src/tests/setup.ts"]
  }
});

