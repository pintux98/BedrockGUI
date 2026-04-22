import React, { useState } from "react";
import { useDroppable } from "@dnd-kit/core";
import { useDesignerStore } from "../core/store";
import { BedrockPreview } from "./previews/BedrockPreview";
import { JavaPreview } from "./previews/JavaPreview";

export function Canvas() {
  const { platform, bedrock, java } = useDesignerStore();
  const bedrockId =
    bedrock?.type === "CUSTOM" ? "bedrock-components" : "bedrock-buttons";
  const { isOver, setNodeRef } = useDroppable({ id: platform === "bedrock" ? bedrockId : "java-canvas" });
  const invalid = isOver && bedrock && bedrock.type !== "CUSTOM" && bedrock.type !== "SIMPLE";

  const [viewMode, setViewMode] = useState<"desktop" | "mobile" | "split">("mobile");
  const [detailedMode, setDetailedMode] = useState(false);

  return (
    <div className="h-full flex flex-col bg-[#1e1e1e] relative">
       {/* Preview Toolbar */}
      <div className="h-10 bg-brand-surface border-b-2 border-[#1e1e1e] flex items-center justify-between px-4">
        <div className="flex items-center gap-4">
          <div className="text-xs text-brand-muted uppercase tracking-widest font-bold">Preview</div>
          <label className="flex items-center gap-2 text-xs text-brand-muted cursor-pointer select-none">
            <input 
              type="checkbox" 
              checked={detailedMode} 
              onChange={(e) => setDetailedMode(e.target.checked)}
              className="accent-brand-accent"
            />
            Detailed Mode
          </label>
        </div>
        <div className="flex gap-1 bg-brand-surface2 p-1 border-2 border-[#555] border-t-[#101010] border-l-[#101010] border-b-[#555] border-r-[#555]">
          <button
            disabled
            className="px-3 py-1 text-xs font-bold bg-brand-accent text-white cursor-default"
          >
            DESKTOP
          </button>
        </div>
      </div>

      {/* Preview Area */}
      <div
        ref={setNodeRef}
        className={`flex-1 p-8 overflow-y-auto overflow-x-hidden flex justify-center items-start ${
          isOver ? "bg-brand-surface/20" : ""
        } ${invalid ? "ring-2 ring-red-500" : ""}`}
        style={{
          backgroundImage: "radial-gradient(#2b2b2b 1px, transparent 1px)",
          backgroundSize: "20px 20px"
        }}
      >
        <div className="flex flex-col items-center gap-2 transition-all duration-300 w-full">
          <div className="text-xs text-brand-muted font-mono">
            Desktop View (Full Width)
          </div>
          <div className="w-full max-w-4xl">
            {platform === "bedrock" && bedrock && <BedrockPreview form={bedrock} detailed={detailedMode} />}
            {platform === "java" && java && <JavaPreview menu={java} />}
          </div>
        </div>
      </div>
    </div>
  );
}
