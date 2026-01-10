import { ActionInstance } from "../core/types";
import { z } from "zod";

export type ActionDef = {
  id: ActionInstance["id"] | string;
  label: string;
  schema: z.ZodType<any>;
  serialize: (params: any) => Record<string, unknown>;
};

const linesSchema = z.array(z.string().min(1)).min(1);

export const ActionRegistry: Record<string, ActionDef> = {
  actionbar: {
    id: "actionbar",
    label: "Action Bar",
    schema: linesSchema,
    serialize: (p) => ({ actionbar: p })
  },
  broadcast: {
    id: "broadcast",
    label: "Broadcast",
    schema: linesSchema,
    serialize: (p) => ({ broadcast: p })
  },
  bungee: {
    id: "bungee",
    label: "Bungee",
    schema: z.object({ subchannel: z.string().min(1), args: linesSchema.optional() }),
    serialize: (p) => ({ bungee: [{ subchannel: p.subchannel }, ...(p.args ?? [])] })
  },
  command: {
    id: "command",
    label: "Player Command",
    schema: linesSchema,
    serialize: (p) => ({ command: p })
  },
  conditional: {
    id: "conditional",
    label: "Conditional",
    schema: z.object({
      check: z.string().min(1),
      true: linesSchema.optional(),
      false: linesSchema.optional()
    }),
    serialize: (p) => ({ conditional: [{ check: p.check }, ...(p.true ?? []), ...(p.false ?? [])] })
  },
  delay: {
    id: "delay",
    label: "Delay",
    schema: z.array(z.string().min(1)).min(1),
    serialize: (p) => ({ delay: p })
  },
  economy: {
    id: "economy",
    label: "Economy",
    schema: linesSchema,
    serialize: (p) => ({ economy: p })
  },
  inventory: {
    id: "inventory",
    label: "Inventory",
    schema: linesSchema,
    serialize: (p) => ({ inventory: p })
  },
  message: {
    id: "message",
    label: "Message",
    schema: linesSchema,
    serialize: (p) => ({ message: p })
  },
  open: {
    id: "open",
    label: "Open Form",
    schema: linesSchema,
    serialize: (p) => ({ open: p })
  },
  url: {
    id: "url",
    label: "Open URL",
    schema: linesSchema,
    serialize: (p) => ({ url: p })
  },
  random: {
    id: "random",
    label: "Random",
    schema: linesSchema,
    serialize: (p) => ({ random: p })
  },
  server: {
    id: "server",
    label: "Server Command",
    schema: linesSchema,
    serialize: (p) => ({ server: p })
  },
  sound: {
    id: "sound",
    label: "Sound",
    schema: linesSchema,
    serialize: (p) => ({ sound: p })
  },
  title: {
    id: "title",
    label: "Title",
    schema: linesSchema,
    serialize: (p) => ({ title: p })
  }
};

