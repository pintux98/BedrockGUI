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

  it("bedrock button text supports new lines", () => {
    wrap(<PropertiesPanel />);
    const textarea = screen.getAllByPlaceholderText("text (supports new lines)")[0] as HTMLTextAreaElement;
    fireEvent.change(textarea, { target: { value: "Line1\nLine2" } });
    fireEvent.blur(textarea);
    const st = useDesignerStore.getState() as any;
    expect(st.bedrock.buttons[0].text).toBe("Line1\nLine2");
  });

  it("action editor supports bungee action blocks", () => {
    wrap(<PropertiesPanel />);
    fireEvent.click(screen.getAllByText("Add action")[0]);
    screen.getAllByPlaceholderText("one line per row")[0];
    const select = screen.getAllByRole("combobox").find((el) => (el as HTMLSelectElement).value === "message") as HTMLSelectElement;
    fireEvent.change(select, { target: { value: "bungee" } });
    const sub = screen.getByPlaceholderText('subchannel (e.g. "Connect")') as HTMLInputElement;
    fireEvent.change(sub, { target: { value: "Connect" } });
    fireEvent.blur(sub);
    const args = screen.getByPlaceholderText("args (one per row)") as HTMLTextAreaElement;
    fireEvent.change(args, { target: { value: "lobby" } });
    fireEvent.blur(args);
    const st = useDesignerStore.getState() as any;
    const actions = st.bedrock.buttons[0].onClick;
    expect(actions[0].raw).toContain("bungee");
    expect(actions[0].raw).toContain('subchannel: "Connect"');
    expect(actions[0].raw).toContain('"lobby"');
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

