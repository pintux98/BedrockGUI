import yaml from "js-yaml";
import { DesignerState } from "./types";
import { ActionInstance } from "./types";
import { JavaMenuFill } from "./types";

export function stateToYaml(state: DesignerState): string {
  const forms: Record<string, unknown> = {};
  const key = state.menuName;
  forms[key] = stateToFormEntry(state);
  return yaml.dump({ forms, configVersion: state.configVersion }, { lineWidth: -1, noRefs: true, forceQuotes: true });
}

export function stateToSnippetYaml(state: DesignerState): string {
  return yaml.dump(stateToFormEntry(state), { lineWidth: -1, noRefs: true, forceQuotes: true });
}

export function stateToFormEntry(state: DesignerState): Record<string, unknown> {
  const entry: Record<string, unknown> = {};

  if (state.bedrock) {
    const bedrock: Record<string, any> = {};
    if (state.bedrock.command) bedrock["command"] = state.bedrock.command;
    if (state.bedrock.commandIntercept) bedrock["command_intercept"] = state.bedrock.commandIntercept;
    if (state.bedrock.permission) bedrock["permission"] = state.bedrock.permission;
    bedrock["type"] = state.bedrock.type;
    bedrock["title"] = state.bedrock.title;
    if ("content" in state.bedrock && state.bedrock.content) {
      bedrock["description"] = state.bedrock.content;
    }
    if (state.bedrock.type !== "CUSTOM") {
      const buttons: Record<string, any> = {};
      for (const b of state.bedrock.buttons ?? []) {
        buttons[b.id] = stripUndefined({
          text: b.text,
          image: b.image,
          onClick: serializeActionBlocks(b.onClick),
          show_condition: b.showCondition,
          alternative_text: b.alternativeText,
          alternative_image: b.alternativeImage,
          alternative_onClick: b.alternativeOnClick,
          conditions: b.conditions?.length
            ? Object.fromEntries(
                b.conditions.map((c) => [
                  c.id,
                  stripUndefined({
                    condition: c.condition,
                    property: c.property,
                    value: c.value
                  })
                ])
              )
            : undefined
        });
      }
      bedrock["buttons"] = buttons;
    } else {
      bedrock["components"] = Object.fromEntries(
        (state.bedrock.components ?? []).map((c) => [
          c.id,
          stripUndefined({
            type: c.type,
            ...c.props,
            onClick: serializeActionBlocks(c.onClick)
          })
        ])
      );
    }
    if (state.globalActions?.length) {
      bedrock["global_actions"] = serializeActionBlocks(state.globalActions);
    }
    entry["bedrock"] = stripUndefined(bedrock);
  }

  if (state.java) {
    const fills = serializeJavaFills(state.java.fills);
    entry["java"] = stripUndefined({
      type: state.java.type,
      title: state.java.title,
      size: state.java.size,
      items: Object.fromEntries(
        state.java.items.map((i) => [
          i.slot,
          {
            material: i.material,
            amount: i.amount,
            name: i.name,
            lore: i.lore,
            glow: i.glow,
            onClick: serializeActionBlocks(i.onClick)
          }
        ])
      ),
      ...(fills ?? {})
    });
  }

  return stripUndefined(entry);
}

export function yamlToStateDoc(text: string): { menuName: string; entry: any; configVersion?: string } {
  const doc = yaml.load(text) as any;
  const forms = doc?.forms ?? {};
  const key = Object.keys(forms)[0];
  if (key) return { menuName: key, entry: forms[key], configVersion: doc?.configVersion };
  if (doc?.bedrock || doc?.java) {
    return { menuName: "example", entry: doc, configVersion: doc?.configVersion };
  }
  throw new Error("No forms.<menu_name> found");
}

export function deserializeActions(onClick: any): ActionInstance[] | undefined {
  if (typeof onClick === "string" && onClick.trim()) {
    return [{ id: "raw", params: onClick.trim(), raw: onClick.trim() } as ActionInstance];
  }
  if (!Array.isArray(onClick) || !onClick.length) return undefined;
  return onClick
    .map((v) => {
      if (typeof v !== "string") return null;
      const raw = v.trim();
      if (!raw) return null;
      return { id: "raw", params: raw, raw } as ActionInstance;
    })
    .filter(Boolean) as ActionInstance[];
}

function serializeActionBlocks(actions?: ActionInstance[]) {
  if (!actions?.length) return undefined;
  return actions
    .map((a) => {
      if (typeof a.raw === "string" && a.raw.trim()) return a.raw.trim();
      if (typeof a.params === "string" && a.params.trim()) return a.params.trim();
      return null;
    })
    .filter(Boolean);
}

function serializeJavaFills(fills?: JavaMenuFill[]) {
  if (!fills?.length) return undefined;
  const normalized = fills.filter((f) => f && f.item && f.type);
  if (!normalized.length) return undefined;
  if (normalized.length === 1) {
    const f = normalized[0];
    return {
      fill: stripUndefined({
        type: f.type,
        row: f.row,
        column: f.column,
        item: stripUndefined({
          material: f.item.material,
          amount: f.item.amount,
          name: f.item.name,
          lore: f.item.lore,
          glow: f.item.glow,
          onClick: serializeActionBlocks(f.item.onClick)
        })
      })
    };
  }
  return {
    fills: Object.fromEntries(
      normalized.map((f) => [
        f.id,
        stripUndefined({
          type: f.type,
          row: f.row,
          column: f.column,
          item: stripUndefined({
            material: f.item.material,
            amount: f.item.amount,
            name: f.item.name,
            lore: f.item.lore,
            glow: f.item.glow,
            onClick: serializeActionBlocks(f.item.onClick)
          })
        })
      ])
    )
  };
}

function stripUndefined<T extends Record<string, any>>(obj: T): T {
  const out: any = Array.isArray(obj) ? [] : {};
  for (const [k, v] of Object.entries(obj)) {
    if (v === undefined) continue;
    if (v && typeof v === "object" && !Array.isArray(v)) out[k] = stripUndefined(v as any);
    else out[k] = v;
  }
  return out;
}
