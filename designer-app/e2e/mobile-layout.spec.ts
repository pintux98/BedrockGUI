import { test, expect, type Page } from "@playwright/test";

test.describe("mobile layout", () => {
  test.use({ viewport: { width: 390, height: 844 } });

  async function dismissMobileWarning(page: Page) {
    const btn = page.getByRole("button", { name: "Continue Anyway" });
    if (await btn.isVisible().catch(() => false)) {
      await btn.click();
    }
  }

  test("fits viewport without horizontal overflow", async ({ page }) => {
    await page.goto("http://localhost:5173/");
    await dismissMobileWarning(page);
    await expect(page.getByRole("button", { name: "Open menu" })).toBeVisible();
    await page.getByRole("button", { name: "Open menu" }).click();
    await expect(page.getByRole("button", { name: "Export" })).toBeVisible();
    await expect(page.getByText("Tools")).toBeVisible();
    await expect(page.getByText("Canvas")).toBeVisible();
    await expect(page.getByText("Props")).toBeVisible();

    const metrics = await page.evaluate(() => {
      const root = document.getElementById("root");
      const rootH = root ? root.getBoundingClientRect().height : -1;
      const innerH = window.innerHeight;
      const vw = document.documentElement.clientWidth;
      const sw = document.documentElement.scrollWidth;
      return { rootH, innerH, vw, sw };
    });
    const offenders = await page.evaluate(() => {
      const vw = document.documentElement.clientWidth;
      const all = Array.from(document.querySelectorAll<HTMLElement>("body *"));
      const ranked = all
        .map((el) => {
          const r = el.getBoundingClientRect();
          const leftOverflow = Math.max(0, Math.round(-r.left));
          const rightOverflow = Math.max(0, Math.round(r.right - vw));
          return {
            tag: el.tagName.toLowerCase(),
            id: el.id,
            cls: el.className ? String(el.className) : "",
            right: Math.round(r.right),
            left: Math.round(r.left),
            width: Math.round(r.width),
            overflow: Math.max(leftOverflow, rightOverflow)
          };
        })
        .filter((x) => x.overflow > 1)
        .sort((a, b) => b.overflow - a.overflow)
        .slice(0, 5);
      return ranked;
    });
    expect(metrics.sw, JSON.stringify({ metrics, offenders })).toBeLessThanOrEqual(metrics.vw + 1);
    expect(metrics.rootH).toBeLessThanOrEqual(metrics.innerH + 2);
  });

  test("project menu opens and shows save actions", async ({ page }) => {
    await page.goto("http://localhost:5173/");
    await dismissMobileWarning(page);
    await page.getByRole("button", { name: /example/i }).click();
    await expect(page.getByText("Saved Projects", { exact: true })).toBeVisible();
    await expect(page.getByRole("button", { name: /save/i })).toBeVisible();
  });

  test("props tab keeps YAML visible on short screens", async ({ page }) => {
    await page.goto("http://localhost:5173/");
    await dismissMobileWarning(page);
    await page.getByText("Props").click();
    await expect(page.getByText("Form YAML")).toBeVisible();
  });
});

test.describe("tablet and small laptop topbar", () => {
  async function dismissMobileWarning(page: Page) {
    const btn = page.getByRole("button", { name: "Continue Anyway" });
    if (await btn.isVisible().catch(() => false)) {
      await btn.click();
    }
  }

  test("tablet width has no horizontal overflow", async ({ page }) => {
    await page.setViewportSize({ width: 820, height: 1180 });
    await page.goto("http://localhost:5173/");
    await dismissMobileWarning(page);
    await expect(page.getByRole("button", { name: /open menu/i })).toBeVisible();
    const ok = await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth + 1);
    expect(ok).toBe(true);
  });

  test("small laptop hides external links and keeps overflow menu", async ({ page }) => {
    await page.setViewportSize({ width: 1100, height: 800 });
    await page.goto("http://localhost:5173/");
    await dismissMobileWarning(page);
    await expect(page.locator('a[title="Documentation"]').first()).toBeHidden();
    await page.getByRole("button", { name: /open menu/i }).click();
    await expect(page.getByText("Links")).toBeVisible();
  });
});

