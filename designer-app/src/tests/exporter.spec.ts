import { describe, it, expect } from "vitest";
import { useDesignerStore } from "../core/store";
import { stateToYaml } from "../core/yaml";
import yaml from "js-yaml";

describe("exporter", () => {
  it("exports inline forms YAML without forms key", () => {
    const state = useDesignerStore.getState();
    const out = stateToYaml(state);
    
    expect(out).not.toContain("forms:");
    expect(out).toContain("configVersion: '1.0.0'");
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
    expect(out).toContain("image: 'http://example.com/image.png'");
    expect(out).not.toContain("image: >-");
  });
});

