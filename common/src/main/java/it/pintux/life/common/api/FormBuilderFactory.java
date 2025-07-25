package it.pintux.life.common.api;

import it.pintux.life.common.api.BedrockGUIApi.*;
import it.pintux.life.common.utils.FormPlayer;
import org.geysermc.cumulus.form.Form;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Factory class for creating specialized form builders and templates
 */
public class FormBuilderFactory {
    
    private final BedrockGUIApi api;
    
    public FormBuilderFactory(BedrockGUIApi api) {
        this.api = api;
    }
    
    /**
     * Creates a confirmation dialog form
     */
    public BedrockGUIApi.ModalFormBuilder createConfirmationDialog(String title, String message, 
                                                                   Runnable onConfirm, Runnable onCancel) {
        BedrockGUIApi.ModalFormBuilder builder = api.createModalForm(title);
        builder.content(message);
        builder.button1("Confirm", player -> {
            if (onConfirm != null) onConfirm.run();
        });
        builder.button2("Cancel", player -> {
            if (onCancel != null) onCancel.run();
        });
        return builder;
    }
    
    /**
     * Creates a paginated list form
     */
    public PaginatedFormBuilder createPaginatedList(String title, List<String> items, int itemsPerPage) {
        return new PaginatedFormBuilder(api, title, items, itemsPerPage);
    }
    
    /**
     * Creates a paginated list form with item handler
     */
    public PaginatedFormBuilder createPaginatedList(String title, List<String> items, int itemsPerPage, 
                                                   BiConsumer<String, BedrockGUIApi.SimpleFormBuilder> itemHandler) {
        PaginatedFormBuilder builder = new PaginatedFormBuilder(api, title, items, itemsPerPage);
        builder.setItemHandler(itemHandler);
        return builder;
    }
    
    /**
     * Creates a settings form with multiple categories
     */
    public SettingsFormBuilder createSettingsForm(String title) {
        return new SettingsFormBuilder(api, title);
    }
    
    /**
     * Creates a wizard-style multi-step form
     */
    public WizardFormBuilder createWizard(String title) {
        return new WizardFormBuilder(api, title);
    }
    
    /**
     * Creates a wizard-style multi-step form with predefined steps
     */
    public WizardFormBuilder createWizard(String title, List<BedrockGUIApi.FormBuilder> steps) {
        WizardFormBuilder wizard = new WizardFormBuilder(api, title);
        for (int i = 0; i < steps.size(); i++) {
            final int stepIndex = i;
            wizard.addStep("Step " + (i + 1), data -> steps.get(stepIndex));
        }
        return wizard;
    }
    
    /**
     * Creates a dynamic inventory-style form
     */
    public InventoryFormBuilder createInventoryForm(String title, int rows, int columns) {
        return new InventoryFormBuilder(api, title, rows, columns);
    }
    
    // ==================== SPECIALIZED BUILDERS ====================
    
    /**
     * Paginated form builder for handling large lists
     */
    public static class PaginatedFormBuilder {
        private final BedrockGUIApi api;
        private final String title;
        private final List<String> items;
        private final int itemsPerPage;
        private Function<String, Runnable> itemClickHandler;
        private BiConsumer<String, BedrockGUIApi.SimpleFormBuilder> itemHandler;
        private int currentPage = 0;
        
        public PaginatedFormBuilder(BedrockGUIApi api, String title, List<String> items, int itemsPerPage) {
            this.api = api;
            this.title = title;
            this.items = new ArrayList<>(items);
            this.itemsPerPage = itemsPerPage;
            this.itemClickHandler = null;
            this.itemHandler = null;
        }
        
        public PaginatedFormBuilder onItemClick(Function<String, Runnable> handler) {
            this.itemClickHandler = handler;
            return this;
        }
        
        public PaginatedFormBuilder setItemHandler(BiConsumer<String, BedrockGUIApi.SimpleFormBuilder> handler) {
            this.itemHandler = handler;
            return this;
        }
        
        public BedrockGUIApi.SimpleFormBuilder buildPage(int page) {
            this.currentPage = page;
            int totalPages = (int) Math.ceil((double) items.size() / itemsPerPage);
            
            BedrockGUIApi.SimpleFormBuilder builder = api.createSimpleForm(title + " (Page " + (page + 1) + "/" + totalPages + ")");
            
            int startIndex = page * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, items.size());
            
            // Add items for current page
            for (int i = startIndex; i < endIndex; i++) {
                String item = items.get(i);
                if (itemHandler != null) {
                    // Use the item handler to customize the button
                    itemHandler.accept(item, builder);
                } else {
                    // Use the default click handler
                    builder.button(item, player -> {
                        if (itemClickHandler != null) {
                            Runnable action = itemClickHandler.apply(item);
                            if (action != null) action.run();
                        }
                    });
                }
            }
            
            // Add navigation buttons
            if (page > 0) {
                builder.button("◀ Previous Page", player -> {
                    // Open previous page
                    buildPage(page - 1).send(player);
                });
            }
            
            if (page < totalPages - 1) {
                builder.button("Next Page ▶", player -> {
                    // Open next page
                    buildPage(page + 1).send(player);
                });
            }
            
            return builder;
        }
        
        public org.geysermc.cumulus.form.Form build() {
            return buildPage(currentPage).build();
        }
    }
    
    /**
     * Settings form builder with categories and validation
     */
    public static class SettingsFormBuilder {
        private final BedrockGUIApi api;
        private final String title;
        private final Map<String, List<SettingComponent>> categories = new LinkedHashMap<>();
        
        public SettingsFormBuilder(BedrockGUIApi api, String title) {
            this.api = api;
            this.title = title;
        }
        
        public SettingsFormBuilder addCategory(String categoryName) {
            categories.put(categoryName, new ArrayList<>());
            return this;
        }
        
        public SettingsFormBuilder addToggleSetting(String category, String name, String description, boolean defaultValue) {
            categories.computeIfAbsent(category, k -> new ArrayList<>())
                .add(new ToggleSettingComponent(name, description, defaultValue));
            return this;
        }
        
        public SettingsFormBuilder addSliderSetting(String category, String name, String description, 
                                                   int min, int max, int step, int defaultValue) {
            categories.computeIfAbsent(category, k -> new ArrayList<>())
                .add(new SliderSettingComponent(name, description, min, max, step, defaultValue));
            return this;
        }
        
        public SettingsFormBuilder addDropdownSetting(String category, String name, String description, 
                                                     List<String> options, int defaultIndex) {
            categories.computeIfAbsent(category, k -> new ArrayList<>())
                .add(new DropdownSettingComponent(name, description, options, defaultIndex));
            return this;
        }
        
        public org.geysermc.cumulus.form.Form build() {
            BedrockGUIApi.CustomFormBuilder builder = api.createCustomForm(title);
            
            for (Map.Entry<String, List<SettingComponent>> entry : categories.entrySet()) {
                String categoryName = entry.getKey();
                List<SettingComponent> components = entry.getValue();
                
                // Add category header (as a disabled input)
                builder.input("=== " + categoryName + " ===", "", "");
                
                // Add category components
                for (SettingComponent component : components) {
                    component.addToForm(builder);
                }
            }
            
            return builder.build();
        }
        
        // Setting component interfaces
        private interface SettingComponent {
            void addToForm(BedrockGUIApi.CustomFormBuilder builder);
        }
        
        private static class ToggleSettingComponent implements SettingComponent {
            private final String name, description;
            private final boolean defaultValue;
            
            public ToggleSettingComponent(String name, String description, boolean defaultValue) {
                this.name = name;
                this.description = description;
                this.defaultValue = defaultValue;
            }
            
            @Override
            public void addToForm(BedrockGUIApi.CustomFormBuilder builder) {
                builder.toggle(name + (description != null ? " - " + description : ""), defaultValue);
            }
        }
        
        private static class SliderSettingComponent implements SettingComponent {
            private final String name, description;
            private final int min, max, step, defaultValue;
            
            public SliderSettingComponent(String name, String description, int min, int max, int step, int defaultValue) {
                this.name = name;
                this.description = description;
                this.min = min;
                this.max = max;
                this.step = step;
                this.defaultValue = defaultValue;
            }
            
            @Override
            public void addToForm(BedrockGUIApi.CustomFormBuilder builder) {
                builder.slider(name + (description != null ? " - " + description : ""), min, max, step, defaultValue);
            }
        }
        
        private static class DropdownSettingComponent implements SettingComponent {
            private final String name, description;
            private final List<String> options;
            private final int defaultIndex;
            
            public DropdownSettingComponent(String name, String description, List<String> options, int defaultIndex) {
                this.name = name;
                this.description = description;
                this.options = options;
                this.defaultIndex = defaultIndex;
            }
            
            @Override
            public void addToForm(BedrockGUIApi.CustomFormBuilder builder) {
                builder.dropdown(name + (description != null ? " - " + description : ""), options, defaultIndex);
            }
        }
    }
    
    /**
     * Wizard form builder for multi-step forms
     */
    public static class WizardFormBuilder {
        private final BedrockGUIApi api;
        private final String title;
        private final List<WizardStep> steps = new ArrayList<>();
        private final Map<String, Object> wizardData = new HashMap<>();
        private BiConsumer<FormPlayer, Map<String, Object>> onCompleteHandler;
        private java.util.function.Consumer<FormPlayer> onCancelHandler;
        
        public WizardFormBuilder(BedrockGUIApi api, String title) {
            this.api = api;
            this.title = title;
        }
        
        public WizardFormBuilder addStep(String stepTitle, Function<Map<String, Object>, BedrockGUIApi.FormBuilder> stepBuilder) {
            steps.add(new WizardStep(stepTitle, stepBuilder));
            return this;
        }
        
        public WizardFormBuilder onComplete(BiConsumer<FormPlayer, Map<String, Object>> handler) {
            this.onCompleteHandler = handler;
            return this;
        }
        
        public WizardFormBuilder onCancel(java.util.function.Consumer<FormPlayer> handler) {
            this.onCancelHandler = handler;
            return this;
        }
        
        public BedrockGUIApi.FormBuilder buildStep(int stepIndex, FormPlayer player) {
            if (stepIndex < 0 || stepIndex >= steps.size()) {
                throw new IllegalArgumentException("Invalid step index: " + stepIndex);
            }
            
            WizardStep step = steps.get(stepIndex);
            String stepTitle = title + " - Step " + (stepIndex + 1) + ": " + step.title;
            
            BedrockGUIApi.FormBuilder stepForm = step.builder.apply(wizardData);
            
            // Add navigation if it's a custom form
            if (stepForm instanceof BedrockGUIApi.CustomFormBuilder) {
                BedrockGUIApi.CustomFormBuilder customForm = (BedrockGUIApi.CustomFormBuilder) stepForm;
                
                customForm.onSubmit((p, results) -> {
                    // Store step results
                    wizardData.putAll(results);
                    
                    // Move to next step or finish
                    if (stepIndex < steps.size() - 1) {
                        // Continue to next step
                        buildStep(stepIndex + 1, p).send(p);
                    } else {
                        // Wizard complete
                        if (onCompleteHandler != null) {
                            onCompleteHandler.accept(p, wizardData);
                        }
                    }
                });
            }
            
            return stepForm;
        }
        
        public org.geysermc.cumulus.form.Form build() {
            if (steps.isEmpty()) {
                throw new IllegalStateException("Wizard must have at least one step");
            }
            return buildStep(0, null).build();
        }
        
        public CompletableFuture<FormResult> send(FormPlayer player) {
            if (steps.isEmpty()) {
                throw new IllegalStateException("Wizard must have at least one step");
            }
            return buildStep(0, player).send(player);
        }
        
        private static class WizardStep {
            final String title;
            final Function<Map<String, Object>, BedrockGUIApi.FormBuilder> builder;
            
            WizardStep(String title, Function<Map<String, Object>, BedrockGUIApi.FormBuilder> builder) {
                this.title = title;
                this.builder = builder;
            }
        }
    }
    
    /**
     * Inventory-style form builder with grid layout
     */
    public static class InventoryFormBuilder {
        private final BedrockGUIApi api;
        private final String title;
        private final int rows, columns;
        private final Map<Integer, InventorySlot> slots = new HashMap<>();
        
        public InventoryFormBuilder(BedrockGUIApi api, String title, int rows, int columns) {
            this.api = api;
            this.title = title;
            this.rows = rows;
            this.columns = columns;
        }
        
        public InventoryFormBuilder setSlot(int row, int column, String itemName, String itemImage, Runnable onClick) {
            int slotIndex = row * columns + column;
            slots.put(slotIndex, new InventorySlot(itemName, itemImage, onClick));
            return this;
        }
        
        public InventoryFormBuilder setSlot(int slotIndex, String itemName, String itemImage, Runnable onClick) {
            slots.put(slotIndex, new InventorySlot(itemName, itemImage, onClick));
            return this;
        }
        
        public org.geysermc.cumulus.form.Form build() {
            BedrockGUIApi.SimpleFormBuilder builder = api.createSimpleForm(title);
            
            int totalSlots = rows * columns;
            
            for (int i = 0; i < totalSlots; i++) {
                InventorySlot slot = slots.get(i);
                
                if (slot != null) {
                    builder.button(slot.name, slot.image, player -> {
                        if (slot.onClick != null) slot.onClick.run();
                    });
                } else {
                    // Empty slot
                    builder.button("[Empty]", player -> {
                        // Do nothing for empty slots
                    });
                }
            }
            
            return builder.build();
        }
        
        private static class InventorySlot {
            final String name;
            final String image;
            final Runnable onClick;
            
            InventorySlot(String name, String image, Runnable onClick) {
                this.name = name;
                this.image = image;
                this.onClick = onClick;
            }
        }
    }
}