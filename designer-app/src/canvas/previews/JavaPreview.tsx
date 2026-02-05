import React from "react";
import { JavaMenu } from "../../core/types";
import { useDroppable } from "@dnd-kit/core";
import { useDesignerStore } from "../../core/store";
import JAVA_ASSETS from "virtual:java-assets-index";
import { MinecraftText } from "../../components/MinecraftText";
import { stripMinecraftCodes } from "../../core/minecraftText";

export function JavaPreview({ menu }: { menu: JavaMenu }) {
  const { selectedJavaSlot, setSelectedJavaSlot } = useDesignerStore();
  const slotRefs = React.useRef<Record<number, HTMLButtonElement | null>>({});
  React.useEffect(() => {
    if (selectedJavaSlot === null) return;
    const el = slotRefs.current[selectedJavaSlot];
    el?.scrollIntoView({ block: "center", behavior: "smooth" });
  }, [selectedJavaSlot]);

  if (!menu) return null;

  if (menu.type === "CHEST") {
    const rows = Math.ceil((menu.size ?? 9) / 9);
    const slots = Array.from({ length: rows * 9 }, (_, i) => i);
    const { itemsBySlot, fillSlots } = computeChestPreviewItems(menu, rows);
    return (
      <div
        className="max-w-xl mx-auto shadow-2xl bg-[#c6c6c6] text-[#404040]"
        style={{
          width: "min(520px, 100%)",
          boxShadow: "inset -2px -2px 0px #555555, inset 2px 2px 0px #ffffff, 0 10px 20px rgba(0,0,0,0.5)",
          border: "2px solid #000000"
        }}
        onClick={(e) => {
          if (e.target !== e.currentTarget) return;
          setSelectedJavaSlot(null);
        }}
      >
        <div className="h-10 flex items-center justify-between px-3 text-[#404040]">
          <div className="text-base truncate">{menu.title}</div>
          <button className="text-[#404040] hover:text-red-500 font-bold" type="button" aria-label="Close preview">
            ✕
          </button>
        </div>
        <div className="p-3">
          <div className="grid grid-cols-9 gap-0.5 bg-[#8b8b8b] p-1 border-b border-r border-white border-t-[#373737] border-l-[#373737]">
            {slots.map((slot) => {
              const item = itemsBySlot.get(slot);
              const icon = item ? materialToIconUrl(item.material) : undefined;
              const tooltipTitle = item?.name ?? item?.material ?? `Slot ${slot}`;
              const tooltipLore = Array.isArray(item?.lore) ? item.lore : [];
              return (
                <JavaSlot
                  key={slot}
                  slot={slot}
                  label={item ? item.material : String(slot)}
                  iconUrl={icon}
                  muted={fillSlots.has(slot) && !menu.items.some((it) => it.slot === slot)}
                  tooltipTitle={tooltipTitle}
                  tooltipLore={tooltipLore}
                  registerRef={(el) => {
                    slotRefs.current[slot] = el;
                  }}
                />
              );
            })}
          </div>
        </div>
      </div>
    );
  }
  if (menu.type === "ANVIL") {
    return (
      <div
        className="max-w-md mx-auto shadow-2xl bg-[#c6c6c6] text-[#404040]"
        style={{
          width: "min(520px, 100%)",
          boxShadow: "inset -2px -2px 0px #555555, inset 2px 2px 0px #ffffff, 0 10px 20px rgba(0,0,0,0.5)",
          border: "2px solid #000000"
        }}
        onClick={(e) => {
          if (e.target !== e.currentTarget) return;
          setSelectedJavaSlot(null);
        }}
      >
        <div className="h-10 flex items-center justify-between px-3 text-[#404040]">
          <div className="text-base truncate">{menu.title}</div>
          <button className="text-[#404040] hover:text-red-500 font-bold" type="button" aria-label="Close preview">
            ✕
          </button>
        </div>
        <div className="p-3">
          <div className="text-xs text-[#2b2b2b] mb-2 font-minecraft">Name</div>
          <input
            className="w-full bg-white border-2 border-[#8b8b8b] px-3 py-2 text-sm text-[#1f2937] mb-3 font-minecraft shadow-inner outline-none focus:border-brand-accent"
            placeholder="Item name..."
            defaultValue=""
          />
          <div className="grid grid-cols-3 gap-2 bg-[#8b8b8b] p-2 border-b border-r border-white border-t-[#373737] border-l-[#373737]">
            {[0, 1, 2].map((slot) => {
              const item = menu.items.find((it) => it.slot === slot);
              const icon = item ? materialToIconUrl(item.material) : undefined;
              const tooltipTitle = item?.name ?? item?.material ?? `Slot ${slot}`;
              const tooltipLore = Array.isArray(item?.lore) ? item.lore : [];
              return (
                <JavaSlot
                  key={slot}
                  slot={slot}
                  label={item ? item.material : String(slot)}
                  iconUrl={icon}
                  tall
                  tooltipTitle={tooltipTitle}
                  tooltipLore={tooltipLore}
                  registerRef={(el) => {
                    slotRefs.current[slot] = el;
                  }}
                />
              );
            })}
          </div>
        </div>
      </div>
    );
  }
  
  // CRAFTING and others
  return (
    <div
      className="max-w-md mx-auto shadow-2xl bg-[#c6c6c6] text-[#404040]"
      style={{
        width: "min(520px, 100%)",
        boxShadow: "inset -2px -2px 0px #555555, inset 2px 2px 0px #ffffff, 0 10px 20px rgba(0,0,0,0.5)",
          border: "2px solid #000000"
      }}
      onClick={(e) => {
        if (e.target !== e.currentTarget) return;
        setSelectedJavaSlot(null);
      }}
    >
      <div className="h-10 flex items-center justify-between px-3 text-[#404040]">
        <div className="text-base truncate">{menu.title}</div>
        <button className="text-[#404040] hover:text-red-500 font-bold" type="button" aria-label="Close preview">
          ✕
        </button>
      </div>
      <div className="p-3">
        {menu.type === "CRAFTING" ? (
          <div className="flex items-center gap-4 justify-center bg-[#c6c6c6]">
            <div className="grid grid-cols-3 gap-0.5 bg-[#8b8b8b] p-1 border-b border-r border-white border-t-[#373737] border-l-[#373737]">
              {[1, 2, 3, 4, 5, 6, 7, 8, 9].map((slot) => {
                const item = menu.items.find((it) => it.slot === slot);
                const icon = item ? materialToIconUrl(item.material) : undefined;
                const tooltipTitle = item?.name ?? item?.material ?? `Slot ${slot}`;
                const tooltipLore = Array.isArray(item?.lore) ? item.lore : [];
                return (
                  <JavaSlot
                    key={slot}
                    slot={slot}
                    label={item ? item.material : String(slot)}
                    iconUrl={icon}
                    tooltipTitle={tooltipTitle}
                    tooltipLore={tooltipLore}
                    registerRef={(el) => {
                      slotRefs.current[slot] = el;
                    }}
                  />
                );
              })}
            </div>
            <div className="text-[#404040] text-2xl">➔</div>
             <div className="grid grid-cols-1 bg-[#8b8b8b] p-1 border-b border-r border-white border-t-[#373737] border-l-[#373737]">
               {(() => {
                 const resultItem = menu.items.find((it) => it.slot === 0);
                 const resultIcon = resultItem ? materialToIconUrl(resultItem.material) : undefined;
                 const tooltipTitle = resultItem?.name ?? resultItem?.material ?? "Result";
                 const tooltipLore = Array.isArray(resultItem?.lore) ? resultItem.lore : [];
                 return (
                   <JavaSlot
                     slot={0}
                     label={resultItem ? resultItem.material : "Result"}
                     iconUrl={resultIcon}
                     tall
                     tooltipTitle={tooltipTitle}
                     tooltipLore={tooltipLore}
                     registerRef={(el) => {
                       slotRefs.current[0] = el;
                     }}
                   />
                 );
               })()}
             </div>
           </div>
        ) : (
          <div className="text-center text-sm text-[#404040] p-4 border border-dashed border-[#8b8b8b]">
            Unknown menu type: {menu.type}
          </div>
        )}
      </div>
    </div>
  );
}

function JavaSlot({
  slot,
  label,
  tall,
  iconUrl,
  registerRef,
  muted,
  tooltipTitle,
  tooltipLore
}: {
  slot: number;
  label: string;
  tall?: boolean;
  iconUrl?: string;
  registerRef?: (el: HTMLButtonElement | null) => void;
  muted?: boolean;
  tooltipTitle?: string;
  tooltipLore?: string[];
}) {
  const { isOver, setNodeRef } = useDroppable({ id: `java-slot-${slot}` });
  const { selectedJavaSlot, setSelectedJavaSlot, setSelectedBedrockButtonId, setSelectedBedrockComponentId } = useDesignerStore();
  const selected = selectedJavaSlot === slot;
  const titlePlain = stripMinecraftCodes(tooltipTitle ?? label);
  return (
    <button
      ref={(node) => {
        (setNodeRef as any)(node);
        registerRef?.(node);
      }}
      onClick={() => {
        setSelectedJavaSlot(slot);
        setSelectedBedrockButtonId(null);
        setSelectedBedrockComponentId(null);
      }}
      type="button"
      aria-label={`Slot ${slot}: ${titlePlain}`}
      className={`${tall ? "h-12" : "h-9"} w-9 bg-[#8b8b8b] flex items-center justify-center text-[10px] cursor-pointer select-none relative group ${muted ? "opacity-75" : ""}`}
      title={titlePlain}
      style={{
        boxShadow: selected || isOver 
          ? "inset 0 0 0 2px white" 
          : "inset 1px 1px 0px #373737, inset -1px -1px 0px white"
      }}
    >
      <div className="absolute inset-0 hover:bg-white/20 pointer-events-none" />
      {tooltipTitle ? (
        <div className="absolute left-1/2 -translate-x-1/2 bottom-full mb-2 hidden group-hover:block pointer-events-none z-50">
          <div className="bg-black/90 border border-[#2b2b2b] px-2 py-1 rounded-sm shadow-2xl font-minecraft text-xs text-white max-w-[260px] whitespace-pre-wrap break-words">
            <div className="leading-tight">
              <MinecraftText text={tooltipTitle} />
            </div>
            {tooltipLore && tooltipLore.length > 0 && (
              <div className="mt-1 space-y-0.5 text-[#AAAAAA]">
                {tooltipLore.map((line, idx) => (
                  <div key={idx} className="leading-tight">
                    <MinecraftText text={line} />
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      ) : null}
      {iconUrl ? <img src={iconUrl} className="w-7 h-7 image-rendering-pixelated" /> : <span className="text-[#2b2b2b] hidden group-hover:block">{label.slice(0,2)}</span>}
    </button>
  );
}

function computeChestPreviewItems(menu: JavaMenu, rows: number) {
  const size = rows * 9;
  const base = new Map<number, any>();
  for (const it of menu.items ?? []) {
    if (typeof it?.slot !== "number") continue;
    if (it.slot < 0 || it.slot >= size) continue;
    base.set(it.slot, it);
  }
  const fillSlots = new Set<number>();
  const fills = menu.fills ?? [];
  for (const f of fills) {
    const material = f?.item?.material;
    if (!material) continue;
    const template = {
      material: String(material),
      amount: f.item.amount,
      name: f.item.name,
      lore: f.item.lore,
      glow: f.item.glow,
      onClick: f.item.onClick
    };
    if (f.type === "EMPTY") {
      for (let slot = 0; slot < size; slot++) {
        if (base.has(slot)) continue;
        base.set(slot, template);
        fillSlots.add(slot);
      }
      continue;
    }
    if (f.type === "ROW") {
      let row = typeof f.row === "number" ? f.row : 1;
      if (row < 1) row = 1;
      if (row > rows) row = rows;
      const start = (row - 1) * 9;
      for (let slot = start; slot < start + 9; slot++) {
        if (base.has(slot)) continue;
        base.set(slot, template);
        fillSlots.add(slot);
      }
      continue;
    }
    if (f.type === "COLUMN") {
      let col = typeof f.column === "number" ? f.column : 1;
      if (col < 1) col = 1;
      if (col > 9) col = 9;
      const baseCol = col - 1;
      for (let r = 0; r < rows; r++) {
        const slot = baseCol + r * 9;
        if (base.has(slot)) continue;
        base.set(slot, template);
        fillSlots.add(slot);
      }
    }
  }
  return { itemsBySlot: base, fillSlots };
}

function materialToIconUrl(material: string): string | undefined {
  const base = material.toLowerCase();
  const file = (JAVA_ASSETS as any)[base];
  if (!file) return undefined;
  return `/${file}`;
}
