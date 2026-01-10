import React from "react";
import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { BedrockPreview } from "../canvas/previews/BedrockPreview";
import { JavaPreview } from "../canvas/previews/JavaPreview";
import { DndContext } from "@dnd-kit/core";

function wrap(ui: React.ReactElement) {
  return render(<DndContext>{ui}</DndContext>);
}

describe("Preview Components", () => {
  describe("BedrockPreview", () => {
    it("renders CUSTOM form without crashing", () => {
      const form: any = {
        type: "CUSTOM",
        title: "Custom Form",
        components: []
      };
      wrap(<BedrockPreview form={form} />);
      expect(screen.getByText("Custom Form")).toBeDefined();
    });

    it("renders SIMPLE form with undefined buttons without crashing", () => {
      const form: any = {
        type: "SIMPLE",
        title: "Simple Form",
        buttons: undefined
      };
      wrap(<BedrockPreview form={form} />);
      expect(screen.getByText("Simple Form")).toBeDefined();
    });

    it("renders MODAL form correctly", () => {
      const form: any = {
        type: "MODAL",
        title: "Modal Form",
        content: "Are you sure?",
        buttons: [{ id: "yes", text: "Yes" }]
      };
      wrap(<BedrockPreview form={form} />);
      expect(screen.getByText("Modal Form")).toBeDefined();
      expect(screen.getByText("Yes")).toBeDefined();
    });
  });

  describe("JavaPreview", () => {
    it("renders CHEST menu correctly", () => {
      const menu: any = {
        type: "CHEST",
        title: "Chest Menu",
        size: 27,
        items: [{ slot: 0, material: "STONE" }]
      };
      wrap(<JavaPreview menu={menu} />);
      expect(screen.getByText("Chest Menu")).toBeDefined();
      expect(screen.getByTitle("STONE")).toBeDefined();
    });

    it("renders ANVIL menu correctly", () => {
      const menu: any = {
        type: "ANVIL",
        title: "Anvil Menu",
        items: [{ slot: 0, material: "PAPER" }]
      };
      wrap(<JavaPreview menu={menu} />);
      expect(screen.getByText("Anvil Menu")).toBeDefined();
      expect(screen.getByTitle("PAPER")).toBeDefined();
    });

    it("renders CRAFTING menu correctly", () => {
      const menu: any = {
        type: "CRAFTING",
        title: "Crafting Menu",
        items: [{ slot: 1, material: "LOG" }, { slot: 0, material: "PLANKS" }]
      };
      wrap(<JavaPreview menu={menu} />);
      expect(screen.getByText("Crafting Menu")).toBeDefined();
      expect(screen.getByTitle("LOG")).toBeDefined();
      expect(screen.getByTitle("PLANKS")).toBeDefined();
    });

    it("renders null if menu is undefined", () => {
      const { container } = render(<JavaPreview menu={undefined as any} />);
      expect(container.innerHTML).toBe("");
    });
  });
});
