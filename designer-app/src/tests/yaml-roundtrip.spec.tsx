import React from "react";
import { describe, expect, it, beforeEach, afterEach } from "vitest";
import { render, fireEvent, cleanup, screen } from "@testing-library/react";
import { useDesignerStore } from "../core/store";
import { YamlEditorPanel } from "../panels/YamlEditorPanel";
import { DndContext } from "@dnd-kit/core";

function wrap(ui: React.ReactElement) {
  return render(<DndContext>{ui}</DndContext>);
}

beforeEach(() => {
  useDesignerStore.setState({
    configVersion: "1.0.0",
    menuName: "example",
    platform: "bedrock",
    bedrock: {
      type: "SIMPLE",
      title: "Example Form",
      content: "Content",
      command: "/example",
      commandIntercept: "/example",
      buttons: [
        {
          id: "button_1",
          text: "Click me",
          showCondition: "permission:bedrockgui.use",
          alternativeText: "No perms",
          conditions: [{ id: "c1", condition: "permission:bedrockgui.use", property: "text", value: "Has perms" }]
        }
      ]
    },
    globalActions: [{ id: "raw", params: 'message {\n  - "Hello"\n}', raw: 'message {\n  - "Hello"\n}' }],
    java: {
      type: "CHEST",
      title: "Menu",
      size: 27,
      items: [{ slot: 0, material: "STONE" }],
      fills: [
        {
          id: "fill_1",
          type: "ROW",
          row: 1,
          item: { material: "GRAY_STAINED_GLASS_PANE" }
        }
      ]
    }
  } as any);
});

afterEach(() => cleanup());

describe("yaml roundtrip", () => {
  it("exports new fields into YAML snippet", () => {
    wrap(<YamlEditorPanel />);
    const textarea = screen.getByRole("textbox") as HTMLTextAreaElement;
    expect(textarea.value).toContain("command_intercept");
    expect(textarea.value).toContain("global_actions");
    expect(textarea.value).toContain("show_condition");
    expect(textarea.value).toContain("fill");
  });

  it("imports new fields from YAML snippet into store", () => {
    wrap(<YamlEditorPanel />);
    const textarea = screen.getByRole("textbox") as HTMLTextAreaElement;
    const yaml = [
      "bedrock:",
      "  type: SIMPLE",
      "  title: Example Form",
      "  command: /example",
      "  command_intercept: /example",
      "  description: Content",
      "  global_actions:",
      "    - 'message {",
      '      - \"Hello\"',
      "      }'",
      "  buttons:",
      "    button_1:",
      "      text: Click me",
      "      show_condition: permission:bedrockgui.use",
      "      alternative_text: No perms",
      "      conditions:",
      "        c1:",
      "          condition: permission:bedrockgui.use",
      "          property: text",
      "          value: Has perms",
      "java:",
      "  type: CHEST",
      "  title: Menu",
      "  size: 27",
      "  fill:",
      "    type: ROW",
      "    row: 1",
      "    item:",
      "      material: GRAY_STAINED_GLASS_PANE",
      "  items:",
      "    0:",
      "      material: STONE",
      ""
    ].join("\n");
    fireEvent.change(textarea, { target: { value: yaml } });
    fireEvent.click(screen.getByText("Apply Changes"));

    const st = useDesignerStore.getState() as any;
    expect(st.bedrock.commandIntercept).toBe("/example");
    expect(st.globalActions?.length).toBe(1);
    expect(st.bedrock.buttons[0].showCondition).toBe("permission:bedrockgui.use");
    expect(st.bedrock.buttons[0].conditions?.[0]?.id).toBe("c1");
    expect(st.java.fills?.[0]?.type).toBe("ROW");
    expect(st.java.fills?.[0]?.row).toBe(1);
  });
});

