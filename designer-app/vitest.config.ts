import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    include: ["src/tests/**/*.spec.{ts,tsx}"],
    environment: "jsdom",
    setupFiles: ["src/tests/setup.ts"]
  }
});

