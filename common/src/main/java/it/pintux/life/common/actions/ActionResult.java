package it.pintux.life.common.actions;


public class ActionResult {
    
    public enum Status {
        SUCCESS,
        FAILURE,
        PARTIAL_SUCCESS,
        SKIPPED
    }
    
    private final Status status;
    private final String message;
    private final Throwable exception;
    private final Object data;
    
    private ActionResult(Status status, String message, Throwable exception, Object data) {
        this.status = status;
        this.message = message;
        this.exception = exception;
        this.data = data;
    }
    
    public static ActionResult success() {
        return new ActionResult(Status.SUCCESS, null, null, null);
    }
    
    public static ActionResult success(String message) {
        return new ActionResult(Status.SUCCESS, message, null, null);
    }
    
    public static ActionResult success(String message, Object data) {
        return new ActionResult(Status.SUCCESS, message, null, data);
    }
    
    public static ActionResult failure(String message) {
        return new ActionResult(Status.FAILURE, message, null, null);
    }
    
    public static ActionResult failure(String message, Throwable exception) {
        return new ActionResult(Status.FAILURE, message, exception, null);
    }
    
    public static ActionResult partialSuccess(String message) {
        return new ActionResult(Status.PARTIAL_SUCCESS, message, null, null);
    }
    
    public static ActionResult skipped(String message) {
        return new ActionResult(Status.SKIPPED, message, null, null);
    }
    
    public Status getStatus() {
        return status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public Throwable getException() {
        return exception;
    }
    
    public Object getData() {
        return data;
    }
    
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
    
    public boolean isFailure() {
        return status == Status.FAILURE;
    }
    
    public boolean hasException() {
        return exception != null;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ActionResult{status=").append(status);
        if (message != null) {
            sb.append(", message='").append(message).append("'");
        }
        if (exception != null) {
            sb.append(", exception=").append(exception.getClass().getSimpleName());
        }
        sb.append("}");
        return sb.toString();
    }
}
