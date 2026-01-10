export type Platform = "bedrock" | "java";

export type BedrockFormType = "SIMPLE" | "MODAL" | "CUSTOM";

export type ConfigVersion = "1.0.0";

export interface DesignerState {
  menuName: string;
  platform: Platform;
  bedrock?: BedrockForm;
  java?: JavaMenu;
  globalActions?: ActionInstance[];
  configVersion: ConfigVersion;
}

export interface BedrockFormBase {
  type: BedrockFormType;
  title: string;
  content?: string;
  description?: string;
  command?: string;
  commandIntercept?: string;
  permission?: string;
}

export interface BedrockSimple extends BedrockFormBase {
  type: "SIMPLE";
  buttons: BedrockButton[];
}

export interface BedrockModal extends BedrockFormBase {
  type: "MODAL";
  buttons: BedrockButton[]; // two buttons typical
}

export interface BedrockCustom extends BedrockFormBase {
  type: "CUSTOM";
  components: BedrockComponent[];
}

export type BedrockForm = BedrockSimple | BedrockModal | BedrockCustom;

export interface BedrockButton {
  id: string;
  text: string;
  translations?: Record<string, string>;
  image?: string;
  onClick?: ActionInstance[];
  showCondition?: string;
  alternativeText?: string;
  alternativeImage?: string;
  alternativeOnClick?: string;
  conditions?: BedrockButtonConditionRule[];
}

export type BedrockButtonConditionProperty = "text" | "image" | "onClick";

export interface BedrockButtonConditionRule {
  id: string;
  condition: string;
  property: BedrockButtonConditionProperty;
  value: string;
}

export type BedrockComponentType =
  | "label"
  | "input"
  | "dropdown"
  | "toggle"
  | "slider"
  | "stepper"
  | "imageButton";

export interface BedrockComponent {
  id: string;
  type: BedrockComponentType;
  props: Record<string, unknown>;
  onClick?: ActionInstance[];
}

export type JavaMenuType = "CHEST" | "ANVIL" | "CRAFTING";

export interface JavaMenuItem {
  slot: number;
  material: string;
  amount?: number;
  name?: string;
  lore?: string[];
  glow?: boolean;
  onClick?: ActionInstance[];
}

export type JavaFillType = "ROW" | "COLUMN" | "EMPTY";

export interface JavaMenuFillItem {
  material: string;
  amount?: number;
  name?: string;
  lore?: string[];
  glow?: boolean;
  onClick?: ActionInstance[];
}

export interface JavaMenuFill {
  id: string;
  type: JavaFillType;
  row?: number;
  column?: number;
  item: JavaMenuFillItem;
}

export interface JavaMenu {
  type: JavaMenuType;
  title: string;
  size?: number; // for CHEST
  items: JavaMenuItem[];
  fills?: JavaMenuFill[];
}

export interface ActionInstance {
  id: string;
  params: any;
  raw?: string;
}

