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

    it("renders Minecraft color codes in title and button text", () => {
      const form: any = {
        type: "SIMPLE",
        title: "&aGreen Title",
        content: "",
        buttons: [{ id: "b1", text: "&#ff00ffPink" }]
      };
      wrap(<BedrockPreview form={form} />);
      expect(screen.getByText("Green Title")).toHaveStyle({ color: "#55FF55" });
      expect(screen.getByText("Pink")).toHaveStyle({ color: "#FF00FF" });
    });
  });

  describe("JavaPreview", () => {
    it("renders CHEST menu correctly", () => {
      const menu: any = {
        type: "CHEST",
        title: "CHEST Menu",
        size: 27,
        items: [{ slot: 0, material: "STONE" }]
      };
      const view = wrap(<JavaPreview menu={menu} />);
      expect(view.getByText("CHEST Menu")).toBeDefined();
      expect(view.getByTitle("STONE")).toBeDefined();
      view.unmount();
    });

    it("renders null if menu is undefined", () => {
      const { container } = render(<JavaPreview menu={undefined as any} />);
      expect(container.innerHTML).toBe("");
    });
  });
});
