import { useDesignerStore } from "../core/store";
import { deserializeActions, yamlToStateDoc } from "../core/yaml";

export function useImporter() {
  const {
    setMenuName,
    setBedrock,
    setJava,
    setGlobalActions
  } = useDesignerStore();

  const importYaml = async (file: File) => {
    const text = await file.text();
    const parsed = yamlToStateDoc(text);
    setMenuName(parsed.menuName);
    const entry = parsed.entry ?? {};
    const bedrockEntry = entry?.bedrock ?? (entry?.type ? entry : undefined);
    if (bedrockEntry?.type && bedrockEntry?.title) {
      if (bedrockEntry.type === "SIMPLE" || bedrockEntry.type === "MODAL") {
        const parsedButtons = Object.entries(bedrockEntry.buttons ?? {}).map(([id, b]: any) => ({
          id,
          text: b.text,
          image: b.image,
          onClick: deserializeActions(b.onClick)
        }));
        const buttons =
          bedrockEntry.type === "MODAL"
            ? normalizeModalButtons(parsedButtons)
            : parsedButtons.length
              ? parsedButtons
              : [{ id: "button_1", text: "Button 1" }];
        setBedrock({
          type: bedrockEntry.type,
          command: bedrockEntry.command,
          permission: bedrockEntry.permission,
          title: bedrockEntry.title,
          content: bedrockEntry.content ?? bedrockEntry.description,
          buttons
        });
      } else if (bedrockEntry.type === "CUSTOM") {
        setBedrock({
          type: "CUSTOM",
          command: bedrockEntry.command,
          permission: bedrockEntry.permission,
          title: bedrockEntry.title,
          components: Object.entries(bedrockEntry.components ?? {}).map(([id, c]: any) => ({
            id,
            type: c.type,
            props: Object.fromEntries(Object.entries(c).filter(([k]) => !["type", "onClick"].includes(k))),
            onClick: deserializeActions(c.onClick)
          }))
        });
      }
    }
    const javaEntry = entry?.java ?? (entry?.type && entry?.items ? entry : undefined);
    if (javaEntry?.type) {
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
      setJava({
        type: javaEntry.type,
        title: javaEntry.title,
        size: javaEntry.size,
        items
      });
    }
    if (entry?.global_actions?.length) {
      setGlobalActions(
        deserializeActions(entry.global_actions) ?? []
      );
    }
  };
  return { importYaml };
}

function normalizeModalButtons(buttons: any[]) {
  const trimmed = buttons.slice(0, 2);
  while (trimmed.length < 2) trimmed.push({ id: trimmed.length === 0 ? "yes" : "no", text: trimmed.length === 0 ? "Yes" : "No" });
  return trimmed;
}
