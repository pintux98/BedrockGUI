import React from "react";

export function IconTile({
  id,
  name,
  iconUrl,
  onClick
}: {
  id: string;
  name: string;
  iconUrl?: string;
  onClick?: () => void;
}) {
  return (
    <button
      className="w-10 h-10 bg-[#8b8b8b] flex items-center justify-center relative group"
      title={name}
      onClick={onClick}
      style={{
        boxShadow: "inset 2px 2px 0px rgba(255,255,255,0.5), inset -2px -2px 0px rgba(0,0,0,0.5)"
      }}
    >
      <div className="absolute inset-0 border border-transparent group-hover:border-white pointer-events-none" />
      {iconUrl ? (
        <img src={iconUrl} alt={name} className="w-8 h-8 image-rendering-pixelated" />
      ) : (
        <span className="text-[8px] leading-none text-[#2b2b2b] break-all px-0.5">
          {id.split("_")[0]}
        </span>
      )}
    </button>
  );
}

