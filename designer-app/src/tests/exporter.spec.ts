import { describe, it, expect } from "vitest";
import { useDesignerStore } from "../core/store";
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
});

