import React from "react";
import { BedrockForm } from "../../core/types";
import ReactMarkdown from "react-markdown";
import { useDroppable } from "@dnd-kit/core";
import { useDesignerStore } from "../../core/store";

export function BedrockPreview({ form, detailed }: { form: BedrockForm; detailed?: boolean }) {
  const { selectedBedrockButtonId, setSelectedBedrockButtonId, selectedBedrockComponentId, setSelectedBedrockComponentId, setSelectedJavaSlot } =
    useDesignerStore();
  const buttonRefs = React.useRef<Record<string, HTMLButtonElement | null>>({});
  const componentRefs = React.useRef<Record<string, HTMLDivElement | null>>({});
  const listRef = React.useRef<HTMLDivElement | null>(null);
  const customRef = React.useRef<HTMLDivElement | null>(null);

  React.useEffect(() => {
    if (!selectedBedrockButtonId) return;
    const el = buttonRefs.current[selectedBedrockButtonId];
    el?.scrollIntoView({ block: "center", behavior: "smooth" });
  }, [selectedBedrockButtonId]);

  React.useEffect(() => {
    if (!selectedBedrockComponentId) return;
    const el = componentRefs.current[selectedBedrockComponentId];
    el?.scrollIntoView({ block: "center", behavior: "smooth" });
  }, [selectedBedrockComponentId]);

  if (!form) return null;

  const [assumeConditionsTrue, setAssumeConditionsTrue] = React.useState(false);
  const droppableId = form.type === "SIMPLE" || form.type === "MODAL" ? "bedrock-buttons" : "bedrock-components";
  const { isOver, setNodeRef } = useDroppable({ id: droppableId });

  const bgStyle = "bg-[#313233]"; // Ore UI dark background
  const borderStyle = "border-[#48494a]"; // Ore UI border
  const headerStyle = "bg-[#2b2c2d]"; // Header background

  if (form.type === "SIMPLE" || form.type === "MODAL") {
    const hasConditional =
      "buttons" in form &&
      Array.isArray(form.buttons) &&
      form.buttons.some(
        (b: any) =>
          !!b?.showCondition ||
          !!b?.alternativeText ||
          !!b?.alternativeImage ||
          !!b?.alternativeOnClick ||
          (Array.isArray(b?.conditions) && !!b.conditions.length)
      );
    return (
      <div
        ref={setNodeRef}
        onClick={(e) => {
          if (e.target !== e.currentTarget) return;
          setSelectedBedrockButtonId(null);
          setSelectedBedrockComponentId(null);
          setSelectedJavaSlot(null);
        }}
        className={`max-w-xl mx-auto shadow-2xl ${bgStyle} text-white font-minecraft ${isOver ? "ring-2 ring-brand-accent" : ""}`}
        style={{ width: "min(520px, 100%)", border: "2px solid #48494a" }}
      >
        <div className={`h-10 flex items-center justify-between px-3 ${headerStyle} border-b-2 ${borderStyle}`}>
          <div className="font-semibold text-lg truncate font-smooth-none">{form.title}</div>
          <button className="text-white hover:text-red-400 font-bold" type="button" aria-label="Close preview">
            ✕
          </button>
        </div>
        <div className={`p-2 ${bgStyle}`}>
          {hasConditional && (
            <div className="flex items-center justify-between mb-3 px-1">
              <div className="text-xs text-gray-400">Preview Conditions</div>
              <label className="flex items-center gap-2 text-xs text-gray-300 cursor-pointer select-none">
                <input
                  type="checkbox"
                  className="accent-brand-accent"
                  checked={assumeConditionsTrue}
                  onChange={(e) => setAssumeConditionsTrue(e.target.checked)}
                />
                Assume True
              </label>
            </div>
          )}
          {form.type !== "MODAL" && "content" in form && form.content && (
            <div className="text-gray-300 p-2 text-base mb-2 whitespace-pre-wrap font-smooth-none">
              {form.type === "MODAL" ? <ReactMarkdown>{form.content}</ReactMarkdown> : form.content}
            </div>
          )}

          {form.type === "SIMPLE" && (
            <div
              ref={listRef}
              className="max-h-80 overflow-y-auto pr-1 custom-scrollbar"
              style={{ scrollbarGutter: "stable" as any }}
            >
              <div className="space-y-1">
                {"buttons" in form &&
                  Array.isArray(form.buttons) &&
                  form.buttons
                    .map((b: any) => ({ base: b, display: getBedrockButtonDisplay(b, assumeConditionsTrue) }))
                    .filter((x) => !x.display.hidden)
                    .map(({ base: b, display }) => (
                    <button
                      key={b.id}
                      ref={(el) => {
                        buttonRefs.current[b.id] = el;
                      }}
                      type="button"
                      onClick={() => {
                        setSelectedBedrockButtonId(b.id);
                        setSelectedBedrockComponentId(null);
                        setSelectedJavaSlot(null);
                      }}
                      className={`w-full flex items-center gap-3 bg-[#3a3b3c] border-2 px-3 py-2 text-white text-base font-smooth-none active:bg-[#2a2b2c] transition-colors relative group ${
                        selectedBedrockButtonId === b.id ? "border-brand-accent bg-[#4a4b4c]" : "border-[#1e1e1f]"
                      }`}
                    >
                      {detailed && <div className="absolute top-0 right-0 bg-black/50 text-[8px] px-1 text-gray-300 font-mono">{b.id}</div>}
                      <div className="w-8 flex items-center justify-center">
                        {display.image ? <BedrockButtonImage image={display.image} /> : <div className="w-6 h-6" />}
                      </div>
                      <div className="flex-1 text-left">{display.text}</div>
                    </button>
                  ))}
              </div>
            </div>
          )}

          {form.type === "MODAL" && (
            <div className="grid grid-cols-1 gap-1">
              <div className="text-gray-300 p-2 text-base h-32 overflow-y-auto whitespace-pre-wrap font-smooth-none">
                {"content" in form && form.content ? (
                  <ReactMarkdown>{form.content}</ReactMarkdown>
                ) : (
                  <div className="opacity-50">Content...</div>
                )}
              </div>
              <div className="grid grid-cols-2 gap-2 mt-2">
              {"buttons" in form &&
                Array.isArray(form.buttons) &&
                form.buttons
                  .map((b: any) => ({ base: b, display: getBedrockButtonDisplay(b, assumeConditionsTrue) }))
                  .filter((x) => !x.display.hidden)
                  .map(({ base: b, display }) => (
                  <button
                    key={b.id}
                    ref={(el) => {
                      buttonRefs.current[b.id] = el;
                    }}
                    type="button"
                    onClick={() => {
                      setSelectedBedrockButtonId(b.id);
                      setSelectedBedrockComponentId(null);
                      setSelectedJavaSlot(null);
                    }}
                    className={`w-full bg-[#3a3b3c] border-2 px-3 py-2 text-white text-base font-smooth-none active:bg-[#2a2b2c] relative ${
                      selectedBedrockButtonId === b.id ? "border-brand-accent bg-[#4a4b4c]" : "border-[#1e1e1f]"
                    }`}
                  >
                    {detailed && <div className="absolute top-0 right-0 bg-black/50 text-[8px] px-1 text-gray-300 font-mono">{b.id}</div>}
                    {display.text}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    );
  }
  return (
    <div
      ref={setNodeRef}
      onClick={(e) => {
        if (e.target !== e.currentTarget) return;
        setSelectedBedrockButtonId(null);
        setSelectedBedrockComponentId(null);
        setSelectedJavaSlot(null);
      }}
      className={`max-w-xl mx-auto shadow-2xl ${bgStyle} text-white font-minecraft ${isOver ? "ring-2 ring-brand-accent" : ""}`}
      style={{ width: "min(520px, 100%)", border: "2px solid #48494a" }}
    >
      <div className={`h-10 flex items-center justify-between px-3 ${headerStyle} border-b-2 ${borderStyle}`}>
        <div className="font-semibold text-lg truncate font-smooth-none">{form.title}</div>
        <button className="text-white hover:text-red-400 font-bold" type="button" aria-label="Close preview">
          ✕
        </button>
      </div>
      <div className={`p-2 ${bgStyle}`}>
        <div
          ref={customRef}
          className="max-h-96 overflow-y-auto pr-1 custom-scrollbar"
          style={{ scrollbarGutter: "stable" as any }}
        >
          <div className="space-y-3">
            {Array.isArray(form.components) && form.components.map((c) => (
              <div
                key={c.id}
                ref={(el) => {
                  componentRefs.current[c.id] = el;
                }}
                onClick={() => {
                  setSelectedBedrockComponentId(c.id);
                  setSelectedBedrockButtonId(null);
                  setSelectedJavaSlot(null);
                }}
                className={`bg-[#3a3b3c] border-2 p-3 cursor-pointer relative ${
                  selectedBedrockComponentId === c.id ? "border-brand-accent" : "border-[#1e1e1f]"
                }`}
              >
                {detailed && <div className="absolute top-0 right-0 bg-black/50 text-[8px] px-1 text-gray-300 font-mono">{c.id} ({c.type})</div>}
                <BedrockComponentView type={c.type} props={c.props as any} />
              </div>
            ))}
            {!form.components?.length && (
              <div className="text-sm text-gray-400 text-center py-8 border-2 border-dashed border-gray-600">Drag components here</div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function BedrockComponentView({ type, props }: { type: string; props: any }) {
  const [localValue, setLocalValue] = React.useState<number | string | boolean | undefined>(undefined);
  
  // Reset local state when props change (specifically default)
  React.useEffect(() => {
    setLocalValue(undefined);
  }, [props?.default]);

  if (type === "label") {
    return <div className="text-base text-white font-smooth-none">{props?.text ?? "Label"}</div>;
  }
  if (type === "input") {
    return (
      <div className="space-y-1">
        <div className="text-sm text-white font-smooth-none">{props?.text ?? "Input"}</div>
        <div className="bg-[rgb(30,30,30)] border border-gray-600 px-2 py-2 text-base text-white font-smooth-none">
          {props?.placeholder ?? "Type here..."}
        </div>
      </div>
    );
  }
  if (type === "dropdown") {
    const opts = Array.isArray(props?.options) ? props.options : [];
    return (
      <div className="space-y-1">
        <div className="text-sm text-white font-smooth-none">{props?.text ?? "Dropdown"}</div>
        <div className="bg-[rgb(30,30,30)] border border-gray-600 px-2 py-2 text-base text-white flex justify-between font-smooth-none">
          <span>{opts[(props?.default ?? 0)] ?? opts[0] ?? "Option 1"}</span>
          <span className="text-gray-400">▼</span>
        </div>
      </div>
    );
  }
  if (type === "toggle") {
    const checked = localValue !== undefined ? Boolean(localValue) : Boolean(props?.default);
    return (
      <div 
        className="flex items-center justify-between cursor-pointer"
        onClick={() => setLocalValue(!checked)}
      >
        <div className="text-base text-white font-smooth-none">{props?.text ?? "Toggle"}</div>
        <div className={`w-12 h-6 border-2 ${checked ? "bg-brand-accent border-brand-accent" : "bg-[rgb(58,58,58)] border-gray-500"}`}>
          <div className={`w-4 h-4 bg-white mt-0.5 ${checked ? "ml-6" : "ml-1"} transition-all`} />
        </div>
      </div>
    );
  }
  if (type === "slider") {
    const min = Number(props?.min ?? 0);
    const max = Number(props?.max ?? 10);
    const def = Number(props?.default ?? min);
    const current = localValue !== undefined ? Number(localValue) : def;
    const pct = max > min ? ((current - min) / (max - min)) * 100 : 0;
    
    return (
      <div className="space-y-1">
        <div className="text-base text-white font-smooth-none">{props?.text ?? "Slider"}</div>
        <div className="h-4 bg-[rgb(30,30,30)] border border-gray-600 relative cursor-pointer"
             onClick={(e) => {
               const rect = e.currentTarget.getBoundingClientRect();
               const x = e.clientX - rect.left;
               const p = Math.max(0, Math.min(1, x / rect.width));
               const val = Math.round(min + p * (max - min));
               setLocalValue(val);
             }}
        >
          <div className="h-full bg-brand-accent absolute top-0 left-0" style={{ width: `${Math.max(0, Math.min(100, pct))}%` }} />
          <div className="w-2 h-5 bg-white absolute top-[-2px] border border-gray-500" style={{ left: `${Math.max(0, Math.min(100, pct))}%`, transform: 'translateX(-50%)' }} />
        </div>
        <div className="text-right text-xs text-white font-mono">{current}</div>
      </div>
    );
  }
  return <div className="text-sm text-gray-400">{String(type)}</div>;
}

function BedrockButtonImage({ image }: { image: string }) {
  const src = resolveBedrockImageSrc(image);
  if (src) return <img src={src} className="w-7 h-7 image-rendering-pixelated" />;
  const label = image.trim().slice(0, 2).toUpperCase() || "IMG";
  return (
    <div className="w-7 h-7 bg-[#5b5b5b] border border-[#8b8b8b] flex items-center justify-center text-[9px] text-white">
      {label}
    </div>
  );
}

function getBedrockButtonDisplay(button: any, assumeTrue: boolean): { text: string; image?: string; hidden: boolean } {
  const baseText = String(button?.text ?? "");
  const baseImage = button?.image ? String(button.image) : undefined;
  const hasShow = typeof button?.showCondition === "string" && button.showCondition.trim().length;
  const altText = button?.alternativeText ? String(button.alternativeText) : undefined;
  const altImage = button?.alternativeImage ? String(button.alternativeImage) : undefined;

  const rules = Array.isArray(button?.conditions) ? button.conditions : [];
  const pick = (prop: string) => rules.find((r: any) => r?.property === prop)?.value;

  if (assumeTrue) {
    const text = (pick("text") ?? baseText) as string;
    const image = (pick("image") ?? baseImage) as string | undefined;
    return { text, image, hidden: false };
  }

  const text = altText ?? baseText;
  const image = altImage ?? baseImage;
  const hidden = hasShow && !altText && !altImage;
  return { text, image, hidden };
}

function resolveBedrockImageSrc(raw: string): string | undefined {
  const s = raw.trim().replace(/^`+|`+$/g, "").trim();
  if (!s) return undefined;
  if (s.startsWith("http://") || s.startsWith("https://") || s.startsWith("data:")) return s;
  if (s.startsWith("textures.minecraft.net/texture/")) return `https://${s}`;
  return undefined;
}
