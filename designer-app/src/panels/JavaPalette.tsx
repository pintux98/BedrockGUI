import React, { useMemo, useState } from "react";
import { useDesignerStore } from "../core/store";
import { IconTile } from "../components/IconTile";
import { useDraggable } from "@dnd-kit/core";
import JAVA_ASSETS from "virtual:java-assets-index";

export function JavaPalette() {
  const { java, setJava, selectedJavaSlot } = useDesignerStore();
  const [query, setQuery] = useState("");
  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    const entries = Object.entries(JAVA_ASSETS).map(([base, file]) => {
      const id = base.toUpperCase();
      const name = titleCase(base.replace(/_/g, " "));
      return { base, id, name, file };
    });
    const out = q
      ? entries.filter((e) => e.base.includes(q) || e.name.toLowerCase().includes(q))
      : entries;
    return out.slice(0, 400);
  }, [query]);
  return (
    <div className="ui-panel h-full flex flex-col">
      <div className="ui-panel-title">Java Palette</div>
      <div className="flex gap-2 mb-2">
        <input
          className="ui-input"
          placeholder="Search items"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
      </div>
      <div className="flex-1 overflow-y-auto custom-scrollbar pr-1">
        <div className="grid grid-cols-5 gap-1">
          {filtered.map((m) => (
            <DraggableMaterial
              key={m.id}
              id={m.id}
              name={m.name}
              iconUrl={`/${m.file}`}
              onClick={() => {
                if (!java) return;
                
                // If a specific slot is selected, fill it
                if (selectedJavaSlot !== null) {
                  const items = java.items.filter((i) => i.slot !== selectedJavaSlot);
                  items.push({ slot: selectedJavaSlot, material: m.id, amount: 1 });
                  setJava({ ...java, items });
                  return;
                }

                // Otherwise find first empty slot
                const usedSlots = java.items.map((i) => i.slot);
                let slot = 0;
                while (usedSlots.includes(slot)) slot++;
                setJava({
                  ...java,
                  items: [...java.items, { slot, material: m.id }]
                });
              }}
            />
          ))}
        </div>
      </div>
    </div>
  );
}

function DraggableMaterial({
  id,
  name,
  iconUrl,
  onClick
}: {
  id: string;
  name: string;
  iconUrl?: string;
  onClick: () => void;
}) {
  const { attributes, listeners, setNodeRef } = useDraggable({
    id: `mat-${id}`,
    data: { material: id, label: name, iconUrl }
  });
  return (
    <div ref={setNodeRef} {...attributes} {...listeners} style={{ touchAction: "none" }}>
      <IconTile id={id} name={name} iconUrl={iconUrl} onClick={onClick} />
    </div>
  );
}

function titleCase(s: string) {
  return s
    .split(" ")
    .filter(Boolean)
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(" ");
}
