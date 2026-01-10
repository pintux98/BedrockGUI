import { z } from "zod";
import { BedrockFormType, JavaFillType, JavaMenuType } from "./types";

export const actionSchema = z.object({
  id: z.string(),
  params: z.any(),
  raw: z.string().optional()
});

export const bedrockButtonSchema = z.object({
  id: z.string().min(1),
  text: z.string().min(1),
  image: z.string().optional(),
  onClick: z.array(actionSchema).optional(),
  showCondition: z.string().optional(),
  alternativeText: z.string().optional(),
  alternativeImage: z.string().optional(),
  alternativeOnClick: z.string().optional(),
  conditions: z
    .array(
      z.object({
        id: z.string().min(1),
        condition: z.string().min(1),
        property: z.enum(["text", "image", "onClick"] as unknown as any[]),
        value: z.string()
      })
    )
    .optional()
});

export const bedrockComponentSchema = z.object({
  id: z.string(),
  type: z.enum([
    "label",
    "input",
    "dropdown",
    "toggle",
    "slider",
    "stepper",
    "imageButton"
  ]),
  props: z.record(z.any()),
  onClick: z.array(actionSchema).optional()
});

export const bedrockBaseSchema = z.object({
  type: z.enum(["SIMPLE", "MODAL", "CUSTOM"] as unknown as BedrockFormType[]),
  title: z.string().min(1),
  content: z.string().optional(),
  description: z.string().optional(),
  command: z.string().optional(),
  commandIntercept: z.string().optional(),
  permission: z.string().optional()
});

export const bedrockSimpleSchema = bedrockBaseSchema.extend({
  type: z.literal("SIMPLE"),
  buttons: z.array(bedrockButtonSchema).min(1)
});

export const bedrockModalSchema = bedrockBaseSchema.extend({
  type: z.literal("MODAL"),
  buttons: z.array(bedrockButtonSchema).length(2)
});

export const bedrockCustomSchema = bedrockBaseSchema.extend({
  type: z.literal("CUSTOM"),
  components: z.array(bedrockComponentSchema).min(1)
});

export const javaItemSchema = z.object({
  slot: z.number().min(0),
  material: z.string().min(1),
  amount: z.number().min(1).max(64).optional(),
  name: z.string().optional(),
  lore: z.array(z.string()).optional(),
  glow: z.boolean().optional(),
  onClick: z.array(actionSchema).optional()
});

export const javaMenuSchema = z.object({
  type: z.enum(["CHEST", "ANVIL", "CRAFTING"] as unknown as JavaMenuType[]),
  title: z.string().min(1),
  size: z.number().min(9).max(54).optional(),
  items: z.array(javaItemSchema),
  fills: z
    .array(
      z.object({
        id: z.string().min(1),
        type: z.enum(["ROW", "COLUMN", "EMPTY"] as unknown as JavaFillType[]),
        row: z.number().int().optional(),
        column: z.number().int().optional(),
        item: z.object({
          material: z.string().min(1),
          amount: z.number().min(1).max(64).optional(),
          name: z.string().optional(),
          lore: z.array(z.string()).optional(),
          glow: z.boolean().optional(),
          onClick: z.array(actionSchema).optional()
        })
      })
    )
    .optional()
});

export const designerSchema = z.object({
  configVersion: z.literal("1.0.0"),
  menuName: z.string().min(1),
  platform: z.enum(["bedrock", "java"]),
  bedrock: z.union([bedrockSimpleSchema, bedrockModalSchema, bedrockCustomSchema]).optional(),
  java: javaMenuSchema.optional(),
  globalActions: z.array(actionSchema).optional()
});

