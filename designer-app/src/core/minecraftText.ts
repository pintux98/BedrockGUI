export type MinecraftTextStyle = {
  color?: string;
  bold?: boolean;
  italic?: boolean;
  underline?: boolean;
  strikethrough?: boolean;
};

export type MinecraftTextSegment = {
  text: string;
  style: MinecraftTextStyle;
};

const LEGACY_COLORS: Record<string, string> = {
  "0": "#000000",
  "1": "#0000AA",
  "2": "#00AA00",
  "3": "#00AAAA",
  "4": "#AA0000",
  "5": "#AA00AA",
  "6": "#FFAA00",
  "7": "#AAAAAA",
  "8": "#555555",
  "9": "#5555FF",
  a: "#55FF55",
  b: "#55FFFF",
  c: "#FF5555",
  d: "#FF55FF",
  e: "#FFFF55",
  f: "#FFFFFF"
};

function isHexDigit(c: string) {
  return /^[0-9a-fA-F]$/.test(c);
}

function tryReadHexColor(input: string, start: number) {
  if (start + 7 > input.length) return null;
  if (input[start] !== "#") return null;
  const hex = input.slice(start + 1, start + 7);
  if (!/^[0-9a-fA-F]{6}$/.test(hex)) return null;
  return { color: `#${hex.toUpperCase()}`, next: start + 7 };
}

function tryReadAmpSectionHexColor(input: string, start: number) {
  const ch = input[start];
  if (ch !== "&" && ch !== "§") return null;
  if (start + 8 > input.length) return null;
  if (input[start + 1] !== "#") return null;
  const hex = input.slice(start + 2, start + 8);
  if (!/^[0-9a-fA-F]{6}$/.test(hex)) return null;
  return { color: `#${hex.toUpperCase()}`, next: start + 8 };
}

function tryReadXSequenceHexColor(input: string, start: number) {
  const ch = input[start];
  if (ch !== "&" && ch !== "§") return null;
  const prefix = ch;
  if (start + 2 > input.length) return null;
  if (input[start + 1].toLowerCase() !== "x") return null;
  let i = start + 2;
  const digits: string[] = [];
  for (let n = 0; n < 6; n++) {
    if (i + 2 > input.length) return null;
    if (input[i] !== prefix) return null;
    const d = input[i + 1];
    if (!isHexDigit(d)) return null;
    digits.push(d.toUpperCase());
    i += 2;
  }
  return { color: `#${digits.join("")}`, next: i };
}

export function stripMinecraftCodes(input: string) {
  return parseMinecraftText(input)
    .map((s) => s.text)
    .join("");
}

export function parseMinecraftText(input: string): MinecraftTextSegment[] {
  const out: MinecraftTextSegment[] = [];
  let style: MinecraftTextStyle = {};
  let buf = "";

  const flush = () => {
    if (!buf) return;
    out.push({ text: buf, style: { ...style } });
    buf = "";
  };

  const setStyle = (next: MinecraftTextStyle) => {
    flush();
    style = next;
  };

  const applyStyle = (patch: Partial<MinecraftTextStyle>) => {
    flush();
    style = { ...style, ...patch };
  };

  let i = 0;
  while (i < input.length) {
    const ch = input[i];

    if (ch === "&" || ch === "§") {
      const next = input[i + 1];
      if (next === ch) {
        buf += ch;
        i += 2;
        continue;
      }

      const xSeq = tryReadXSequenceHexColor(input, i);
      if (xSeq) {
        setStyle({ color: xSeq.color });
        i = xSeq.next;
        continue;
      }

      const ampHex = tryReadAmpSectionHexColor(input, i);
      if (ampHex) {
        setStyle({ color: ampHex.color });
        i = ampHex.next;
        continue;
      }

      if (typeof next === "string") {
        const code = next.toLowerCase();
        if (LEGACY_COLORS[code]) {
          setStyle({ color: LEGACY_COLORS[code] });
          i += 2;
          continue;
        }
        if (code === "r") {
          setStyle({});
          i += 2;
          continue;
        }
        if (code === "l") {
          applyStyle({ bold: true });
          i += 2;
          continue;
        }
        if (code === "o") {
          applyStyle({ italic: true });
          i += 2;
          continue;
        }
        if (code === "n") {
          applyStyle({ underline: true });
          i += 2;
          continue;
        }
        if (code === "m") {
          applyStyle({ strikethrough: true });
          i += 2;
          continue;
        }
      }

      buf += ch;
      i += 1;
      continue;
    }

    if (ch === "#") {
      const hex = tryReadHexColor(input, i);
      if (hex) {
        setStyle({ color: hex.color });
        i = hex.next;
        continue;
      }
    }

    buf += ch;
    i += 1;
  }

  flush();
  return out;
}
