import React, { useState, useEffect } from "react";
import { useExporter } from "../exporters/useExporter";
import { useImporter } from "../importers/useImporter";
import { useDesignerStore } from "../core/store";
import { toast } from "../core/toast";
import { confirmDialog } from "../core/confirm";

export function TopBar() {
  const { exportYaml } = useExporter();
  const { importYaml } = useImporter();
  const { undo, redo, undoStack, redoStack, menuName, setMenuName, loadState } = useDesignerStore();
  
  const [showProjects, setShowProjects] = useState(false);
  const [showMobileMenu, setShowMobileMenu] = useState(false);
  const [projects, setProjects] = useState<string[]>([]);
  const dropdownRef = React.useRef<HTMLDivElement>(null);
  const mobileMenuRef = React.useRef<HTMLDivElement>(null);
  const projectsMenuId = React.useId();

  useEffect(() => {
    updateProjectList();
    const handleSave = () => saveProject();
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setShowProjects(false);
      }
      if (mobileMenuRef.current && !mobileMenuRef.current.contains(event.target as Node)) {
        setShowMobileMenu(false);
      }
    };

    window.addEventListener("save-project", handleSave);
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      window.removeEventListener("save-project", handleSave);
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [menuName]);

  const updateProjectList = () => {
    const list: string[] = [];
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (key?.startsWith("project_")) {
        list.push(key.replace("project_", ""));
      }
    }
    setProjects(list);
  };

  const saveProject = () => {
    const name = String(menuName ?? "").trim();
    if (!name) {
      toast.error("Project name is required.");
      return;
    }
    const state = useDesignerStore.getState();
    const data = JSON.stringify(state);
    localStorage.setItem(`project_${name}`, data);
    updateProjectList();
    toast.success(`Project '${name}' saved.`);
  };

  const loadProject = (name: string) => {
    const data = localStorage.getItem(`project_${name}`);
    if (data) {
      try {
        const state = JSON.parse(data);
        loadState(state);
        setShowProjects(false);
        toast.success(`Project '${name}' loaded.`);
      } catch (e) {
        console.error("Failed to load project", e);
        toast.error(`Failed to load project '${name}'.`);
      }
    }
  };

  const deleteProject = async (name: string, e: React.MouseEvent) => {
    e.stopPropagation();
    const ok = await confirmDialog({
      title: "Delete project",
      message: `Delete project '${name}'?`,
      confirmText: "Delete",
      cancelText: "Cancel"
    });
    if (!ok) return;
    localStorage.removeItem(`project_${name}`);
    updateProjectList();
    toast.info(`Project '${name}' deleted.`);
  };

  return (
    <div className="relative min-h-14 flex flex-wrap items-center justify-between gap-2 px-4 py-2 pt-[calc(0.5rem+env(safe-area-inset-top))] bg-brand-surface border-b border-brand-border z-10 shrink-0">
      <div className="flex flex-wrap md:flex-nowrap items-center gap-x-2 gap-y-2 min-w-0 flex-1">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 bg-brand-accent border border-brand-border rounded flex items-center justify-center text-white font-semibold text-base select-none">
              B
          </div>
          <div className="font-minecraft font-bold text-xl text-white hidden md:block">
              BEDROCK<span className="text-brand-accent">GUI</span>
          </div>
        </div>
        
        <div className="h-8 w-px bg-brand-border mx-2 hidden sm:block"></div>
        
        <div className="flex items-center gap-2 shrink-0">
          <button 
            className="ui-btn ui-btn-secondary px-2 sm:px-3 py-1 text-xs"
            onClick={undo}
            disabled={!undoStack.length}
            title="Undo (Ctrl+Z)"
          >
            ↶ <span className="hidden sm:inline">Undo</span>
          </button>
          <button 
            className="ui-btn ui-btn-secondary px-2 sm:px-3 py-1 text-xs"
            onClick={redo}
            disabled={!redoStack.length}
            title="Redo (Ctrl+Y)"
          >
            ↷ <span className="hidden sm:inline">Redo</span>
          </button>
        </div>

        <div className="h-8 w-px bg-brand-border mx-2 hidden sm:block"></div>

      {/* Project Management - List Based */}
      <div className="relative shrink-0" ref={dropdownRef}>
        <button
          type="button"
          className={`ui-btn ui-btn-secondary min-h-10 px-3 py-2 cursor-pointer max-w-[65vw] sm:max-w-none ${showProjects ? "border-brand-accent" : ""}`}
          onClick={() => setShowProjects(!showProjects)}
          aria-haspopup="menu"
          aria-expanded={showProjects}
          aria-controls={projectsMenuId}
        >
          <div className="text-sm font-medium text-white truncate max-w-[45vw] sm:max-w-none">{menuName}</div>
          <div className="text-xs text-brand-muted">▼</div>
        </button>

        {showProjects && (
          <div id={projectsMenuId} role="menu" className="absolute top-full mt-2 w-72 max-w-[calc(100vw-2rem)] right-0 sm:left-0 sm:right-auto bg-brand-surface border border-brand-border rounded-lg shadow-lg z-50 flex flex-col max-h-[80vh]">
            <div className="p-3 border-b border-brand-border bg-brand-surface2">
               <div className="text-xs text-brand-muted font-medium mb-2">Current Project</div>
               <input 
                 className="ui-input"
                 value={menuName}
                 onChange={(e) => setMenuName(e.target.value)}
                 placeholder="Project Name"
                 onClick={(e) => e.stopPropagation()}
               />
            </div>
            
            <div className="flex-1 overflow-y-auto custom-scrollbar p-1">
              <div className="text-xs text-brand-muted px-2 py-1 font-medium sticky top-0 bg-brand-surface">Saved Projects</div>
              {projects.length === 0 && <div className="text-xs text-brand-muted px-4 py-4 italic text-center">No saved projects found</div>}
              {projects.map(p => (
                <div 
                  key={p} 
                  className={`flex items-center justify-between px-3 py-2 rounded cursor-pointer group transition-colors mb-0.5 ${p === menuName ? "bg-brand-accent/20 border border-brand-accent/50" : "hover:bg-brand-surface-raised/30 border border-transparent"}`}
                  onClick={(e) => {
                    if (p !== menuName) loadProject(p);
                  }}
                  onKeyDown={(e) => {
                    if (e.key !== "Enter" && e.key !== " ") return;
                    e.preventDefault();
                    if (p !== menuName) loadProject(p);
                  }}
                  role="menuitem"
                  tabIndex={0}
                >
                  <div className="flex items-center gap-2 overflow-hidden">
                    <span className={`w-2 h-2 rounded-sm ${p === menuName ? "bg-brand-accent" : "bg-transparent border border-brand-border"}`}></span>
                    <span className={`text-sm truncate ${p === menuName ? "text-white font-medium" : "text-brand-muted"}`}>{p}</span>
                  </div>
                  <button 
                    className="text-brand-muted hover:text-red-300 transition-colors px-1.5 py-0.5 rounded hover:bg-brand-surface2"
                    onClick={(e) => deleteProject(p, e)}
                    title="Delete Project"
                    aria-label={`Delete ${p}`}
                  >
                    ✕
                  </button>
                </div>
              ))}
            </div>

            <div className="p-3 border-t border-brand-border bg-brand-surface2 flex gap-2 shrink-0">
              <button 
                className="flex-1 ui-btn ui-btn-primary text-xs py-2 font-medium"
                onClick={saveProject}
                type="button"
              >
                💾 Save
              </button>
              <button 
                className="flex-1 ui-btn ui-btn-secondary text-xs py-2 font-medium"
                onClick={() => {
                   setMenuName("New Project");
                }}
                type="button"
              >
                + New
              </button>
            </div>
          </div>
        )}
      </div>
        {/* External Links - Desktop */}
        <div className="hidden xl:flex items-center gap-2 mx-2">
          <a
            href="https://pintux.gitbook.io/pintux-support/bedrockgui/bedrockgui-v2"
            target="_blank"
            rel="noreferrer"
            className="ui-btn ui-btn-secondary px-3 py-1.5 text-brand-muted hover:text-white relative group"
            title="Documentation"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
              <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
            </svg>
            <span className="text-xs font-medium hidden xl:inline">Docs</span>
          </a>
          
          <div className="h-6 w-px bg-brand-border mx-1"></div>

          <a
            href="https://modrinth.com/plugin/bedrockgui"
            target="_blank"
            rel="noreferrer"
            className="ui-btn ui-btn-secondary px-3 py-1.5 text-[#78d896] hover:text-[#abefbf] relative group"
            title="Modrinth"
          >
             <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
               <path d="M12.002 0a12 12 0 1 0 0 24 12 12 0 0 0 0-24Zm0 21.6a9.6 9.6 0 1 1 0-19.2 9.6 9.6 0 0 1 0 19.2Zm0-15.6a6 6 0 1 0 0 12 6 6 0 0 0 0-12Z" />
             </svg>
             <span className="text-xs font-medium hidden xl:inline">Modrinth</span>
          </a>

          <a
            href="https://github.com/pintux98/BedrockGUI"
            target="_blank"
            rel="noreferrer"
            className="ui-btn ui-btn-secondary px-3 py-1.5 text-white hover:text-gray-300 relative group"
            title="GitHub"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z" />
            </svg>
            <span className="text-xs font-medium hidden xl:inline">GitHub</span>
          </a>

          <a
            href="https://ko-fi.com/pintux"
            target="_blank"
            rel="noreferrer"
            className="ui-btn ui-btn-secondary px-3 py-1.5 text-[#ff8b88] hover:text-[#ffb2b0] relative group"
            title="Ko-Fi"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
              <path d="M23.881 8.948c-.773-4.085-4.859-4.593-4.859-4.593H.723c-.604 0-.679.798-.679.798s-.082 7.324-.022 11.822c.164 2.424 2.586 2.672 2.586 2.672s8.267-.023 11.966-.049c2.438-.426 2.683-2.566 2.658-3.734 4.352.24 7.422-2.831 6.649-6.916zm-11.062 3.511c-1.246 1.453-4.011 3.976-4.011 3.976s-.121.119-.31.023c-.076-.057-.108-.09-.108-.09-.443-.441-3.368-3.049-4.034-3.954-.709-.965-1.041-2.7-.091-3.71.951-1.01 3.005-1.086 4.363.407 0 0 1.565-1.782 3.468-.963 1.904.82 1.832 3.011.723 4.311zm6.173.478c-.928.116-1.682.028-1.682.028V7.284h1.77s1.971.551 1.971 2.638c0 1.913-.985 2.667-2.059 3.015z"/>
            </svg>
            <span className="text-xs font-medium hidden xl:inline">Ko-Fi</span>
          </a>
        </div>
      </div>

      <div className="flex gap-2 shrink-0">
        <button 
          className="xl:hidden ui-btn ui-btn-ghost min-h-11 min-w-11 text-brand-muted hover:text-white"
          onClick={() => setShowMobileMenu(!showMobileMenu)}
          type="button"
          aria-label="Open menu"
          aria-expanded={showMobileMenu}
          aria-controls="mobile-links-menu"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="3" y1="12" x2="21" y2="12"></line>
            <line x1="3" y1="6" x2="21" y2="6"></line>
            <line x1="3" y1="18" x2="21" y2="18"></line>
          </svg>
        </button>

        <button
          className="ui-btn ui-btn-primary px-4 py-2 text-sm hidden sm:block"
          onClick={() => exportYaml()}
        >
          Export
        </button>
        <label className="ui-btn ui-btn-secondary px-4 py-2 text-sm cursor-pointer hidden sm:block">
          Import
          <input
            type="file"
            accept=".yml,.yaml"
            className="hidden"
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) importYaml(file);
            }}
          />
        </label>
      </div>

      {/* Mobile Menu Dropdown */}
      {showMobileMenu && (
        <div id="mobile-links-menu" ref={mobileMenuRef} className="absolute top-full right-0 mt-2 w-64 max-w-[calc(100vw-2rem)] bg-brand-surface border border-brand-border rounded-lg shadow-lg z-50 p-4 flex flex-col gap-4 xl:hidden">
           <div className="flex gap-2 sm:hidden">
              <button
                className="flex-1 ui-btn ui-btn-primary px-4 py-2 text-sm"
                onClick={() => { exportYaml(); setShowMobileMenu(false); }}
                type="button"
              >
                Export
              </button>
              <label className="flex-1 ui-btn ui-btn-secondary px-4 py-2 text-sm cursor-pointer text-center">
                Import
                <input
                  type="file"
                  accept=".yml,.yaml"
                  className="hidden"
                  onChange={(e) => {
                    const file = e.target.files?.[0];
                    if (file) {
                      importYaml(file);
                      setShowMobileMenu(false);
                    }
                  }}
                />
              </label>
           </div>
           
           <div className="h-px bg-brand-border w-full sm:hidden"></div>

           <div className="flex flex-col gap-2">
              <div className="text-xs text-brand-muted font-medium px-2">Links</div>
              <a href="https://pintux.gitbook.io/pintux-support/bedrockgui/bedrockgui-v2" target="_blank" rel="noreferrer" className="flex items-center gap-3 px-2 py-2 text-brand-text hover:bg-brand-surface-raised/20 rounded">
                <span>📚</span> Documentation
              </a>
              <a href="https://github.com/pintux98/BedrockGUI" target="_blank" rel="noreferrer" className="flex items-center gap-3 px-2 py-2 text-brand-text hover:bg-brand-surface-raised/20 rounded">
                <span>💻</span> GitHub
              </a>
              <a href="https://www.spigotmc.org/resources/bedrockgui-spigot-and-bungeecord-support.119592/" target="_blank" rel="noreferrer" className="flex items-center gap-3 px-2 py-2 text-[#ffc078] hover:bg-brand-surface-raised/20 rounded">
                <span>🟧</span> SpigotMC
              </a>
              <a href="https://modrinth.com/plugin/bedrockgui" target="_blank" rel="noreferrer" className="flex items-center gap-3 px-2 py-2 text-[#78d896] hover:bg-brand-surface-raised/20 rounded">
                <span>🟩</span> Modrinth
              </a>
              <a href="https://ko-fi.com/pintux" target="_blank" rel="noreferrer" className="flex items-center gap-3 px-2 py-2 text-[#ff9f9d] hover:bg-brand-surface-raised/20 rounded">
                <span>☕</span> Ko-Fi
              </a>
           </div>
        </div>
      )}
    </div>
  );
}
