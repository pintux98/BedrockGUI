import { useDesignerStore } from "../core/store";
import { stateToYaml } from "../core/yaml";

export function useExporter() {
  const state = useDesignerStore();
  const exportYaml = () => {
    const doc = stateToYaml(state);
    const blob = new Blob([doc], { type: "text/yaml" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${state.menuName}.yml`;
    a.click();
    URL.revokeObjectURL(url);
  };
  return { exportYaml };
}

