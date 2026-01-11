import { describe, it, expect } from "vitest";
import { useDesignerStore } from "../core/store";
import { stateToYaml } from "../core/yaml";
import yaml from "js-yaml";

describe("exporter", () => {
  it("exports inline forms YAML", () => {
    const state = useDesignerStore.getState();
    const forms: Record<string, unknown> = {};
    const key = state.menuName;
    forms[key] = {
      bedrock: {
        type: state.bedrock?.type,
        title: state.bedrock?.title
      }
    };
    const doc = yaml.dump({ forms });
    expect(doc).toContain("forms:");
    expect(doc).toContain(state.menuName);
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

