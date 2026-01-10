import React from "react";
import { useDesignerStore } from "../core/store";
import { SortableContext, useSortable, verticalListSortingStrategy } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { BufferedInput, BufferedTextArea } from "../components/BufferedInput";

export function PropertiesPanel() {
  const {
    platform,
    bedrock,
    java,
    setBedrock,
    setJava,
    globalActions,
    setGlobalActions,
    selectedJavaSlot,
    setSelectedJavaSlot,
    selectedBedrockButtonId,
    setSelectedBedrockButtonId,
    selectedBedrockComponentId,
    setSelectedBedrockComponentId
  } = useDesignerStore();

  const buttonRefs = React.useRef<Record<string, HTMLDivElement | null>>({});
  const componentRefs = React.useRef<Record<string, HTMLDivElement | null>>({});
  const slotEditorRef = React.useRef<HTMLDivElement | null>(null);

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

  React.useEffect(() => {
    if (selectedJavaSlot === null) return;
    slotEditorRef.current?.scrollIntoView({ block: "center", behavior: "smooth" });
  }, [selectedJavaSlot]);

  return (
    <div className="ui-panel flex-1 min-h-0 overflow-y-auto custom-scrollbar flex flex-col">
      <div className="ui-panel-title shrink-0">Properties</div>
      <div className="p-2">
      {platform === "bedrock" && bedrock && (
        <div className="space-y-2">
          <div className="ui-chip">Bedrock</div>
          <BufferedInput
            className="ui-input"
            value={bedrock.command ?? ""}
            placeholder="command (optional)"
            onCommit={(v) => setBedrock({ ...bedrock, command: v }, "Updated command")}
          />
          <BufferedInput
            className="ui-input"
            value={bedrock.commandIntercept ?? ""}
            placeholder="command_intercept (optional)"
            onCommit={(v) => setBedrock({ ...bedrock, commandIntercept: v }, "Updated command intercept")}
          />
          <BufferedInput
            className="ui-input"
            value={bedrock.permission ?? ""}
            placeholder="permission (optional)"
            onCommit={(v) => setBedrock({ ...bedrock, permission: v }, "Updated permission")}
          />
          <BufferedInput
            className="ui-input"
            value={bedrock.title}
            maxLength={64}
            onCommit={(v) => setBedrock({ ...bedrock, title: v }, "Updated title")}
          />
          {"content" in bedrock && (
            <BufferedTextArea
              className="ui-textarea"
              value={bedrock.content ?? ""}
              onCommit={(v) => setBedrock({ ...bedrock, content: v }, "Updated content")}
            />
          )}
          {bedrock.type === "MODAL" && (
            <div className="text-xs text-yellow-300">
              Layout locked; content areas are editable.
            </div>
          )}
          {(bedrock.type === "SIMPLE" || bedrock.type === "MODAL") && "buttons" in bedrock && (
            <div className="mt-2">
              <div className="flex items-center justify-between mb-1">
                <div className="text-xs text-brand-muted">Buttons</div>
                {bedrock.type === "SIMPLE" && (
                  <button
                    className="ui-btn-secondary px-2 py-1 text-xs"
                    onClick={() => {
                      const buttons = [
                        ...bedrock.buttons,
                        { id: `button_${bedrock.buttons.length + 1}`, text: `Button ${bedrock.buttons.length + 1}` }
                      ];
                      setBedrock({ ...bedrock, buttons }, "Added button");
                    }}
                  >
                    Add
                  </button>
                )}
              </div>
              {selectedBedrockButtonId && (
                <div className="flex items-center justify-between mb-1">
                  <div className="text-xs text-brand.accent">Editing button: {selectedBedrockButtonId}</div>
                  <button
                    className="ui-btn-ghost px-2 py-1 text-xs"
                    onClick={(e) => {
                      e.preventDefault();
                      e.stopPropagation();
                      setSelectedBedrockButtonId(null);
                    }}
                  >
                    Clear
                  </button>
                </div>
              )}
              <SortableContext
                items={bedrock.buttons.map((b) => `bedrock-button-${b.id}`)}
                strategy={verticalListSortingStrategy}
              >
                {bedrock.buttons.map((b, idx) => (
                  <SortableCard
                    key={b.id}
                    id={`bedrock-button-${b.id}`}
                    selected={selectedBedrockButtonId === b.id}
                    onSelect={() => {
                      setSelectedBedrockButtonId(b.id);
                      setSelectedBedrockComponentId(null);
                      setSelectedJavaSlot(null);
                    }}
                    registerRef={(el) => {
                      buttonRefs.current[b.id] = el;
                    }}
                  >
                  <BufferedInput
                    className="ui-input text-xs mb-2"
                    value={b.id}
                    disabled={bedrock.type === "MODAL"}
                    onCommit={(v) => {
                      const buttons = [...bedrock.buttons];
                      buttons[idx] = { ...b, id: v };
                      setBedrock({ ...bedrock, buttons }, "Renamed button ID");
                    }}
                  />
                  <div className="flex gap-2 mb-2">
                    <BufferedInput
                      className="flex-1 ui-input px-2 py-1"
                      value={b.text}
                      maxLength={bedrock.type === "MODAL" ? 32 : 64}
                      onCommit={(v) => {
                        const buttons = [...bedrock.buttons];
                        buttons[idx] = { ...b, text: v };
                        setBedrock({ ...bedrock, buttons }, "Updated button text");
                      }}
                    />
                    {bedrock.type === "SIMPLE" && (
                      <button
                        className="ui-btn-secondary px-2 py-1 text-xs"
                        onClick={() => {
                          const buttons = bedrock.buttons.filter((_, i) => i !== idx);
                          setBedrock({
                            ...bedrock,
                            buttons: buttons.length ? buttons : [{ id: "button_1", text: "Button 1" }]
                          }, "Removed button");
                        }}
                      >
                        Remove
                      </button>
                    )}
                  </div>
                  <BufferedInput
                    className="ui-input text-xs mb-2"
                    value={b.image ?? ""}
                    placeholder='image (URL / "Notch" / %player_name% / base64)'
                    onCommit={(v) => {
                      const buttons = [...bedrock.buttons];
                      buttons[idx] = { ...b, image: v ? v : undefined };
                      setBedrock({ ...bedrock, buttons }, "Updated button image");
                    }}
                  />
                  {/* Conditional properties are currently hidden as per requirements */}
                  <div className="bg-brand-surface border border-brand-border p-2 mb-2 hidden">
                    {/* ... Conditional logic kept same but hidden ... */}
                  </div>
                  <ActionBlocksEditor
                    value={(b.onClick ?? []).map((a) => a.raw ?? "").filter(Boolean)}
                    onChange={(blocks) => {
                      const buttons = [...bedrock.buttons];
                      buttons[idx] = {
                        ...b,
                        onClick: blocks.filter(Boolean).map((raw) => ({ id: "raw", params: raw, raw }))
                      };
                      setBedrock({ ...bedrock, buttons }, "Updated button actions");
                    }}
                  />
                  </SortableCard>
                ))}
              </SortableContext>
            </div>
          )}
          {bedrock.type === "CUSTOM" && "components" in bedrock && (
            <div className="mt-2">
              <div className="text-xs text-brand-muted mb-1">Components</div>
              {selectedBedrockComponentId && (
                <div className="flex items-center justify-between mb-1">
                  <div className="text-xs text-brand.accent">Editing component: {selectedBedrockComponentId}</div>
                  <button
                    className="ui-btn-ghost px-2 py-1 text-xs"
                    onClick={(e) => {
                      e.preventDefault();
                      e.stopPropagation();
                      setSelectedBedrockComponentId(null);
                    }}
                  >
                    Clear
                  </button>
                </div>
              )}
              <SortableContext
                items={bedrock.components.map((c) => `bedrock-component-${c.id}`)}
                strategy={verticalListSortingStrategy}
              >
              {bedrock.components.map((c, ci) => (
                <SortableCard
                  key={c.id}
                  id={`bedrock-component-${c.id}`}
                  selected={selectedBedrockComponentId === c.id}
                  onSelect={() => {
                    setSelectedBedrockComponentId(c.id);
                    setSelectedBedrockButtonId(null);
                    setSelectedJavaSlot(null);
                  }}
                  registerRef={(el) => {
                    componentRefs.current[c.id] = el;
                  }}
                >
                  <div className="flex items-center justify-between mb-2">
                    <div className="text-xs">{c.type}</div>
                    <button
                      className="ui-btn-secondary px-2 py-1 text-xs"
                      onClick={() => {
                        const components = bedrock.components.filter((x) => x.id !== c.id);
                        setBedrock({ ...bedrock, components }, "Removed component");
                      }}
                    >
                      Remove
                    </button>
                  </div>
                  <BufferedInput
                    className="ui-input text-xs mb-2"
                    value={c.id}
                    onCommit={(v) => {
                      const components = [...bedrock.components];
                      components[ci] = { ...c, id: v };
                      setBedrock({ ...bedrock, components }, "Renamed component ID");
                    }}
                  />
                  <ComponentPropsEditor
                    type={c.type}
                    props={c.props}
                    onChange={(props) => {
                      const components = [...bedrock.components];
                      components[ci] = { ...c, props };
                      setBedrock({ ...bedrock, components }, `Updated ${c.type} props`);
                    }}
                  />
                  <ActionBlocksEditor
                    value={(c.onClick ?? []).map((a) => a.raw ?? "").filter(Boolean)}
                    onChange={(blocks) => {
                      const components = [...bedrock.components];
                      components[ci] = {
                        ...c,
                        onClick: blocks.filter(Boolean).map((raw) => ({ id: "raw", params: raw, raw }))
                      };
                      setBedrock({ ...bedrock, components }, "Updated component actions");
                    }}
                  />
                </SortableCard>
              ))}
              </SortableContext>
            </div>
          )}
          <div className="mt-3">
            <div className="text-xs text-brand-muted mb-1">Global actions</div>
            <ActionBlocksEditor
              value={(globalActions ?? []).map((a) => a.raw ?? "").filter(Boolean)}
              onChange={(blocks) => {
                setGlobalActions(blocks.filter(Boolean).map((raw) => ({ id: "raw", params: raw, raw })));
              }}
            />
          </div>
        </div>
      )}
      {platform === "java" && java && (
        <div className="space-y-2">
          <div className="ui-chip">Java</div>
          <BufferedInput
            className="ui-input"
            value={java.title}
            onCommit={(v) => setJava({ ...java, title: v }, "Updated title")}
          />
          {java.type === "CRAFTING" && (
            <div className="text-xs text-brand-muted">
              CRAFTING opens the player crafting table; items are ignored.
            </div>
          )}
          {java.type === "CHEST" && (
            <div className="flex items-center gap-2">
              <label className="text-sm">Size</label>
              <select
                className="ui-input w-auto px-2 py-1"
                value={java.size ?? 27}
                onChange={(e) => {
                  const size = Number(e.target.value);
                  const items = java.items.filter((i) => i.slot < size);
                  setJava({ ...java, size, items }, "Resized menu");
                }}
              >
                {[9, 18, 27, 36, 45, 54].map((n) => (
                  <option key={n} value={n}>
                    {n}
                  </option>
                ))}
              </select>
            </div>
          )}
          {java.type === "CHEST" && (
            <JavaFillsEditor java={java} onChange={setJava} />
          )}
          <div className="flex items-center gap-2">
            <BufferedInput
              className="ui-input w-24 px-2 py-1"
              type="number"
              placeholder="slot"
              value={selectedJavaSlot ?? ""}
              onCommit={(v) =>
                setSelectedJavaSlot(v === "" ? null : Number(v))
              }
              disabled={java.type === "CRAFTING"}
              min={0}
              max={java.type === "ANVIL" ? 2 : java.type === "CHEST" ? (java.size ?? 27) - 1 : 0}
            />
            <button
              className="ui-btn-secondary px-2 py-1 text-xs"
              onClick={() => setSelectedJavaSlot(null)}
              disabled={java.type === "CRAFTING"}
            >
              Clear
            </button>
          </div>
          {selectedJavaSlot !== null && (
            <div className="text-xs text-brand.accent">Editing slot: {selectedJavaSlot}</div>
          )}
          {selectedJavaSlot !== null && (
            <div ref={slotEditorRef}>
              <JavaSlotEditor
                slot={selectedJavaSlot}
                java={java}
                onChange={(next) => setJava(next, "Updated slot")}
              />
            </div>
          )}
        </div>
      )}
      </div>
    </div>
  );
}

function SortableCard({
  id,
  selected,
  onSelect,
  registerRef,
  children
}: {
  id: string;
  selected?: boolean;
  onSelect?: () => void;
  registerRef?: (el: HTMLDivElement | null) => void;
  children: React.ReactNode;
}) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } =
    useSortable({ id });
  const style: React.CSSProperties = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.7 : 1
  };
  return (
    <div
      ref={(node) => {
        setNodeRef(node);
        registerRef?.(node);
      }}
      style={style}
      onClick={onSelect}
      className={`mb-2 bg-brand-surface2 p-2 border ${selected ? "border-brand.accent" : "border-brand-border"}`}
    >
      <div className="flex items-center justify-between mb-2">
        <div className="text-xs text-brand-muted">Drag</div>
        <div className="px-2 py-1 bg-brand-surface text-xs cursor-move select-none border border-brand-border" {...attributes} {...listeners}>
          ⋮⋮
        </div>
      </div>
      {children}
    </div>
  );
}

function JavaFillsEditor({ java, onChange }: { java: any; onChange: (next: any, desc?: string) => void }) {
  const fills = (java.fills ?? []) as any[];
  return (
    <div className="bg-brand-surface2 border border-brand-border p-2">
      <div className="flex items-center justify-between mb-2">
        <div className="text-xs text-brand-muted">Fills</div>
        <button
          className="ui-btn-secondary px-2 py-1 text-xs"
          onClick={() => {
            const next = [...fills, { id: `fill_${fills.length + 1}`, type: "EMPTY", item: { material: "STONE" } }];
            onChange({ ...java, fills: next }, "Added fill");
          }}
        >
          Add
        </button>
      </div>
      <div className="space-y-2">
        <SortableContext
          items={fills.map((f, i) => `fill-${f.id ?? i}`)}
          strategy={verticalListSortingStrategy}
        >
          {fills.map((f, idx) => (
            <SortableFillCard
              key={f.id ?? idx}
              id={`fill-${f.id ?? idx}`}
              fill={f}
              onChange={(nextFill) => {
                const next = [...fills];
                next[idx] = nextFill;
                onChange({ ...java, fills: next }, "Updated fill");
              }}
              onRemove={() => {
                const next = fills.filter((_, i) => i !== idx);
                onChange({ ...java, fills: next.length ? next : undefined }, "Removed fill");
              }}
            />
          ))}
        </SortableContext>
        {!fills.length && <div className="text-xs text-brand-muted">No fills</div>}
      </div>
    </div>
  );
}

function SortableFillCard({
  id,
  fill,
  onChange,
  onRemove
}: {
  id: string;
  fill: any;
  onChange: (f: any) => void;
  onRemove: () => void;
}) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } =
    useSortable({ id });
  const style: React.CSSProperties = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.7 : 1
  };
  const f = fill;
  return (
    <div
      ref={setNodeRef}
      style={style}
      className="bg-brand-surface border border-brand-border p-2"
    >
      <div className="flex items-center justify-between mb-2">
        <div className="text-xs text-brand-muted">Fill</div>
        <div className="flex items-center gap-2">
          <div className="px-2 py-1 bg-brand-surface text-xs cursor-move select-none border border-brand-border" {...attributes} {...listeners}>
            ⋮⋮
          </div>
          <button className="ui-btn-ghost px-2 py-1 text-xs" onClick={onRemove}>
            ✕
          </button>
        </div>
      </div>
      <div className="grid grid-cols-12 gap-2 mb-2">
        <BufferedInput
          className="ui-input text-xs col-span-4"
          placeholder="id"
          value={f.id ?? ""}
          onCommit={(v) => onChange({ ...f, id: v })}
        />
        <select
          className="ui-input text-xs col-span-4 px-2 py-1"
          value={f.type ?? "EMPTY"}
          onChange={(e) => onChange({ ...f, type: e.target.value })}
        >
          <option value="ROW">ROW</option>
          <option value="COLUMN">COLUMN</option>
          <option value="EMPTY">EMPTY</option>
        </select>
        <BufferedInput
          className="ui-input text-xs col-span-2"
          type="number"
          placeholder="row"
          value={f.row ?? ""}
          onCommit={(v) => onChange({ ...f, row: v === "" ? undefined : Number(v) })}
        />
        <BufferedInput
          className="ui-input text-xs col-span-2"
          type="number"
          placeholder="col"
          value={f.column ?? ""}
          onCommit={(v) => onChange({ ...f, column: v === "" ? undefined : Number(v) })}
        />
      </div>
      <div className="grid grid-cols-12 gap-2 mb-2">
        <BufferedInput
          className="ui-input text-xs col-span-6"
          placeholder="item.material"
          value={f.item?.material ?? ""}
          onCommit={(v) => onChange({ ...f, item: { ...(f.item ?? {}), material: v } })}
        />
        <BufferedInput
          className="ui-input text-xs col-span-3"
          type="number"
          placeholder="amount"
          value={f.item?.amount ?? ""}
          onCommit={(v) => onChange({
            ...f,
            item: { ...(f.item ?? {}), amount: v === "" ? undefined : Number(v) }
          })}
        />
        <label className="col-span-3 flex items-center gap-2 text-xs text-brand-muted">
          <input
            type="checkbox"
            checked={!!f.item?.glow}
            onChange={(e) => onChange({ ...f, item: { ...(f.item ?? {}), glow: e.target.checked } })}
          />
          glow
        </label>
      </div>
      <BufferedInput
        className="ui-input text-xs mb-2"
        placeholder="item.name"
        value={f.item?.name ?? ""}
        onCommit={(v) => onChange({ ...f, item: { ...(f.item ?? {}), name: v || undefined } })}
      />
      <BufferedTextArea
        className="ui-textarea text-xs h-20 mb-2"
        placeholder="item.lore (one line per row)"
        value={(f.item?.lore ?? []).join("\n")}
        onCommit={(v) => {
          const lore = v.split("\n").map((s) => s.trim()).filter(Boolean);
          onChange({ ...f, item: { ...(f.item ?? {}), lore } });
        }}
      />
      <ActionBlocksEditor
        value={(f.item?.onClick ?? []).map((a: any) => a.raw ?? "").filter(Boolean)}
        onChange={(blocks) => {
          onChange({
            ...f,
            item: {
              ...(f.item ?? {}),
              onClick: blocks.filter(Boolean).map((raw) => ({ id: "raw", params: raw, raw }))
            }
          });
        }}
      />
    </div>
  );
}

function ActionBlocksEditor({ value, onChange }: { value: string[]; onChange: (v: string[]) => void }) {
  const [entries, setEntries] = React.useState<ActionEntry[]>(() => value.map(parseActionEntry));
  React.useEffect(() => {
    setEntries(value.map(parseActionEntry));
  }, [value.join("\n---\n")]);

  const update = (next: ActionEntry[]) => {
    setEntries(next);
    onChange(next.map(serializeActionEntry).filter(Boolean) as string[]);
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-1">
        <div className="text-xs text-brand-muted">onClick</div>
        <button
          className="ui-btn-secondary px-2 py-1 text-xs"
          onClick={(e) => {
            e.preventDefault();
            e.stopPropagation();
            update([...entries, { mode: "list", type: "message", lines: [""] }]);
          }}
        >
          Add action
        </button>
      </div>
      <div className="space-y-2">
        {entries.map((en, idx) => (
          <div key={idx} className="bg-brand-surface2 border border-brand-border p-2">
            <div className="flex items-center justify-between mb-2">
              <select
                className="ui-input px-2 py-1 text-xs"
                value={en.mode === "raw" ? "raw" : en.type}
                onChange={(e) => {
                  const v = e.target.value;
                  const next = [...entries];
                  if (v === "raw") next[idx] = { mode: "raw", raw: en.mode === "raw" ? en.raw : serializeActionEntry(en) ?? "" };
                  else next[idx] = { mode: "list", type: v, lines: en.mode === "list" ? en.lines : [""] };
                  update(next);
                }}
              >
                {ACTION_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
              <div className="flex gap-2">
                <button
                  className="ui-btn-secondary px-2 py-1 text-xs"
                  onClick={(e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    if (idx === 0) return;
                    const next = [...entries];
                    const tmp = next[idx - 1];
                    next[idx - 1] = next[idx];
                    next[idx] = tmp;
                    update(next);
                  }}
                >
                  Up
                </button>
                <button
                  className="ui-btn-secondary px-2 py-1 text-xs"
                  onClick={(e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    if (idx === entries.length - 1) return;
                    const next = [...entries];
                    const tmp = next[idx + 1];
                    next[idx + 1] = next[idx];
                    next[idx] = tmp;
                    update(next);
                  }}
                >
                  Down
                </button>
                <button
                  className="ui-btn-secondary px-2 py-1 text-xs"
                  onClick={(e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    const next = entries.filter((_, i) => i !== idx);
                    update(next);
                  }}
                >
                  Remove
                </button>
              </div>
            </div>

            {en.mode === "raw" ? (
              <BufferedTextArea
                className="ui-textarea h-28 text-xs"
                placeholder={'raw action block, e.g.\nmessage {\n  - "Hello"\n}'}
                value={en.raw}
                onCommit={(v) => {
                  const next = [...entries];
                  next[idx] = { ...en, raw: v };
                  update(next);
                }}
              />
            ) : (
              <BufferedTextArea
                className="ui-textarea h-20 text-xs"
                placeholder="one line per row"
                value={en.lines.join("\n")}
                onCommit={(v) => {
                  const next = [...entries];
                  next[idx] = { ...en, lines: v.split("\n") };
                  update(next);
                }}
              />
            )}
          </div>
        ))}
        {!entries.length && <div className="text-xs text-brand-muted">No actions</div>}
      </div>
    </div>
  );
}

type ActionEntry =
  | { mode: "list"; type: string; lines: string[] }
  | { mode: "raw"; raw: string };

const ACTION_TYPES: { value: string; label: string }[] = [
  { value: "message", label: "message" },
  { value: "command", label: "command" },
  { value: "server", label: "server" },
  { value: "broadcast", label: "broadcast" },
  { value: "open", label: "open" },
  { value: "sound", label: "sound" },
  { value: "economy", label: "economy" },
  { value: "title", label: "title" },
  { value: "actionbar", label: "actionbar" },
  { value: "inventory", label: "inventory" },
  { value: "delay", label: "delay" },
  { value: "random", label: "random" },
  { value: "url", label: "url" },
  { value: "raw", label: "raw" }
];

const LIST_ACTION_SET = new Set(
  ACTION_TYPES.map((t) => t.value).filter((v) => v !== "raw")
);

function parseActionEntry(raw: string): ActionEntry {
  const s = String(raw ?? "").trim();
  const open = s.indexOf("{");
  const close = s.lastIndexOf("}");
  if (open !== -1 && close !== -1 && close > open) {
    const type = s.slice(0, open).trim();
    const inner = s.slice(open + 1, close);
    const lines = inner
      .split("\n")
      .map((l) => l.trim())
      .filter((l) => l.startsWith("-"))
      .map((l) => l.replace(/^-+\s*/, "").trim())
      .map((v) => v.replace(/^"+|"+$/g, ""))
      .map((v) => v.replace(/\\"/g, '"'));
    if (type && LIST_ACTION_SET.has(type)) return { mode: "list", type, lines: lines.length ? lines : [""] };
  }
  return { mode: "raw", raw: s };
}

function serializeActionEntry(entry: ActionEntry): string | undefined {
  if (entry.mode === "raw") return entry.raw?.trim() ? entry.raw.trim() : undefined;
  const type = entry.type.trim();
  const lines = entry.lines.map((l) => l.replace(/\r/g, "")).map((l) => l.trim()).filter(Boolean);
  const body = lines.map((l) => `  - "${l.replace(/\\/g, "\\\\").replace(/"/g, '\\"')}"`).join("\n");
  if (!type) return undefined;
  if (!lines.length) return `${type} {\n}`;
  return `${type} {\n${body}\n}`;
}

function JavaSlotEditor({
  slot,
  java,
  onChange
}: {
  slot: number;
  java: any;
  onChange: (next: any) => void;
}) {
  const existing = java.items.find((i: any) => i.slot === slot);
  const item = existing ?? { slot, material: "STONE", amount: 1, lore: [] };
  return (
    <div className="bg-brand-surface p-2 border border-brand-border">
      <div className="text-xs text-brand-muted mb-2">Slot {slot}</div>
      <div className="flex gap-2 mb-2">
        <BufferedInput
          className="flex-1 ui-input px-2 py-1"
          value={item.material}
          onCommit={(v) => {
            const items = java.items.filter((i: any) => i.slot !== slot);
            items.push({ ...item, material: v });
            onChange({ ...java, items });
          }}
        />
        <BufferedInput
          className="w-20 ui-input px-2 py-1"
          type="number"
          value={item.amount ?? 1}
          onCommit={(v) => {
            const items = java.items.filter((i: any) => i.slot !== slot);
            items.push({ ...item, amount: Number(v) });
            onChange({ ...java, items });
          }}
        />
      </div>
      <BufferedInput
        className="w-full ui-input px-2 py-1 mb-2"
        placeholder="name"
        value={item.name ?? ""}
        onCommit={(v) => {
          const items = java.items.filter((i: any) => i.slot !== slot);
          items.push({ ...item, name: v });
          onChange({ ...java, items });
        }}
      />
      <label className="flex items-center gap-2 text-xs text-brand-muted mb-2">
        <input
          type="checkbox"
          checked={!!item.glow}
          onChange={(e) => {
            const items = java.items.filter((i: any) => i.slot !== slot);
            items.push({ ...item, glow: e.target.checked });
            onChange({ ...java, items });
          }}
        />
        glow
      </label>
      <BufferedTextArea
        className="ui-textarea h-24 text-xs mb-2"
        placeholder="lore (one line per row)"
        value={(item.lore ?? []).join("\n")}
        onCommit={(v) => {
          const lore = v.split("\n").map((s) => s.trim()).filter(Boolean);
          const items = java.items.filter((i: any) => i.slot !== slot);
          items.push({ ...item, lore });
          onChange({ ...java, items });
        }}
      />
      <button
        className="ui-btn-secondary px-2 py-1 text-xs mb-2"
        onClick={() => {
          const lore = [...(item.lore ?? []), `Line ${(item.lore ?? []).length + 1}`];
          const items = java.items.filter((i: any) => i.slot !== slot);
          items.push({ ...item, lore });
          onChange({ ...java, items });
        }}
      >
        Add lore line
      </button>
      <ActionBlocksEditor
        value={(item.onClick ?? []).map((a: any) => a.raw ?? "").filter(Boolean)}
        onChange={(blocks) => {
          const items = java.items.filter((i: any) => i.slot !== slot);
          items.push({
            ...item,
            onClick: blocks.filter(Boolean).map((raw) => ({ id: "raw", params: raw, raw }))
          });
          onChange({ ...java, items });
        }}
      />
      <div className="flex justify-between mt-2">
        <button
          className="ui-btn-secondary px-2 py-1 text-xs"
          onClick={() => {
            const items = java.items.filter((i: any) => i.slot !== slot);
            onChange({ ...java, items });
          }}
        >
          Remove Item
        </button>
      </div>
    </div>
  );
}

function ComponentPropsEditor({
  type,
  props,
  onChange
}: {
  type: string;
  props: Record<string, any>;
  onChange: (p: Record<string, any>) => void;
}) {
  if (type === "label") {
    return (
      <BufferedInput
        className="w-full ui-input px-2 py-1 mb-2"
        placeholder="text"
        value={props.text ?? ""}
        onCommit={(v) => onChange({ ...props, text: v })}
      />
    );
  }
  if (type === "input") {
    return (
      <div className="space-y-2 mb-2">
        <BufferedInput
          className="w-full ui-input px-2 py-1"
          placeholder="text"
          value={props.text ?? ""}
          onCommit={(v) => onChange({ ...props, text: v })}
        />
        <BufferedInput
          className="w-full ui-input px-2 py-1"
          placeholder="placeholder"
          value={props.placeholder ?? ""}
          onCommit={(v) => onChange({ ...props, placeholder: v })}
        />
        <BufferedInput
          className="w-full ui-input px-2 py-1"
          placeholder="default"
          value={props.default ?? ""}
          onCommit={(v) => onChange({ ...props, default: v })}
        />
      </div>
    );
  }
  if (type === "dropdown") {
    return (
      <div className="space-y-2 mb-2">
        <BufferedInput
          className="w-full ui-input px-2 py-1"
          placeholder="text"
          value={props.text ?? ""}
          onCommit={(v) => onChange({ ...props, text: v })}
        />
        <BufferedTextArea
          className="ui-textarea h-20 text-xs"
          placeholder="options (one per line)"
          value={Array.isArray(props.options) ? props.options.join("\n") : ""}
          onCommit={(v) =>
            onChange({
              ...props,
              options: v.split("\n").map((s) => s.trim()).filter(Boolean)
            })
          }
        />
        <BufferedInput
          className="w-full ui-input px-2 py-1"
          type="number"
          placeholder="default index"
          value={props.default ?? 0}
          onCommit={(v) => onChange({ ...props, default: Number(v) })}
        />
      </div>
    );
  }
  if (type === "toggle") {
    return (
      <div className="space-y-2 mb-2">
        <BufferedInput
          className="w-full ui-input px-2 py-1"
          placeholder="text"
          value={props.text ?? ""}
          onCommit={(v) => onChange({ ...props, text: v })}
        />
        <label className="flex items-center gap-2 text-xs text-brand-muted">
          <input
            type="checkbox"
            checked={Boolean(props.default)}
            onChange={(e) => onChange({ ...props, default: e.target.checked })}
          />
          default
        </label>
      </div>
    );
  }
  if (type === "slider") {
    return (
      <div className="space-y-2 mb-2">
        <BufferedInput
          className="w-full ui-input px-2 py-1"
          placeholder="text"
          value={props.text ?? ""}
          onCommit={(v) => onChange({ ...props, text: v })}
        />
        <div className="grid grid-cols-4 gap-2">
          <BufferedInput
            className="ui-input px-2 py-1"
            type="number"
            placeholder="min"
            value={props.min ?? 0}
            onCommit={(v) => onChange({ ...props, min: Number(v) })}
          />
          <BufferedInput
            className="ui-input px-2 py-1"
            type="number"
            placeholder="max"
            value={props.max ?? 10}
            onCommit={(v) => onChange({ ...props, max: Number(v) })}
          />
          <BufferedInput
            className="ui-input px-2 py-1"
            type="number"
            placeholder="step"
            value={props.step ?? 1}
            onCommit={(v) => onChange({ ...props, step: Number(v) })}
          />
          <BufferedInput
            className="ui-input px-2 py-1"
            type="number"
            placeholder="default"
            value={props.default ?? 0}
            onCommit={(v) => onChange({ ...props, default: Number(v) })}
          />
        </div>
      </div>
    );
  }
  return null;
}
