import React, { useEffect, useState } from "react";
import { Palette } from "../panels/Palette";
import { PropertiesPanel } from "../panels/PropertiesPanel";
import { FormTypePanel } from "../panels/FormTypePanel";
import { JavaPalette } from "../panels/JavaPalette";
import { Canvas } from "../canvas/Canvas";
import { ErrorBoundary } from "./ErrorBoundary";
import { TopBar } from "./TopBar";
import { YamlEditorPanel } from "../panels/YamlEditorPanel";
import { useDesignerStore } from "../core/store";
import { DndHost } from "./DndHost";
import { ValidationPanel } from "../panels/ValidationPanel";
import { Wizard } from "../components/Wizard";
import { HistoryPanel } from "../panels/HistoryPanel";
import { ResizablePanel } from "../components/ResizablePanel";

export function DesignerShell() {
  const { platform, isWizardOpen } = useDesignerStore();
  const [viewport, setViewport] = useState<"mobile" | "tablet" | "desktop">("desktop");
  const [yamlCollapsed, setYamlCollapsed] = useState(false);
  const [historyCollapsed, setHistoryCollapsed] = useState(false);
  const [mobileTab, setMobileTab] = useState<"tools" | "canvas" | "properties">("canvas");
  const isMobile = viewport === "mobile";
  const isTablet = viewport === "tablet";
  const isDesktop = viewport === "desktop";

  useEffect(() => {
    const check = () => {
      if (window.innerWidth < 768) {
        setViewport("mobile");
        return;
      }
      if (window.innerWidth < 1280) {
        setViewport("tablet");
        return;
      }
      setViewport("desktop");
    };
    check();
    window.addEventListener("resize", check);
    return () => window.removeEventListener("resize", check);
  }, []);

  return (
    <div className="h-[100dvh] w-full flex flex-col bg-brand-bg text-brand-text relative">
      <TopBar />
      {isWizardOpen && <Wizard />}
      <div className="flex-1 overflow-hidden relative flex flex-col min-h-0">
        <DndHost>
          <ErrorBoundary>
            <div className={`flex flex-1 overflow-hidden min-h-0 ${isMobile ? "flex-col" : "flex-row"}`}>
            {!isMobile ? (
              <ResizablePanel 
                initialSize={isTablet ? 320 : 384} 
                minSize={240} 
                maxSize={500} 
                side="left" 
                persistenceKey="left_panel_width"
                className="border-r border-brand-border h-full"
              >
                <div className="flex-1 flex flex-col h-full overflow-hidden">
                  <div className="flex-1 overflow-y-auto overflow-x-hidden custom-scrollbar min-h-0">
                    <FormTypePanel />
                    {platform === "bedrock" ? <Palette /> : <JavaPalette />}
                  </div>
                  <ResizablePanel
                    initialSize={200}
                    minSize={32}
                    maxSize={500}
                    side="bottom"
                    persistenceKey="history_panel_height"
                    className={`shrink-0 border-t border-brand-border transition-all duration-300 ${historyCollapsed ? "!h-8 !min-h-[32px]" : ""}`}
                    forceCollapse={historyCollapsed}
                  >
                    <HistoryPanel onCollapseChange={setHistoryCollapsed} />
                  </ResizablePanel>
                </div>
              </ResizablePanel>
            ) : (
              <div className={`w-full flex-1 flex flex-col overflow-hidden ${mobileTab === "tools" ? "flex" : "hidden"}`}>
                <div className="flex-1 overflow-y-auto overflow-x-hidden custom-scrollbar">
                  <FormTypePanel />
                  {platform === "bedrock" ? <Palette /> : <JavaPalette />}
                  <HistoryPanel />
                </div>
              </div>
            )}

            <div className={`flex-1 flex flex-col min-w-0 min-h-0 relative bg-brand-surface2/40 ${isMobile && mobileTab !== "canvas" ? "hidden" : "flex"}`}>
              <div className="flex-1 overflow-hidden relative">
                <Canvas />
              </div>
              <div className="border-t border-brand-border shrink-0">
                <ValidationPanel />
              </div>
            </div>

            {isDesktop ? (
              <ResizablePanel 
                initialSize={420} 
                minSize={300} 
                maxSize={600} 
                side="right" 
                persistenceKey="right_panel_width"
                className="border-l border-brand-border h-full"
              >
                <div className="flex-1 flex flex-col h-full overflow-hidden">
                  <div className="flex-1 overflow-y-auto overflow-x-hidden custom-scrollbar min-h-0">
                    <PropertiesPanel />
                  </div>
                  <ResizablePanel
                    initialSize={250}
                    minSize={32}
                    maxSize={600}
                    side="bottom"
                    persistenceKey="yaml_panel_height"
                    className={`shrink-0 transition-all duration-300 ${yamlCollapsed ? "!h-8 !min-h-[32px]" : ""}`}
                    forceCollapse={yamlCollapsed}
                  >
                    <YamlEditorPanel onCollapseChange={setYamlCollapsed} />
                  </ResizablePanel>
                </div>
              </ResizablePanel>
            ) : isTablet ? (
              <div className="w-[360px] max-w-[42vw] min-w-[300px] border-l border-brand-border h-full flex flex-col bg-brand-surface">
                <div className="flex-1 overflow-y-auto overflow-x-hidden custom-scrollbar min-h-0">
                  <PropertiesPanel />
                </div>
                <div className="border-t border-brand-border shrink-0 h-[min(320px,35dvh)] overflow-hidden">
                  <YamlEditorPanel defaultExpanded={true} />
                </div>
              </div>
            ) : (
              <div className={`w-full flex-1 flex flex-col overflow-hidden min-h-0 ${mobileTab === "properties" ? "flex" : "hidden"}`}>
                <div className="flex-1 flex flex-col overflow-hidden min-h-0">
                  <div className="flex-1 overflow-y-auto overflow-x-hidden custom-scrollbar min-h-0">
                    <PropertiesPanel />
                  </div>
                  <div className="border-t border-brand-border shrink-0 h-[min(300px,35dvh)] overflow-hidden">
                    <YamlEditorPanel defaultExpanded={true} />
                  </div>
                </div>
              </div>
            )}
          </div>

          {isMobile && (
            <div className="min-h-14 bg-brand-surface border-t border-brand-border shrink-0 flex items-center justify-around px-2 pb-[env(safe-area-inset-bottom)] z-50">
              <button 
                onClick={() => setMobileTab("tools")}
                className={`flex flex-col items-center justify-center min-h-11 w-24 py-1 rounded transition-colors ${mobileTab === "tools" ? "text-brand-accent bg-brand-surface-raised/40" : "text-brand-muted"}`}
                aria-label="Open tools tab"
                aria-pressed={mobileTab === "tools"}
              >
                <span className="text-lg">🛠️</span>
                <span className="text-[11px] font-medium mt-0.5">Tools</span>
              </button>
              <button 
                onClick={() => setMobileTab("canvas")}
                className={`flex flex-col items-center justify-center min-h-11 w-24 py-1 rounded transition-colors ${mobileTab === "canvas" ? "text-brand-accent bg-brand-surface-raised/40" : "text-brand-muted"}`}
                aria-label="Open canvas tab"
                aria-pressed={mobileTab === "canvas"}
              >
                <span className="text-lg">🎨</span>
                <span className="text-[11px] font-medium mt-0.5">Canvas</span>
              </button>
              <button 
                onClick={() => setMobileTab("properties")}
                className={`flex flex-col items-center justify-center min-h-11 w-24 py-1 rounded transition-colors ${mobileTab === "properties" ? "text-brand-accent bg-brand-surface-raised/40" : "text-brand-muted"}`}
                aria-label="Open properties tab"
                aria-pressed={mobileTab === "properties"}
              >
                <span className="text-lg">⚙️</span>
                <span className="text-[11px] font-medium mt-0.5">Props</span>
              </button>
            </div>
          )}
          </ErrorBoundary>
        </DndHost>
      </div>
    </div>
  );
}
