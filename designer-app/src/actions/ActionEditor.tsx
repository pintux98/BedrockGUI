import React, { useMemo } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ActionRegistry } from "./registry";
import { ActionInstance } from "../core/types";

export function ActionEditor({
  action,
  onChange
}: {
  action: ActionInstance;
  onChange: (a: ActionInstance) => void;
}) {
  const def = ActionRegistry[action.id];
  const defaultValues = useMemo(() => action.params, [action]);
  const form = useForm({ defaultValues, resolver: zodResolver(def.schema as any) });
  return (
    <form
      className="space-y-2"
      onChange={() => onChange({ ...action, params: form.getValues() })}
    >
      <div className="text-xs text-gray-400">{def.label}</div>
      {"length" in def.schema && (
        <textarea
          className="w-full h-24 bg-gray-800 border border-gray-700 rounded px-2 py-1 text-xs"
          placeholder="One entry per line"
          value={(form.getValues() as any[] | string)?.join?.("\n") ?? ""}
          onChange={(e) =>
            onChange({
              ...action,
              params: e.target.value.split("\n").map((s) => s.trim()).filter(Boolean)
            })
          }
        />
      )}
      {"shape" in (def.schema as any) &&
        Object.keys((def.schema as any).shape).map((key) => (
          <div key={key} className="flex gap-2 items-center">
            <label className="w-28 text-sm">{key}</label>
            <input
              className="flex-1 bg-gray-800 border border-gray-700 rounded px-2 py-1"
              {...form.register(key as any)}
            />
          </div>
        ))}
    </form>
  );
}
