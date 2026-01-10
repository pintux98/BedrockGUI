import React, { useEffect, useState } from "react";
import { useDesignerStore } from "../core/store";
import { designerSchema } from "../core/schemas";
import { stateToSnippetYaml, yamlToStateDoc } from "../core/yaml";
import { deserializeActions } from "../core/yaml";

export function YamlEditorPanel() {
  const state = useDesignerStore();
  const {
    setMenuName,
    setBedrock,
    setJava,
    setGlobalActions
  } = useDesignerStore();
  const [text, setText] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [collapsed, setCollapsed] = useState(() => {
    return localStorage.getItem("yaml_panel_collapsed") === "true";
  });

  useEffect(() => {
    localStorage.setItem("yaml_panel_collapsed", String(collapsed));
  }, [collapsed]);

  useEffect(() => {
    setText(stateToSnippetYaml(state));
  }, [state]);

  const applyYaml = () => {
    try {
      const { menuName, entry, configVersion } = yamlToStateDoc(text);
      const bedrockEntry = entry?.bedrock ?? (entry?.type ? entry : undefined);
      const javaEntry = entry?.java ?? (entry?.type && entry?.items ? entry : undefined);

      const nextMenuName =
        String(text).includes("\nforms:") || String(text).includes("\r\nforms:")
          ? menuName
          : state.menuName;

      const nextBedrock = parseBedrockFromYaml(bedrockEntry);
      const nextJava = parseJavaFromYaml(javaEntry);
      const bedrockGlobal = bedrockEntry?.global_actions;
      const legacyGlobal = entry?.global_actions;
      const nextGlobalActions =
        (bedrockGlobal?.length ? deserializeActions(bedrockGlobal) : undefined) ??
        (legacyGlobal?.length ? deserializeActions(legacyGlobal) : undefined) ??
        undefined;

      const validation = designerSchema.safeParse({
        configVersion: configVersion ?? "1.0.0",
        menuName: nextMenuName,
        platform: state.platform,
        bedrock: nextBedrock,
        java: nextJava,
        globalActions: nextGlobalActions
      });
      if (!validation.success) {
        setError(validation.error.errors[0]?.message ?? "Invalid YAML");
        return;
      }

      setError(null);
      setMenuName(nextMenuName);
      setBedrock(nextBedrock);
      setJava(nextJava);
      setGlobalActions(nextGlobalActions);
    } catch (e: any) {
      setError(String(e.message ?? e));
    }
  };

  return (
    <div 
      className={`ui-panel flex flex-col transition-all duration-300 ease-in-out border-t border-brand-border ${
        collapsed ? "flex-none h-8" : "flex-1 min-h-0"
      }`}
    >
      <div 
        className="ui-panel-title flex items-center justify-between cursor-pointer hover:bg-brand-surface2"
        onClick={() => setCollapsed(!collapsed)}
      >
        <span>Form YAML</span>
        <span className="text-xs text-brand-muted">{collapsed ? "Show ▲" : "Hide ▼"}</span>
      </div>
      
      {!collapsed && (
        <div className="flex-1 flex flex-col min-h-0 p-2 overflow-hidden">
          <textarea
            className="ui-textarea flex-1 text-xs font-mono resize-none mb-2 w-full h-full"
            value={text}
            onChange={(e) => setText(e.target.value)}
            spellCheck={false}
          />
          <div className="flex items-center gap-2 shrink-0">
            <button
              className="ui-btn ui-btn-primary w-full"
              onClick={applyYaml}
            >
              Apply Changes
            </button>
          </div>
          {error && (
            <div className="mt-2 p-2 bg-red-900/20 border border-red-900/40 text-red-400 text-xs font-mono break-all shrink-0">
              {error}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function normalizeModalButtons(buttons: any[]) {
  const trimmed = buttons.slice(0, 2);
  while (trimmed.length < 2) trimmed.push({ id: trimmed.length === 0 ? "yes" : "no", text: trimmed.length === 0 ? "Yes" : "No" });
  return trimmed;
}

function parseBedrockFromYaml(bedrockEntry: any) {
  if (!bedrockEntry?.type || !bedrockEntry?.title) return undefined;
  if (bedrockEntry.type === "SIMPLE" || bedrockEntry.type === "MODAL") {
    const parsedButtons = Object.entries(bedrockEntry.buttons ?? {}).map(([id, b]: any) => ({
      id,
      text: b.text,
      image: b.image,
      onClick: deserializeActions(b.onClick),
      showCondition: b.show_condition,
      alternativeText: b.alternative_text,
      alternativeImage: b.alternative_image,
      alternativeOnClick: b.alternative_onClick,
      conditions: b.conditions
        ? Object.entries(b.conditions).map(([cid, c]: any) => ({
            id: cid,
            condition: c.condition,
            property: c.property,
            value: c.value
          }))
        : undefined
    }));
    const buttons =
      bedrockEntry.type === "MODAL"
        ? normalizeModalButtons(parsedButtons)
        : parsedButtons.length
          ? parsedButtons
          : [{ id: "button_1", text: "Button 1" }];
    return {
      type: bedrockEntry.type,
      command: bedrockEntry.command,
      commandIntercept: bedrockEntry.command_intercept,
      permission: bedrockEntry.permission,
      title: bedrockEntry.title,
      content: bedrockEntry.content ?? bedrockEntry.description,
      buttons
    };
  }
  if (bedrockEntry.type === "CUSTOM") {
    return {
      type: "CUSTOM",
      command: bedrockEntry.command,
      commandIntercept: bedrockEntry.command_intercept,
      permission: bedrockEntry.permission,
      title: bedrockEntry.title,
      components: Object.entries(bedrockEntry.components ?? {}).map(([id, c]: any) => ({
        id,
        type: c.type,
        props: Object.fromEntries(Object.entries(c).filter(([k]) => !["type", "onClick"].includes(k))),
        onClick: deserializeActions(c.onClick)
      }))
    };
  }
  return undefined;
}

function parseJavaFromYaml(javaEntry: any) {
  if (!javaEntry?.type) return undefined;
  const items = javaEntry.items
    ? Object.entries(javaEntry.items).map(([slot, v]: [string, any]) => ({
        slot: Number(slot),
        material: v.material,
        amount: v.amount,
        name: v.name,
        glow: v.glow,
        lore: v.lore,
        onClick: deserializeActions(v.onClick)
      }))
    : [];
  const fillsObj = javaEntry.fills && typeof javaEntry.fills === "object" ? javaEntry.fills : undefined;
  const fillsArr = fillsObj
    ? Object.entries(fillsObj).map(([id, v]: any) => ({
        id,
        type: v.type,
        row: v.row !== undefined ? Number(v.row) : undefined,
        column: v.column !== undefined ? Number(v.column) : undefined,
        item: {
          material: v.item?.material,
          amount: v.item?.amount,
          name: v.item?.name,
          glow: v.item?.glow,
          lore: v.item?.lore,
          onClick: deserializeActions(v.item?.onClick)
        }
      }))
    : [];
  const legacyFill = javaEntry.fill;
  const legacyArr = legacyFill
    ? [
        {
          id: "fill_1",
          type: legacyFill.type,
          row: legacyFill.row !== undefined ? Number(legacyFill.row) : undefined,
          column: legacyFill.column !== undefined ? Number(legacyFill.column) : undefined,
          item: {
            material: legacyFill.item?.material,
            amount: legacyFill.item?.amount,
            name: legacyFill.item?.name,
            glow: legacyFill.item?.glow,
            lore: legacyFill.item?.lore,
            onClick: deserializeActions(legacyFill.item?.onClick)
          }
        }
      ]
    : [];
  const fills = fillsArr.length ? fillsArr : legacyArr.length ? legacyArr : undefined;
  return {
    type: javaEntry.type,
    title: javaEntry.title,
    size: javaEntry.size,
    items,
    fills
  };
}
