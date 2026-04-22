import { JavaMenu, JavaMenuType } from "./types";

export const JAVA_MENU_TYPE_OPTIONS: Array<{ value: JavaMenuType; label: string }> = [
  { value: "CHEST", label: "Chest" }
];

const FIXED_SLOT_COUNTS: Record<Exclude<JavaMenuType, "CHEST">, number> = {
  ANVIL: 3,
  BARREL: 27,
  BEACON: 1,
  BLAST_FURNACE: 3,
  BREWING: 5,
  CARTOGRAPHY: 3,
  CRAFTING: 10,
  CREATIVE: 54,
  DISPENSER: 9,
  DROPPER: 9,
  ENCHANTING: 2,
  ENDER_CHEST: 27,
  FURNACE: 3,
  GRINDSTONE: 3,
  HOPPER: 5,
  LECTERN: 1,
  LOOM: 4,
  MERCHANT: 3,
  PLAYER: 46,
  SHULKER_BOX: 27,
  SMITHING: 4,
  SMOKER: 3,
  STONECUTTER: 2,
  WORKBENCH: 10
};

const GRID_COLUMNS: Partial<Record<JavaMenuType, number>> = {
  CHEST: 9,
  BARREL: 9,
  ENDER_CHEST: 9,
  SHULKER_BOX: 9,
  PLAYER: 9,
  CREATIVE: 9,
  DISPENSER: 3,
  DROPPER: 3,
  HOPPER: 5,
  BREWING: 5,
  LOOM: 2,
  SMITHING: 2
};

export function supportsJavaMenuSize(type: JavaMenuType) {
  return type === "CHEST";
}

export function getJavaMenuSlotCount(type: JavaMenuType, size?: number) {
  if (type === "CHEST") {
    const resolved = Number(size ?? 27);
    if (!Number.isFinite(resolved)) return 27;
    const clamped = Math.max(9, Math.min(54, resolved));
    return Math.ceil(clamped / 9) * 9;
  }
  return FIXED_SLOT_COUNTS[type];
}

export function getJavaMenuMaxSlot(type: JavaMenuType, size?: number) {
  return getJavaMenuSlotCount(type, size) - 1;
}

export function isJavaSlotValid(menu: JavaMenu, slot: number) {
  if (!Number.isInteger(slot)) return false;
  return slot >= 0 && slot <= getJavaMenuMaxSlot(menu.type, menu.size);
}

export function getJavaMenuGridColumns(type: JavaMenuType, size?: number) {
  if (type === "CHEST") return 9;
  return GRID_COLUMNS[type] ?? Math.min(getJavaMenuSlotCount(type, size), 9);
}

export function coerceJavaMenuType(prev: JavaMenu, type: JavaMenuType): JavaMenu {
  const size = supportsJavaMenuSize(type) ? prev.size ?? 27 : undefined;
  const maxSlot = getJavaMenuMaxSlot(type, size);
  const items = (prev.items ?? []).filter((i) => i.slot >= 0 && i.slot <= maxSlot);
  const fills = type === "CHEST" ? prev.fills : undefined;
  return { ...prev, type, size, items, fills };
}
