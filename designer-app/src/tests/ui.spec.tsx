import React from "react";
import { describe, expect, it, beforeEach, afterEach } from "vitest";
import { render, fireEvent, screen, cleanup } from "@testing-library/react";
import { DndContext } from "@dnd-kit/core";
import { useDesignerStore } from "../core/store";
import { PropertiesPanel } from "../panels/PropertiesPanel";
import { StyleGuidePanel } from "../panels/StyleGuidePanel";
import { TopBar } from "../app/TopBar";

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
      buttons: [{ id: "button_1", text: "Click me" }]
    },
    java: undefined,
    globalActions: undefined,
    dirty: false,
    selectedJavaSlot: null,
    selectedBedrockButtonId: null,
    selectedBedrockComponentId: null
  } as any);
});

describe("ui panels", () => {
  afterEach(() => cleanup());

  it("renders style guide panel", () => {
    wrap(<StyleGuidePanel />);
    expect(screen.getByText("UI Guide")).toBeInTheDocument();
    expect(screen.getByText("Buttons")).toBeInTheDocument();
    expect(screen.getByText("Primary")).toBeInTheDocument();
  });

  it("renders top bar without crashing", () => {
    wrap(<TopBar />);
    expect(screen.getByText("BEDROCK")).toBeInTheDocument();
    expect(screen.getByText("GUI")).toBeInTheDocument();
    expect(screen.getByText("Export")).toBeInTheDocument();
  });

  it("action editor adds an action and updates store", () => {
    wrap(<PropertiesPanel />);
    fireEvent.click(screen.getAllByText("Add action")[0]);
    const textarea = screen.getAllByPlaceholderText("one line per row")[0] as HTMLTextAreaElement;
    fireEvent.change(textarea, { target: { value: "Hello" } });
    fireEvent.blur(textarea); // Trigger commit
    const st = useDesignerStore.getState();
    expect(st.bedrock?.type).toBe("SIMPLE");
    const actions = (st.bedrock as any).buttons[0].onClick;
    expect(Array.isArray(actions)).toBe(true);
    expect(actions[0].raw).toContain("message");
    expect(actions[0].raw).toContain("Hello");
  });

  it("java lore add line updates item lore", () => {
    useDesignerStore.setState({
      platform: "java",
      java: { type: "CHEST", title: "Menu", size: 27, items: [{ slot: 0, material: "STONE", lore: [] }] },
      bedrock: undefined,
      selectedJavaSlot: 0
    } as any);
    wrap(<PropertiesPanel />);
    fireEvent.click(screen.getAllByText("Add lore line")[0]);
    const st = useDesignerStore.getState();
    const item = st.java?.items.find((i) => i.slot === 0);
    expect(item?.lore?.length).toBe(1);
  });

  it("java fills add creates a fill rule", () => {
    useDesignerStore.setState({
      platform: "java",
      java: { type: "CHEST", title: "Menu", size: 27, items: [] },
      bedrock: undefined,
      selectedJavaSlot: null
    } as any);
    wrap(<PropertiesPanel />);
    fireEvent.click(screen.getByText("Add"));
    const st = useDesignerStore.getState() as any;
    expect(Array.isArray(st.java.fills)).toBe(true);
    expect(st.java.fills.length).toBe(1);
    expect(st.java.fills[0].type).toBe("EMPTY");
  });
});

