import React, { useMemo, useState } from "react";
import { createPortal } from "react-dom";
import {
  DndContext,
  DragEndEvent,
  DragOverlay,
  KeyboardSensor,
  MouseSensor,
  TouchSensor,
  useSensor,
  useSensors
} from "@dnd-kit/core";
import { useDesignerStore } from "../core/store";
import { BedrockForm, JavaMenu } from "../core/types";
import { IconTile } from "../components/IconTile";
import { arrayMove } from "@dnd-kit/sortable";
import { sortableKeyboardCoordinates } from "@dnd-kit/sortable";

export function DndHost({ children }: { children: React.ReactNode }) {
  const { platform, bedrock, java, setBedrock, setJava } = useDesignerStore();
  const [activeId, setActiveId] = React.useState<string | null>(null);
  const [activeLabel, setActiveLabel] = React.useState<string | null>(null);
  const [activeMaterial, setActiveMaterial] = React.useState<string | null>(null);
  const [activeIconUrl, setActiveIconUrl] = useState<string | null>(null);

  const sensors = useSensors(
    useSensor(MouseSensor, { activationConstraint: { distance: 6 } }),
    useSensor(TouchSensor, { activationConstraint: { delay: 150, tolerance: 5 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates })
  );

  const overlay = useMemo(() => {
    if (activeMaterial) {
      return <IconTile id={activeMaterial} name={activeMaterial} />;
    }
    if (activeLabel) {
      return (
        <div className="px-2 py-1 bg-gray-700 rounded text-xs border border-gray-500">
          {activeLabel}
        </div>
      );
    }
    return null;
  }, [activeLabel, activeMaterial]);

  const onDragEnd = (event: DragEndEvent) => {
    const over = event.over?.id?.toString();
    const active = event.active.id?.toString();
    const data = event.active.data.current as any;
    setActiveId(null);
    setActiveLabel(null);
    setActiveMaterial(null);
    if (!over) return;
    if (!active) return;

    if (platform === "bedrock" && bedrock) {
      if (active.startsWith("bedrock-button-") && over.startsWith("bedrock-button-") && bedrock.type !== "MODAL") {
        const activeKey = active.replace("bedrock-button-", "");
        const overKey = over.replace("bedrock-button-", "");
        const from = (bedrock.buttons ?? []).findIndex((b: any) => b.id === activeKey);
        const to = (bedrock.buttons ?? []).findIndex((b: any) => b.id === overKey);
        if (from !== -1 && to !== -1 && from !== to) {
          const buttons = arrayMove(bedrock.buttons ?? [], from, to);
          setBedrock({ ...(bedrock as any), buttons } as BedrockForm);
        }
        return;
      }
      if (active.startsWith("bedrock-component-") && over.startsWith("bedrock-component-") && bedrock.type === "CUSTOM") {
        const activeKey = active.replace("bedrock-component-", "");
        const overKey = over.replace("bedrock-component-", "");
        const from = (bedrock.components ?? []).findIndex((c: any) => c.id === activeKey);
        const to = (bedrock.components ?? []).findIndex((c: any) => c.id === overKey);
        if (from !== -1 && to !== -1 && from !== to) {
          const components = arrayMove(bedrock.components ?? [], from, to);
          setBedrock({ ...(bedrock as any), components } as BedrockForm);
        }
        return;
      }
    }

    if (platform === "bedrock" && bedrock) {
      const t = data?.type as string | undefined;
      if (!t) return;
      if (over === "bedrock-buttons" && (bedrock.type === "SIMPLE" || bedrock.type === "MODAL") && t === "button") {
        const buttons = [
          ...(bedrock.buttons ?? []),
          { id: `button_${(bedrock.buttons ?? []).length + 1}`, text: `Button ${(bedrock.buttons ?? []).length + 1}` }
        ];
        setBedrock({ ...(bedrock as any), buttons } as BedrockForm);
      }
      if (over === "bedrock-components" && bedrock.type === "CUSTOM") {
        const id = `component_${(bedrock.components ?? []).length + 1}`;
        const props = defaultBedrockComponentProps(t);
        const components = [
          ...(bedrock.components ?? []),
          { id, type: t, props }
        ];
        setBedrock({ ...(bedrock as any), components } as BedrockForm);
      }
      return;
    }

    if (platform === "java" && java) {
      const material = data?.material as string | undefined;
      if (!material) return;
      if (!over.startsWith("java-slot-")) return;
      const slot = Number(over.replace("java-slot-", ""));
      if (Number.isNaN(slot)) return;
      if (java.type === "ANVIL" && (slot < 0 || slot > 2)) return;
      if (java.type === "CHEST") {
        const max = (java.size ?? 9) - 1;
        if (slot < 0 || slot > max) return;
      }
      const withoutSlot = java.items.filter((i) => i.slot !== slot);
      const items = [...withoutSlot, { slot, material }];
      setJava({ ...(java as JavaMenu), items });
    }
  };

  return (
    <DndContext
      sensors={sensors}
      onDragStart={(e) => {
        setActiveId(e.active.id.toString());
        const d = e.active.data.current as any;
        setActiveLabel(d?.label ?? null);
        setActiveMaterial(d?.material ?? null);
      }}
      onDragEnd={onDragEnd}
      onDragCancel={() => {
        setActiveId(null);
        setActiveLabel(null);
        setActiveMaterial(null);
      }}
    >
      {children}
      {createPortal(
        <DragOverlay dropAnimation={null} zIndex={9999}>
          {activeId && (
            <div className="pointer-events-none">
              {overlay}
            </div>
          )}
        </DragOverlay>,
        document.body
      )}
    </DndContext>
  );
}

function defaultBedrockComponentProps(type: string) {
  if (type === "label") return { text: "Label" };
  if (type === "input") return { text: "Input", placeholder: "Type here...", default: "" };
  if (type === "dropdown") return { text: "Dropdown", options: ["Option 1", "Option 2"], default: 0 };
  if (type === "toggle") return { text: "Toggle", default: false };
  if (type === "slider") return { text: "Slider", min: 0, max: 10, step: 1, default: 5 };
  if (type === "stepper") return { text: "Stepper", steps: ["A", "B", "C"], default: 0 };
  return {};
}

