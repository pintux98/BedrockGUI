import React, { useState } from "react";
import { useDesignerStore } from "../core/store";

export function Wizard() {
  const { setIsWizardOpen, setPlatform, setBedrock, setJava, setMenuName } = useDesignerStore();
  const [step, setStep] = useState(1);
  
  // Form State
  const [platform, setLocalPlatform] = useState<"bedrock" | "java">("bedrock");
  const [bedrockType, setBedrockType] = useState<"SIMPLE" | "MODAL" | "CUSTOM">("SIMPLE");
  const [javaType, setJavaType] = useState<"CHEST" | "ANVIL" | "CRAFTING">("CHEST");
  const [name, setName] = useState("new_form");
  const [title, setTitle] = useState("New Form");

  const handleFinish = () => {
    setPlatform(platform);
    setMenuName(name);
    
    if (platform === "bedrock") {
      setBedrock({
        type: bedrockType as any,
        title: title,
        content: "Content goes here...",
        buttons: bedrockType === "MODAL" 
          ? [{ id: "yes", text: "Yes" }, { id: "no", text: "No" }]
          : [{ id: "btn_1", text: "Button 1" }],
        components: []
      } as any);
    } else {
      setJava({
        type: javaType as any,
        title: title,
        size: 27,
        items: []
      });
    }
    setIsWizardOpen(false);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-[#343434] border-4 border-[#1e1e1e] shadow-2xl w-[500px] text-white font-minecraft p-1 relative">
        <div className="bg-[#2b2b2b] border-b-4 border-[#1e1e1e] p-4 flex justify-between items-center mb-1">
          <h2 className="text-xl font-bold tracking-wide text-white drop-shadow-md">Create New Form</h2>
          <button 
            onClick={() => setIsWizardOpen(false)}
            className="text-gray-400 hover:text-white"
          >
            ✕
          </button>
        </div>

        <div className="p-6 bg-[#343434]">
          {/* Progress Steps */}
          <div className="flex items-center justify-between mb-8 relative">
            <div className="absolute top-1/2 left-0 w-full h-1 bg-[#5b5b5b] -z-10" />
            {[1, 2, 3].map((s) => (
              <div 
                key={s}
                className={`w-8 h-8 flex items-center justify-center border-2 ${
                  step >= s 
                    ? "bg-brand-accent border-white text-white" 
                    : "bg-[#5b5b5b] border-[#8b8b8b] text-gray-400"
                }`}
              >
                {s}
              </div>
            ))}
          </div>

          {/* Step 1: Platform & Type */}
          {step === 1 && (
            <div className="space-y-6 animate-in slide-in-from-right-4 duration-300">
              <div className="space-y-2">
                <label className="block text-brand-muted text-sm uppercase tracking-wide">Select Platform</label>
                <div className="grid grid-cols-2 gap-4">
                  <button
                    onClick={() => setLocalPlatform("bedrock")}
                    className={`p-4 border-2 text-center transition-all ${
                      platform === "bedrock" 
                        ? "bg-brand-accent/20 border-brand-accent text-white" 
                        : "bg-[#2b2b2b] border-[#5b5b5b] text-gray-400 hover:bg-[#3f3f3f]"
                    }`}
                  >
                    <div className="font-bold text-lg mb-1">Bedrock</div>
                    <div className="text-xs opacity-70">Forms & Dialogs</div>
                  </button>
                  <button
                    onClick={() => setLocalPlatform("java")}
                    className={`p-4 border-2 text-center transition-all ${
                      platform === "java" 
                        ? "bg-brand-accent/20 border-brand-accent text-white" 
                        : "bg-[#2b2b2b] border-[#5b5b5b] text-gray-400 hover:bg-[#3f3f3f]"
                    }`}
                  >
                    <div className="font-bold text-lg mb-1">Java</div>
                    <div className="text-xs opacity-70">Inventories & Menus</div>
                  </button>
                </div>
              </div>

              <div className="space-y-2">
                <label className="block text-brand-muted text-sm uppercase tracking-wide">Select Type</label>
                <select
                  className="w-full bg-[#1e1e1e] border-2 border-[#5b5b5b] p-3 text-white outline-none focus:border-brand-accent"
                  value={platform === "bedrock" ? bedrockType : javaType}
                  onChange={(e) => platform === "bedrock" ? setBedrockType(e.target.value as any) : setJavaType(e.target.value as any)}
                >
                  {platform === "bedrock" ? (
                    <>
                      <option value="SIMPLE">Simple Form (Buttons)</option>
                      <option value="MODAL">Modal Form (Yes/No)</option>
                      <option value="CUSTOM">Custom Form (Inputs)</option>
                    </>
                  ) : (
                    <>
                      <option value="CHEST">Chest Menu</option>
                      <option value="ANVIL">Anvil Menu</option>
                      <option value="CRAFTING">Crafting Table</option>
                    </>
                  )}
                </select>
              </div>
            </div>
          )}

          {/* Step 2: Details */}
          {step === 2 && (
            <div className="space-y-6 animate-in slide-in-from-right-4 duration-300">
              <div className="space-y-2">
                <label className="block text-brand-muted text-sm uppercase tracking-wide">Menu Name (ID)</label>
                <input
                  type="text"
                  className="w-full bg-[#1e1e1e] border-2 border-[#5b5b5b] p-3 text-white outline-none focus:border-brand-accent"
                  placeholder="e.g. warp_menu"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                />
                <p className="text-xs text-gray-500">Unique identifier used in commands.</p>
              </div>

              <div className="space-y-2">
                <label className="block text-brand-muted text-sm uppercase tracking-wide">Display Title</label>
                <input
                  type="text"
                  className="w-full bg-[#1e1e1e] border-2 border-[#5b5b5b] p-3 text-white outline-none focus:border-brand-accent"
                  placeholder="e.g. Warp Selector"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                />
              </div>
            </div>
          )}

          {/* Step 3: Review */}
          {step === 3 && (
            <div className="space-y-6 animate-in slide-in-from-right-4 duration-300 text-center">
              <div className="py-8">
                <div className="text-6xl mb-4">✨</div>
                <h3 className="text-xl font-bold text-white mb-2">Ready to Create!</h3>
                <p className="text-gray-400 max-w-xs mx-auto">
                  You are about to create a <span className="text-brand-accent font-bold">{platform === "bedrock" ? bedrockType : javaType}</span> form named <span className="text-white font-mono bg-[#1e1e1e] px-1">{name}</span>.
                </p>
              </div>
            </div>
          )}
        </div>

        {/* Footer Actions */}
        <div className="bg-[#2b2b2b] border-t-4 border-[#1e1e1e] p-4 flex justify-between">
          <button
            onClick={() => step > 1 ? setStep(step - 1) : setIsWizardOpen(false)}
            className="ui-btn ui-btn-secondary px-6"
          >
            {step === 1 ? "Cancel" : "Back"}
          </button>
          
          <button
            onClick={() => step < 3 ? setStep(step + 1) : handleFinish()}
            className="ui-btn ui-btn-primary px-8"
          >
            {step === 3 ? "Create Form" : "Next"}
          </button>
        </div>
      </div>
    </div>
  );
}
