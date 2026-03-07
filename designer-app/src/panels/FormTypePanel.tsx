import React from "react";
import { useDesignerStore } from "../core/store";
import { coerceJavaMenuType, JAVA_MENU_TYPE_OPTIONS } from "../core/javaMenu";
import { JavaMenu, JavaMenuType } from "../core/types";

export function FormTypePanel() {
  const { platform, bedrock, java, setBedrock, setJava, setPlatform } =
    useDesignerStore();
  return (
    <div className="ui-panel">
      <div className="ui-panel-title">Form Type</div>
      <div className="flex flex-col gap-3">
        <div className="flex flex-col gap-1">
          <label className="text-sm text-brand-muted">Platform</label>
          <select
            className="ui-input w-full"
            value={platform}
            onChange={(e) => setPlatform(e.target.value as "bedrock" | "java")}
          >
            <option value="bedrock">Bedrock</option>
            <option value="java">Java</option>
          </select>
        </div>
        {platform === "bedrock" && bedrock && (
          <div className="flex flex-col gap-1">
            <label className="text-sm text-brand-muted">Bedrock Type</label>
            <select
              className="ui-input w-full"
              value={bedrock.type}
              onChange={(e) =>
                setBedrock(coerceBedrockType(bedrock, e.target.value as any))
              }
            >
              <option value="SIMPLE">Simple Form</option>
              <option value="MODAL">Modal Form</option>
              <option value="CUSTOM">Custom Form</option>
            </select>
          </div>
        )}
        {platform === "java" && java && (
          <div className="flex flex-col gap-1">
            <label className="text-sm text-brand-muted">Java Menu</label>
            <select
              className="ui-input w-full"
              value={java.type}
              onChange={(e) =>
                setJava(coerceJavaMenuType(java as JavaMenu, e.target.value as JavaMenuType))
              }
            >
              {JAVA_MENU_TYPE_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>
    </div>
  );
}

function coerceBedrockType(prev: any, type: "SIMPLE" | "MODAL" | "CUSTOM") {
  if (type === "SIMPLE") {
    return {
      type,
      title: prev.title ?? "Form",
      content: prev.content ?? "",
      buttons:
        Array.isArray(prev.buttons) && prev.buttons.length
          ? prev.buttons.map((b: any, i: number) => ({ id: b.id ?? `button_${i + 1}`, ...b }))
          : [{ id: "button_1", text: "Button 1" }]
    };
  }
  if (type === "MODAL") {
    const buttonsRaw =
      Array.isArray(prev.buttons) && prev.buttons.length >= 2
        ? prev.buttons.slice(0, 2)
        : [{ id: "yes", text: "Yes" }, { id: "no", text: "No" }];
    const buttons = buttonsRaw.map((b: any, i: number) => ({ id: b.id ?? (i === 0 ? "yes" : "no"), ...b }));
    return {
      type,
      title: prev.title ?? "Modal",
      content: prev.content ?? "",
      buttons
    };
  }
  return {
    type,
    title: prev.title ?? "Custom",
    components: Array.isArray(prev.components) ? prev.components : []
  };
}
