import { test, expect } from "@playwright/test";

test("loads designer app", async ({ page }) => {
  await page.goto("http://localhost:5173/");
  await expect(page.locator("text=BEDROCKGUI").first()).toBeVisible();
  await expect(page.getByRole("button", { name: "Export" })).toBeVisible();
});

