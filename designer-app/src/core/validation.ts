import { BedrockForm, BedrockComponentType } from "./types";

export function canDropBedrockComponent(form: BedrockForm, t: BedrockComponentType) {
  if (form.type === "CUSTOM") return true;
  return false;
}
