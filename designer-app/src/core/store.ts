import { create } from "zustand";
import { DesignerState, BedrockForm, JavaMenu, ActionInstance } from "./types";

const initialState: DesignerState = {
  configVersion: "1.0.0",
  menuName: "example",
  platform: "bedrock",
  bedrock: {
    type: "SIMPLE",
    title: "Example Form",
    content: "Content",
    buttons: [{ id: "button_1", text: "Click me" }]
  }
};

type Actions = {
  setMenuName: (name: string) => void;
  setPlatform: (p: DesignerState["platform"]) => void;
  setBedrock: (form: BedrockForm | undefined, description?: string) => void;
  setJava: (menu: JavaMenu | undefined, description?: string) => void;
  setGlobalActions: (a: ActionInstance[] | undefined, description?: string) => void;
  setDirty: (dirty: boolean) => void;
  setSelectedJavaSlot: (slot: number | null) => void;
  setSelectedBedrockButtonId: (id: string | null) => void;
  setSelectedBedrockComponentId: (id: string | null) => void;
  undo: () => void;
  redo: () => void;
  setIsWizardOpen: (open: boolean) => void;
  jumpToHistory: (index: number) => void;
  loadState: (state: DesignerState) => void;
};

interface HistoryEntry {
  state: DesignerState;
  description: string;
  timestamp: number;
}

// Helper to extract only the persistent state
const getSnapshot = (state: any): DesignerState => ({
  configVersion: state.configVersion,
  menuName: state.menuName,
  platform: state.platform,
  bedrock: state.bedrock,
  java: state.java,
  globalActions: state.globalActions
});

export const useDesignerStore = create<
  DesignerState &
    Actions & {
      dirty: boolean;
      selectedJavaSlot: number | null;
      selectedBedrockButtonId: string | null;
      selectedBedrockComponentId: string | null;
      undoStack: HistoryEntry[];
      redoStack: HistoryEntry[];
      isWizardOpen: boolean;
    }
>()((set, get) => ({
  ...initialState,
  dirty: false,
  selectedJavaSlot: null,
  selectedBedrockButtonId: null,
  selectedBedrockComponentId: null,
  undoStack: [],
  redoStack: [],
  isWizardOpen: false,

  setMenuName: (menuName) => {
    const snap = getSnapshot(get());
    set((s) => ({
      menuName,
      dirty: true,
      undoStack: [...s.undoStack, { state: snap, description: "Changed menu name", timestamp: Date.now() }],
      redoStack: []
    }));
  },

  setPlatform: (platform) =>
    set((s) => {
      const snap = getSnapshot(s);
      const next: any = { 
        platform,
        undoStack: [...s.undoStack, { state: snap, description: `Switched to ${platform}`, timestamp: Date.now() }],
        redoStack: []
      };
      
      if (platform === "bedrock" && !s.bedrock) {
        next.bedrock = {
          type: "SIMPLE",
          title: "Example Form",
          content: "Content",
          buttons: [{ id: "button_1", text: "Button 1" }]
        };
      }
      if (platform === "java" && !s.java) {
        next.java = {
          type: "CHEST",
          title: "Example Menu",
          size: 27,
          items: []
        };
      }
      return next;
    }),

  setBedrock: (bedrock, description = "Updated Bedrock form") => {
    const snap = getSnapshot(get());
    set((s) => ({
      bedrock,
      dirty: true,
      undoStack: [...s.undoStack, { state: snap, description, timestamp: Date.now() }],
      redoStack: []
    }));
  },

  setJava: (java, description = "Updated Java menu") => {
    const snap = getSnapshot(get());
    set((s) => ({
      java,
      dirty: true,
      undoStack: [...s.undoStack, { state: snap, description, timestamp: Date.now() }],
      redoStack: []
    }));
  },

  setGlobalActions: (globalActions, description = "Updated global actions") => {
    const snap = getSnapshot(get());
    set((s) => ({
      globalActions,
      dirty: true,
      undoStack: [...s.undoStack, { state: snap, description, timestamp: Date.now() }],
      redoStack: []
    }));
  },

  setDirty: (dirty) => set({ dirty }),
  setSelectedJavaSlot: (selectedJavaSlot) => set({ selectedJavaSlot }),
  setSelectedBedrockButtonId: (selectedBedrockButtonId) => set({ selectedBedrockButtonId }),
  setSelectedBedrockComponentId: (selectedBedrockComponentId) => set({ selectedBedrockComponentId }),

  undo: () => {
    const { undoStack, redoStack } = get();
    if (undoStack.length === 0) return;
    const prev = undoStack[undoStack.length - 1];
    const current = getSnapshot(get());
    set({
      ...prev.state,
      undoStack: undoStack.slice(0, -1),
      redoStack: [...redoStack, { state: current, description: prev.description, timestamp: Date.now() }],
      // Clear selection on undo to avoid invalid refs
      selectedJavaSlot: null,
      selectedBedrockButtonId: null,
      selectedBedrockComponentId: null
    });
  },

  redo: () => {
    const { undoStack, redoStack } = get();
    if (redoStack.length === 0) return;
    const next = redoStack[redoStack.length - 1];
    const current = getSnapshot(get());
    set({
      ...next.state,
      undoStack: [...undoStack, { state: current, description: next.description, timestamp: Date.now() }],
      redoStack: redoStack.slice(0, -1),
      // Clear selection on redo
      selectedJavaSlot: null,
      selectedBedrockButtonId: null,
      selectedBedrockComponentId: null
    });
  },

  setIsWizardOpen: (isWizardOpen: boolean) => set({ isWizardOpen }),
  
  jumpToHistory: (index: number) => {
    const { undoStack, redoStack } = get();
    const current = getSnapshot(get());
    
    // If index is within undoStack
    if (index < undoStack.length) {
       const target = undoStack[index];
       const newUndo = undoStack.slice(0, index);
       // Items between index and end of undoStack + current + redoStack need to go to redoStack
       const toRedo = [...undoStack.slice(index + 1), { state: current, description: "Reverted state", timestamp: Date.now() }, ...redoStack.slice().reverse()];
       
       set({
         ...target.state,
         undoStack: newUndo,
         redoStack: toRedo.reverse().map(x => ({...x, timestamp: Date.now()})) as HistoryEntry[], 
         selectedJavaSlot: null,
         selectedBedrockButtonId: null,
         selectedBedrockComponentId: null
       });
    }
  }
}));
