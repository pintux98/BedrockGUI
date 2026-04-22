import { describe, expect, it } from "vitest";
import { parseMinecraftText, stripMinecraftCodes } from "../core/minecraftText";

describe("minecraftText", () => {
  it("parses legacy & color codes", () => {
    const segs = parseMinecraftText("&aHello");
    expect(stripMinecraftCodes("&aHello")).toBe("Hello");
    expect(segs.some((s) => s.style.color === "#55FF55")).toBe(true);
  });

  it("parses legacy § color codes", () => {
    expect(stripMinecraftCodes("§cHi")).toBe("Hi");
    const segs = parseMinecraftText("§cHi");
    expect(segs.some((s) => s.style.color === "#FF5555")).toBe(true);
  });

  it("parses direct #RRGGBB hex colors", () => {
    const segs = parseMinecraftText("#ff00ffHex");
    expect(stripMinecraftCodes("#ff00ffHex")).toBe("Hex");
    expect(segs.some((s) => s.style.color === "#FF00FF")).toBe(true);
  });

  it("parses &#RRGGBB hex colors", () => {
    const segs = parseMinecraftText("&#00ff00Green");
    expect(stripMinecraftCodes("&#00ff00Green")).toBe("Green");
    expect(segs.some((s) => s.style.color === "#00FF00")).toBe(true);
  });

  it("parses §x style hex sequences", () => {
    const text = "§x§F§F§0§0§F§FHex";
    const segs = parseMinecraftText(text);
    expect(stripMinecraftCodes(text)).toBe("Hex");
    expect(segs.some((s) => s.style.color === "#FF00FF")).toBe(true);
  });

  it("treats double && and §§ as literal characters", () => {
    expect(stripMinecraftCodes("&&aTest")).toBe("&aTest");
    expect(stripMinecraftCodes("§§cTest")).toBe("§cTest");
  });
});
