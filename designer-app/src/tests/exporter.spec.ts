import { describe, it, expect } from "vitest";
import { useDesignerStore } from "../core/store";
import { stateToYaml } from "../core/yaml";
import yaml from "js-yaml";

describe("exporter", () => {
  it("exports inline forms YAML without forms key", () => {
    const state = useDesignerStore.getState();
    const out = stateToYaml(state);
    
    expect(out).not.toContain("forms:");
    expect(out).toMatch(/configVersion:\s+["']1\.0\.0["']/);
    // Should contain bedrock or java key depending on default state
    expect(out).toMatch(/(bedrock|java):/);
  });

  it("does not use block scalars for image urls", () => {
    const state = useDesignerStore.getState();
    // Reset state before setting it to ensure clean slate
    state.setBedrock({
      type: "SIMPLE",
      title: "Test",
      buttons: [{ id: "btn1", text: "Click", image: "http://example.com/image.png" }]
    });
    // Ensure state update is reflected
    const updatedState = useDesignerStore.getState();
    const out = stateToYaml(updatedState);
    expect(out).not.toContain("image: >-");
    expect(out).toMatch(/image:\s+["']http:\/\/example\.com\/image\.png["']/);
  });

  it("preserves multiline action formatting", () => {
    const state = useDesignerStore.getState();
    state.setBedrock({
      type: "SIMPLE",
      title: "Test",
      buttons: [
        {
          id: "btn1",
          text: "Click",
          onClick: [{ id: "raw", params: 'message {\n  - "Hello"\n}', raw: 'message {\n  - "Hello"\n}' }]
        }
      ]
    } as any);
    const out = stateToYaml(useDesignerStore.getState() as any);
    expect(out).toMatch(/onClick:\n\s+- \|-/);
    expect(out).toContain("message {");
    expect(out).toContain('  - "Hello"');
  });
});

