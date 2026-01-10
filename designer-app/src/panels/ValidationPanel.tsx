import React from "react";
import { useDesignerStore } from "../core/store";
import { ActionInstance } from "../core/types";

type Issue = { level: "error" | "warning"; message: string };

const KNOWN_ACTION_TYPES = new Set([
  "command",
  "open",
  "message",
  "delay",
  "server",
  "broadcast",
  "sound",
  "economy",
  "title",
  "actionbar",
  "random",
  "conditional",
  "bungee",
  "url",
  "inventory"
]);

export function ValidationPanel() {
  const state = useDesignerStore();
  const issues = React.useMemo(() => validateState(state as any), [state]);
  const errors = issues.filter((i) => i.level === "error");
  const warnings = issues.filter((i) => i.level === "warning");
  const [expanded, setExpanded] = React.useState(false);

  if (!issues.length) {
    return (
      <div className="h-8 bg-brand-surface border-t border-brand-border flex items-center px-4 text-xs text-brand-muted">
        <span className="text-green-500 mr-2">✓</span> No validation issues
      </div>
    );
  }

  return (
    <div className="flex flex-col bg-brand-surface border-t border-brand-border min-w-0">
      {expanded && (
        <div className="max-h-48 overflow-y-auto p-2 border-b border-brand-border bg-brand-surface2">
          {errors.map((e, idx) => (
            <div key={`e-${idx}`} className="flex gap-2 text-xs text-red-400 bg-red-900/10 p-1 mb-1 border border-red-900/30 whitespace-pre-wrap break-words">
              <span className="font-bold shrink-0">!</span> {e.message}
            </div>
          ))}
          {warnings.map((w, idx) => (
            <div key={`w-${idx}`} className="flex gap-2 text-xs text-yellow-300 bg-yellow-900/10 p-1 mb-1 border border-yellow-900/30 whitespace-pre-wrap break-words">
              <span className="font-bold shrink-0">?</span> {w.message}
            </div>
          ))}
        </div>
      )}
      <div 
        className="h-8 flex items-center px-4 text-xs cursor-pointer hover:bg-brand-surface2 transition-colors justify-between overflow-hidden"
        onClick={() => setExpanded(!expanded)}
      >
        <div className="flex items-center gap-4 min-w-0 flex-1">
          <div className="font-bold text-brand-text shrink-0">Validation</div>
          {errors.length > 0 && (
            <div className="flex items-center gap-1 text-red-400 shrink-0">
              <span className="font-bold">{errors.length}</span> Errors
            </div>
          )}
          {warnings.length > 0 && (
            <div className="flex items-center gap-1 text-yellow-400 shrink-0">
              <span className="font-bold">{warnings.length}</span> Warnings
            </div>
          )}
          <div className="text-brand-muted truncate ml-4 opacity-75 min-w-0 flex-1">
            {errors[0]?.message ?? warnings[0]?.message}
          </div>
        </div>
        <div className="text-brand-muted shrink-0 ml-2">
          {expanded ? "▼" : "▲"}
        </div>
      </div>
    </div>
  );
}

function validateState(state: any): Issue[] {
  const out: Issue[] = [];

  if (state.bedrock) {
    const b = state.bedrock;
    if (b.type === "MODAL" && Array.isArray(b.buttons) && b.buttons.length !== 2) {
      out.push({ level: "error", message: "Bedrock MODAL must have exactly 2 buttons." });
    }
    if (b.commandIntercept && typeof b.commandIntercept === "string" && b.commandIntercept.trim().length < 2) {
      out.push({ level: "warning", message: "command_intercept looks too short." });
    }

    if (b.type === "SIMPLE" || b.type === "MODAL") {
      for (const btn of b.buttons ?? []) {
        if (btn?.image && !isLikelyValidImageSource(btn.image)) {
          out.push({ level: "warning", message: `Button '${btn.id}': image source looks invalid.` });
        }
        if (btn?.showCondition && !isValidConditionString(btn.showCondition)) {
          out.push({ level: "warning", message: `Button '${btn.id}': show_condition looks invalid.` });
        }
        for (const rule of btn?.conditions ?? []) {
          if (!rule.condition || !rule.property) continue;
          if (!isValidConditionString(rule.condition)) {
            out.push({ level: "warning", message: `Button '${btn.id}': override condition '${rule.id}' looks invalid.` });
          }
        }
        out.push(...validateActionBlocks(`Button '${btn.id}' onClick`, btn?.onClick));
      }
    }

    for (const comp of b.components ?? []) {
      out.push(...validateActionBlocks(`Component '${comp.id}' onClick`, comp?.onClick));
    }

    out.push(...validateActionBlocks("Global actions", state.globalActions));
    out.push(...detectCircularOpenReferences(state.menuName ?? "example", collectAllActionBlocks(state)));
  }

  if (state.java) {
    const j = state.java;
    if (j.type === "CHEST") {
      const size = j.size ?? 27;
      if (size % 9 !== 0 || size < 9 || size > 54) {
        out.push({ level: "warning", message: "Java CHEST size should be 9..54 and multiple of 9." });
      }
      for (const it of j.items ?? []) {
        if (it.slot < 0 || it.slot >= size) {
          out.push({ level: "warning", message: `Java item slot ${it.slot} is outside chest size ${size}.` });
        }
        out.push(...validateActionBlocks(`Java item slot ${it.slot} onClick`, it?.onClick));
      }
      for (const f of j.fills ?? []) {
        if (!f.id) out.push({ level: "warning", message: "Java fill missing id." });
        const rows = size / 9;
        if ((f.type === "ROW" || f.type === "EMPTY") && f.row !== undefined && (f.row < 1 || f.row > rows)) {
          out.push({ level: "warning", message: `Java fill '${f.id}': row should be 1..${rows}.` });
        }
        if ((f.type === "COLUMN" || f.type === "EMPTY") && f.column !== undefined && (f.column < 1 || f.column > 9)) {
          out.push({ level: "warning", message: `Java fill '${f.id}': column should be 1..9.` });
        }
        out.push(...validateActionBlocks(`Java fill '${f.id}' item.onClick`, f.item?.onClick));
      }
    }
    if (j.type === "ANVIL") {
      for (const it of j.items ?? []) {
        if (it.slot < 0 || it.slot > 2) out.push({ level: "warning", message: `ANVIL item slot ${it.slot} must be 0..2.` });
        out.push(...validateActionBlocks(`Java anvil slot ${it.slot} onClick`, it?.onClick));
      }
    }
  }

  return out;
}

function validateActionBlocks(label: string, actions?: ActionInstance[]): Issue[] {
  const out: Issue[] = [];
  for (const a of actions ?? []) {
    const raw = typeof a?.raw === "string" ? a.raw.trim() : typeof a?.params === "string" ? a.params.trim() : "";
    if (!raw) continue;
    const type = extractActionType(raw);
    if (!type) {
      out.push({ level: "warning", message: `${label}: could not detect action type for '${shorten(raw)}'.` });
      continue;
    }
    if (!KNOWN_ACTION_TYPES.has(type)) {
      out.push({ level: "warning", message: `${label}: unknown action type '${type}'.` });
    }
    if (raw.includes(":") && !raw.includes("{") && !raw.includes("}")) {
      out.push({ level: "warning", message: `${label}: legacy action format detected '${shorten(raw)}'.` });
    }
    const open = (raw.match(/\{/g) ?? []).length;
    const close = (raw.match(/\}/g) ?? []).length;
    if (open !== close) {
      out.push({ level: "warning", message: `${label}: braces may be unbalanced for '${type}'.` });
    }
  }
  return out;
}

function extractActionType(raw: string): string | undefined {
  const s = raw.trim();
  const brace = s.indexOf("{");
  if (brace > 0) return s.slice(0, brace).trim().toLowerCase();
  const colon = s.indexOf(":");
  if (colon > 0) return s.slice(0, colon).trim().toLowerCase();
  return undefined;
}

function shorten(s: string) {
  const t = s.replace(/\s+/g, " ").trim();
  return t.length > 60 ? `${t.slice(0, 57)}...` : t;
}

function isLikelyValidImageSource(image: string) {
  const v = image.trim();
  if (!v) return true;
  if (v.startsWith("http://") || v.startsWith("https://")) return true;
  if (v.startsWith("data:image/")) return true;
  if (v.includes("%") || v.includes("{") || v.includes("}")) return true;
  if (/^[A-Za-z0-9+/=]+$/.test(v) && v.length > 40) return true;
  if (/^[A-Za-z0-9_]{2,16}$/.test(v)) return true;
  return false;
}

function isValidConditionString(condition: string) {
  const s = condition.trim();
  if (!s) return true;
  const parts = s.split(":");
  if (parts.length < 2) return false;
  let offset = 0;
  if (parts[0] === "not") {
    offset = 1;
    if (parts.length < 3) return false;
  }
  const type = (parts[offset] ?? "").toLowerCase();
  if (type === "permission") return parts.length >= offset + 2;
  if (type === "plugin") return parts.length >= offset + 2;
  if (type === "placeholder") return parts.length >= offset + 4;
  if (type === "bedrock_player" || type === "java_player") return parts.length >= offset + 2;
  return false;
}

function collectAllActionBlocks(state: any): string[] {
  const out: string[] = [];
  const push = (a?: ActionInstance[]) => {
    for (const x of a ?? []) {
      const raw = typeof x?.raw === "string" ? x.raw.trim() : typeof x?.params === "string" ? x.params.trim() : "";
      if (raw) out.push(raw);
    }
  };
  if (state.bedrock) {
    if (state.bedrock.type === "SIMPLE" || state.bedrock.type === "MODAL") {
      for (const b of state.bedrock.buttons ?? []) push(b.onClick);
    }
    for (const c of state.bedrock.components ?? []) push(c.onClick);
  }
  push(state.globalActions);
  if (state.java) {
    for (const it of state.java.items ?? []) push(it.onClick);
    for (const f of state.java.fills ?? []) push(f.item?.onClick);
  }
  return out;
}

function detectCircularOpenReferences(menuName: string, blocks: string[]): Issue[] {
  const edges = new Map<string, Set<string>>();
  edges.set(menuName, new Set());
  for (const b of blocks) {
    const type = extractActionType(b);
    if (type !== "open") continue;
    const target = extractFirstQuotedValue(b) ?? extractOpenColonValue(b);
    if (!target) continue;
    edges.get(menuName)!.add(target.toLowerCase());
  }
  const visited = new Set<string>();
  const stack = new Set<string>();
  const issues: Issue[] = [];

  const dfs = (n: string) => {
    if (stack.has(n)) {
      issues.push({ level: "warning", message: `Possible circular open-form reference detected involving '${n}'.` });
      return;
    }
    if (visited.has(n)) return;
    visited.add(n);
    stack.add(n);
    for (const nxt of edges.get(n) ?? []) dfs(nxt);
    stack.delete(n);
  };

  dfs(menuName.toLowerCase());
  return issues;
}

function extractFirstQuotedValue(block: string): string | undefined {
  const m = block.match(/-\s*\"((?:[^\"\\\\]|\\\\.)*)\"/);
  return m?.[1];
}

function extractOpenColonValue(block: string): string | undefined {
  const m = block.match(/^\s*open\s*:\s*(.+)\s*$/i);
  if (!m?.[1]) return undefined;
  return m[1].trim().replace(/^\"|\"$/g, "");
}

