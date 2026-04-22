import { describe, it, expect } from "vitest";
import { designerSchema } from "../core/schemas";

describe("designerSchema", () => {
  it("valid SIMPLE bedrock form", () => {
    const parsed = designerSchema.parse({
      configVersion: "1.0.0",
      menuName: "test",
      platform: "bedrock",
      bedrock: {
        type: "SIMPLE",
        title: "Title",
        buttons: [{ id: "a", text: "A" }]
      }
    });
    expect(parsed.menuName).toBe("test");
  });
});

