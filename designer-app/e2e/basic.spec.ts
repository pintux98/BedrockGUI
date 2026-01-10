import { test, expect } from "@playwright/test";

test("loads designer app", async ({ page }) => {
  await page.goto("http://localhost:5173/");
  const title = page.locator("text=BedrockGUI Designer");
  await expect(title).toBeVisible();
});

