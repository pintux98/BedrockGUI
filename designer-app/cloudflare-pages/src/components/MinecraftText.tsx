import React from "react";
import { MinecraftTextStyle, parseMinecraftText } from "../core/minecraftText";

export function MinecraftText({ text }: { text: string }) {
  const segments = React.useMemo(() => parseMinecraftText(text), [text]);
  return (
    <>
      {segments.map((s, idx) => (
        <span key={idx} style={minecraftTextStyleToCss(s.style)}>
          {s.text}
        </span>
      ))}
    </>
  );
}

function minecraftTextStyleToCss(style: MinecraftTextStyle): React.CSSProperties {
  const decorations: string[] = [];
  if (style.underline) decorations.push("underline");
  if (style.strikethrough) decorations.push("line-through");
  return {
    color: style.color,
    fontWeight: style.bold ? "bold" : undefined,
    fontStyle: style.italic ? "italic" : undefined,
    textDecoration: decorations.length ? decorations.join(" ") : undefined
  };
}
