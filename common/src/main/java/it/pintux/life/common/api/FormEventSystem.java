package it.pintux.life.common.api;

import it.pintux.life.common.api.BedrockGUIApi.*;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;


public class FormEventSystem {
    
    private static final Logger logger = Logger.getLogger(FormEventSystem.class);
    
    private final Map<String, List<FormEventListener>> eventListeners = new ConcurrentHashMap<>();
    private final Map<String, FormEventHandler> eventHandlers = new ConcurrentHashMap<>();
    private final List<FormInterceptor> interceptors = new CopyOnWriteArrayList<>();
    
    public FormEventSystem() {
        registerBuiltInEvents();
    }
    
    
    private void registerBuiltInEvents() {
        
        registerEventType(FormEvent.FORM_CREATED);
        registerEventType(FormEvent.FORM_OPENED);
        registerEventType(FormEvent.FORM_CLOSED);
        registerEventType(FormEvent.FORM_SUBMITTED);
        registerEventType(FormEvent.FORM_CANCELLED);
        
        
        registerEventType(FormEvent.BUTTON_CLICKED);
        registerEventType(FormEvent.INPUT_CHANGED);
        registerEventType(FormEvent.DROPDOWN_SELECTED);
        registerEventType(FormEvent.SLIDER_MOVED);
        registerEventType(FormEvent.TOGGLE_SWITCHED);
        
        
        registerEventType(FormEvent.VALIDATION_STARTED);
        registerEventType(FormEvent.VALIDATION_PASSED);
        registerEventType(FormEvent.VALIDATION_FAILED);
        
        
        registerEventType(FormEvent.FORM_ERROR);
        registerEventType(FormEvent.SEND_FAILED);
        
        logger.info("Registered " + eventListeners.size() + " built-in event types");
    }
    
    
    public void registerEventType(String eventType) {
        eventListeners.putIfAbsent(eventType, new CopyOnWriteArrayList<>());
    }
    
    
    public void addEventListener(String eventType, FormEventListener listener) {
        List<FormEventListener> listeners = eventListeners.get(eventType);
        if (listeners != null) {
            listeners.add(listener);
        } else {
            logger.warn("Unknown event type: " + eventType);
        }
    }
    
    
    public void removeEventListener(String eventType, FormEventListener listener) {
        List<FormEventListener> listeners = eventListeners.get(eventType);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }
    
    
    public void registerEventHandler(String eventType, FormEventHandler handler) {
        eventHandlers.put(eventType, handler);
    }
    
    
    public void addInterceptor(FormInterceptor interceptor) {
        interceptors.add(interceptor);
    }
    
    
    public void removeInterceptor(FormInterceptor interceptor) {
        interceptors.remove(interceptor);
    }
    
    
    public void fireEvent(FormEventData eventData) {
        String eventType = eventData.getEventType();
        
        
        for (FormInterceptor interceptor : interceptors) {
            if (interceptor.shouldIntercept(eventData)) {
                FormEventData modifiedData = interceptor.intercept(eventData);
                if (modifiedData == null) {
                    
                    return;
                }
                eventData = modifiedData;
            }
        }
        
        
        FormEventHandler handler = eventHandlers.get(eventType);
        if (handler != null) {
            try {
                handler.handle(eventData);
            } catch (Exception e) {
                logger.error("Error in event handler for " + eventType, e);
            }
        }
        
        
        List<FormEventListener> listeners = eventListeners.get(eventType);
        if (listeners != null) {
            for (FormEventListener listener : listeners) {
                try {
                    listener.onEvent(eventData);
                } catch (Exception e) {
                    logger.error("Error in event listener for " + eventType, e);
                }
            }
        }
    }
    
    
    public FormEventData createFormEvent(String eventType, FormPlayer player, FormBuilder form) {
        return new FormEventData(eventType, player, form);
    }
    
    
    public FormEventData createInteractionEvent(String eventType, FormPlayer player, FormBuilder form, 
                                               String componentId, Object value) {
        FormEventData eventData = new FormEventData(eventType, player, form);
        eventData.setComponentId(componentId);
        eventData.setValue(value);
        return eventData;
    }
    
    
    public FormEventData createValidationEvent(String eventType, FormPlayer player, FormBuilder form, 
                                             ValidationResult result) {
        FormEventData eventData = new FormEventData(eventType, player, form);
        eventData.setValidationResult(result);
        return eventData;
    }
    
    
    public FormEventData createErrorEvent(String eventType, FormPlayer player, FormBuilder form, 
                                        String errorMessage, Throwable cause) {
        FormEventData eventData = new FormEventData(eventType, player, form);
        eventData.setErrorMessage(errorMessage);
        eventData.setCause(cause);
        return eventData;
    }
    
    
    
    
    @FunctionalInterface
    public interface FormEventListener {
        void onEvent(FormEventData eventData);
    }
    
    
    @FunctionalInterface
    public interface FormEventHandler {
        void handle(FormEventData eventData);
    }
    
    
    public interface FormInterceptor {
        boolean shouldIntercept(FormEventData eventData);
        FormEventData intercept(FormEventData eventData); 
    }
    
    
    
    
    public static class FormEventData {
        private final String eventType;
        private final FormPlayer player;
        private final FormBuilder form;
        private final long timestamp;
        private final Map<String, Object> metadata;
        
        
        private String componentId;
        private Object value;
        private ValidationResult validationResult;
        private String errorMessage;
        private Throwable cause;
        
        public FormEventData(String eventType, FormPlayer player, FormBuilder form) {
            this.eventType = eventType;
            this.player = player;
            this.form = form;
            this.timestamp = System.currentTimeMillis();
            this.metadata = new HashMap<>();
        }
        
        
        public String getEventType() { return eventType; }
        public FormPlayer getPlayer() { return player; }
        public FormBuilder getForm() { return form; }
        public long getTimestamp() { return timestamp; }
        public Map<String, Object> getMetadata() { return metadata; }
        public String getComponentId() { return componentId; }
        public Object getValue() { return value; }
        public ValidationResult getValidationResult() { return validationResult; }
        public String getErrorMessage() { return errorMessage; }
        public Throwable getCause() { return cause; }
        
        
        public void setComponentId(String componentId) { this.componentId = componentId; }
        public void setValue(Object value) { this.value = value; }
        public void setValidationResult(ValidationResult validationResult) { this.validationResult = validationResult; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public void setCause(Throwable cause) { this.cause = cause; }
        
        
        public void setMetadata(String key, Object value) {
            metadata.put(key, value);
        }
        
        public Object getMetadata(String key) {
            return metadata.get(key);
        }
        
        @SuppressWarnings("unchecked")
        public <T> T getMetadata(String key, Class<T> type) {
            Object value = metadata.get(key);
            return type.isInstance(value) ? (T) value : null;
        }
    }
    
    
    
    
    public static class FormEvent {
        
        public static final String FORM_CREATED = "form.created";
        public static final String FORM_OPENED = "form.opened";
        public static final String FORM_CLOSED = "form.closed";
        public static final String FORM_SUBMITTED = "form.submitted";
        public static final String FORM_CANCELLED = "form.cancelled";
        
        
        public static final String BUTTON_CLICKED = "button.clicked";
        public static final String INPUT_CHANGED = "input.changed";
        public static final String DROPDOWN_SELECTED = "dropdown.selected";
        public static final String SLIDER_MOVED = "slider.moved";
        public static final String TOGGLE_SWITCHED = "toggle.switched";
        
        
        public static final String VALIDATION_STARTED = "validation.started";
        public static final String VALIDATION_PASSED = "validation.passed";
        public static final String VALIDATION_FAILED = "validation.failed";
        
        
        public static final String FORM_ERROR = "form.error";
        public static final String SEND_FAILED = "send.failed";
    }
    
    
    
    
    public static class LoggingInterceptor implements FormInterceptor {
        private final Logger logger;
        private final Set<String> loggedEvents;
        
        public LoggingInterceptor() {
            this.logger = Logger.getLogger(LoggingInterceptor.class);
            this.loggedEvents = new HashSet<>();
            
            loggedEvents.addAll(Arrays.asList(
                FormEvent.FORM_CREATED, FormEvent.FORM_OPENED, FormEvent.FORM_CLOSED,
                FormEvent.FORM_SUBMITTED, FormEvent.FORM_CANCELLED, FormEvent.BUTTON_CLICKED,
                FormEvent.VALIDATION_FAILED, FormEvent.FORM_ERROR, FormEvent.SEND_FAILED
            ));
        }
        
        public LoggingInterceptor(Set<String> eventsToLog) {
            this.logger = Logger.getLogger(LoggingInterceptor.class);
            this.loggedEvents = new HashSet<>(eventsToLog);
        }
        
        @Override
        public boolean shouldIntercept(FormEventData eventData) {
            return loggedEvents.contains(eventData.getEventType());
        }
        
        @Override
        public FormEventData intercept(FormEventData eventData) {
            String playerName = eventData.getPlayer() != null ? eventData.getPlayer().getName() : "Unknown";
            String formTitle = eventData.getForm() != null ? eventData.getForm().getTitle() : "Unknown";
            
            logger.info(String.format("Form Event: %s | Player: %s | Form: %s", 
                eventData.getEventType(), playerName, formTitle));
            
            if (eventData.getErrorMessage() != null) {
                logger.error("Error: " + eventData.getErrorMessage(), eventData.getCause());
            }
            
            return eventData; 
        }
    }
    
    
    public static class PermissionInterceptor implements FormInterceptor {
        private final Map<String, String> eventPermissions;
        
        public PermissionInterceptor() {
            this.eventPermissions = new HashMap<>();
        }
        
        public PermissionInterceptor addPermissionCheck(String eventType, String permission) {
            eventPermissions.put(eventType, permission);
            return this;
        }
        
        @Override
        public boolean shouldIntercept(FormEventData eventData) {
            return eventPermissions.containsKey(eventData.getEventType());
        }
        
        @Override
        public FormEventData intercept(FormEventData eventData) {
            String requiredPermission = eventPermissions.get(eventData.getEventType());
            
            if (requiredPermission != null && eventData.getPlayer() != null) {
                if (!eventData.getPlayer().hasPermission(requiredPermission)) {
                    
                    return null;
                }
            }
            
            return eventData;
        }
    }
    
    
    public static class RateLimitInterceptor implements FormInterceptor {
        private final Map<String, Long> lastEventTimes = new ConcurrentHashMap<>();
        private final long cooldownMs;
        private final Set<String> rateLimitedEvents;
        
        public RateLimitInterceptor(long cooldownMs) {
            this.cooldownMs = cooldownMs;
            this.rateLimitedEvents = new HashSet<>(Arrays.asList(
                FormEvent.FORM_OPENED, FormEvent.BUTTON_CLICKED, FormEvent.FORM_SUBMITTED
            ));
        }
        
        public RateLimitInterceptor(long cooldownMs, Set<String> eventsToLimit) {
            this.cooldownMs = cooldownMs;
            this.rateLimitedEvents = new HashSet<>(eventsToLimit);
        }
        
        @Override
        public boolean shouldIntercept(FormEventData eventData) {
            return rateLimitedEvents.contains(eventData.getEventType());
        }
        
        @Override
        public FormEventData intercept(FormEventData eventData) {
            if (eventData.getPlayer() == null) {
                return eventData;
            }
            
            String key = eventData.getPlayer().getUniqueId() + ":" + eventData.getEventType();
            long currentTime = System.currentTimeMillis();
            Long lastTime = lastEventTimes.get(key);
            
            if (lastTime != null && (currentTime - lastTime) < cooldownMs) {
                
                return null;
            }
            
            lastEventTimes.put(key, currentTime);
            return eventData;
        }
    }
    
    
    
    
    public static FormEventListener createListener(Consumer<FormEventData> consumer) {
        return consumer::accept;
    }
    
    
    public static FormEventListener createFilteredListener(java.util.function.Predicate<FormEventData> filter, 
                                                          Consumer<FormEventData> consumer) {
        return eventData -> {
            if (filter.test(eventData)) {
                consumer.accept(eventData);
            }
        };
    }
    
    
    public static FormEventListener createPlayerListener(String playerName, Consumer<FormEventData> consumer) {
        return createFilteredListener(
            eventData -> eventData.getPlayer() != null && 
                        playerName.equals(eventData.getPlayer().getName()),
            consumer
        );
    }
    
    
    public static FormEventListener createFormListener(String formTitle, Consumer<FormEventData> consumer) {
        return createFilteredListener(
            eventData -> eventData.getForm() != null && 
                        formTitle.equals(eventData.getForm().getTitle()),
            consumer
        );
    }
}
