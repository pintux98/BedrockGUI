import React from "react";
import { JavaPreview } from "../canvas/previews/JavaPreview";
import { BedrockPreview } from "../canvas/previews/BedrockPreview";

export function StyleGuidePanel() {
  return (
    <div className="ui-panel">
      <div className="ui-panel-title">UI Guide</div>
      <div className="text-xs text-brand-muted mb-3">
        Tokens, components, and mockups for consistent UI.
      </div>

      <div className="space-y-4">
        <Section title="Colors">
          <div className="grid grid-cols-2 gap-2">
            <Swatch label="bg" className="bg-brand-bg" />
            <Swatch label="surface" className="bg-brand-surface" />
            <Swatch label="surface2" className="bg-brand-surface2" />
            <Swatch label="border" className="bg-brand-border" />
            <Swatch label="text" className="bg-brand-text" />
            <Swatch label="muted" className="bg-brand-muted" />
            <Swatch label="accent" className="bg-brand-accent" />
            <Swatch label="focus" className="bg-brand-focus" />
          </div>
        </Section>

        <Section title="Buttons">
          <div className="flex flex-wrap gap-2">
            <button className="ui-btn-primary">Primary</button>
            <button className="ui-btn-secondary">Secondary</button>
            <button className="ui-btn-ghost">Ghost</button>
            <button className="ui-btn-secondary" disabled>
              Disabled
            </button>
          </div>
        </Section>

        <Section title="Inputs">
          <div className="space-y-2">
            <input className="ui-input" placeholder="Text input" />
            <textarea className="ui-textarea h-20" placeholder="Textarea" />
            <select className="ui-input">
              <option>Option A</option>
              <option>Option B</option>
            </select>
          </div>
        </Section>

        <Section title="Bedrock mockups">
          <div className="space-y-3">
            <div className="text-xs text-brand-muted">SIMPLE</div>
            <BedrockPreview form={bedrockSimpleMock as any} />
            <div className="text-xs text-brand-muted">MODAL</div>
            <BedrockPreview form={bedrockModalMock as any} />
            <div className="text-xs text-brand-muted">CUSTOM</div>
            <BedrockPreview form={bedrockCustomMock as any} />
          </div>
        </Section>

        <Section title="Java mockups">
          <div className="space-y-3">
            <div className="text-xs text-brand-muted">CHEST</div>
            <JavaPreview menu={javaChestMock as any} />
            <div className="text-xs text-brand-muted">ANVIL</div>
            <JavaPreview menu={javaAnvilMock as any} />
          </div>
        </Section>
      </div>
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div>
      <div className="text-xs font-semibold text-brand-text mb-2">{title}</div>
      {children}
    </div>
  );
}

function Swatch({ label, className }: { label: string; className: string }) {
  return (
    <div className="flex items-center gap-2">
      <div className={`w-8 h-6 rounded border border-brand-border ${className}`} />
      <div className="text-xs text-brand-muted">{label}</div>
    </div>
  );
}

const bedrockSimpleMock = {
  type: "SIMPLE",
  title: "A simple title $1",
  content: "Long text to fit in the menu",
  buttons: [
    { id: "a", text: "Some fancy text with > player_name/ as placeholder", image: "" },
    { id: "b", text: "Some fancy text with > player_name/ as placeholder", image: "" },
    { id: "c", text: "Some fancy text with > player_name/ as placeholder", image: "" },
    { id: "d", text: "Some fancy text with > player_name/ as placeholder", image: "" }
  ]
};

const bedrockModalMock = {
  type: "MODAL",
  title: "Modal test $1",
  content: "Something here to write",
  buttons: [
    { id: "yes", text: "Your_name > player_name/", image: "" },
    { id: "no", text: "Other > thing", image: "" }
  ]
};

const bedrockCustomMock = {
  type: "CUSTOM",
  title: "Custom Form Example",
  components: [
    { id: "label_1", type: "label", props: { text: "Enter your name" } },
    { id: "input_1", type: "input", props: { text: "", placeholder: "DefaultName", default: "" } },
    { id: "slider_1", type: "slider", props: { text: "Select your age! 18", min: 0, max: 100, step: 1, default: 18 } },
    { id: "dropdown_1", type: "dropdown", props: { text: "Select a preference", options: ["Option 1", "Option 2"], default: 0 } },
    { id: "toggle_1", type: "toggle", props: { text: "Enable feature", default: false } }
  ]
};

const javaChestMock = {
  type: "CHEST",
  title: "Example Menu",
  size: 27,
  items: [
    { slot: 10, material: "DIAMOND_SWORD", name: "Select", lore: ["Line 1", "Line 2"] },
    { slot: 12, material: "EMERALD", name: "Shop" },
    { slot: 14, material: "PAPER", name: "Info" }
  ]
};

const javaAnvilMock = {
  type: "ANVIL",
  title: "Repair & Name",
  items: [
    { slot: 0, material: "IRON_SWORD", name: "Input" },
    { slot: 1, material: "IRON_INGOT", name: "Material" },
    { slot: 2, material: "IRON_SWORD", name: "Result" }
  ]
};

