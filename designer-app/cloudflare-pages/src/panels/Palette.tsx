import React from "react";
import { useDraggable } from "@dnd-kit/core";
import { useDesignerStore } from "../core/store";

const items = [
  { id: "btn", label: "Button", data: { type: "button", label: "Button" } },
  { id: "label", label: "Label", data: { type: "label" } },
  { id: "input", label: "Input", data: { type: "input" } },
  { id: "dropdown", label: "Dropdown", data: { type: "dropdown" } },
  { id: "toggle", label: "Toggle", data: { type: "toggle" } },
  { id: "slider", label: "Slider", data: { type: "slider" } }
];

export function Palette() {
  const { platform, bedrock } = useDesignerStore();
  const disabled = platform === "bedrock" && bedrock?.type === "MODAL";
  const visibleItems =
    platform === "bedrock" && bedrock?.type === "SIMPLE"
      ? items.filter((i) => i.id === "btn")
      : items;
  return (
    <div className="ui-panel h-full">
      <div className="ui-panel-title">Palette</div>
      <div className="grid grid-cols-2 gap-2">
        {visibleItems.map((i) => (
          <Draggable key={i.id} id={i.id} label={i.label} data={i.data} disabled={disabled} />
        ))}
      </div>
    </div>
  );
}

function Draggable({
  id,
  label,
  data,
  disabled
}: {
  id: string;
  label: string;
  data: Record<string, unknown>;
  disabled?: boolean;
}) {
  const { attributes, listeners, setNodeRef } = useDraggable({
    id,
    data: { ...data, label }
  });
  return (
    <button
      ref={setNodeRef as any}
      {...attributes}
      {...(disabled ? {} : listeners)}
      type="button"
      aria-label={label}
      className={`ui-btn-secondary w-full ${disabled ? "opacity-50 cursor-not-allowed" : "cursor-move"}`}
      style={{ touchAction: "none" }}
    >
      {label}
    </button>
  );
}
