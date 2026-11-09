package it.pintux.life.common.form.obj;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class FormMenu {
    private String formCommand;
    private String commandIntercept;
    private String permission;
    private String formTitle;
    private String formContent;
    private String formType;
    private List<FormButton> formButtons;
    private Map<String, Map<String, Object>> components;
    private List<String> globalActions;
    private String closeActions;
    private String openCondition;
    private String openConditionMessage;
    private int cooldownSeconds;

    public FormMenu(String formCommand, String commandIntercept, String permission, String formTitle, String formContent, String formType, List<FormButton> formButtons, Map<String, Map<String, Object>> components, List<String> globalActions) {
        this.formCommand = formCommand;
        this.commandIntercept = commandIntercept;
        this.permission = permission;
        this.formTitle = formTitle;
        this.formContent = formContent;
        this.formType = formType;
        this.formButtons = formButtons;
        this.components = components;
        this.globalActions = globalActions;
    }


    public String getFormCommand() {
        return formCommand;
    }

    public int getRequiredArgs() {
        return formCommand == null || formCommand.isBlank() ? 0 : formCommand.trim().split("\\s+").length - 1;
    }


    public String getCommandIntercept() {
        return commandIntercept;
    }


    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getFormTitle() {
        return formTitle;
    }


    public String getFormContent() {
        return formContent;
    }


    public String getFormType() {
        return formType;
    }


    public List<FormButton> getFormButtons() {
        return formButtons;
    }


    public Map<String, Map<String, Object>> getComponents() {
        return components;
    }


    public List<String> getGlobalActions() {
        return globalActions;
    }

    public String getCloseActions() {
        return closeActions;
    }

    public void setCloseActions(String closeActions) {
        this.closeActions = closeActions;
    }

    public String getOpenCondition() {
        return openCondition;
    }

    public String getOpenConditionMessage() {
        return openConditionMessage;
    }

    public void setOpenCondition(String openCondition, String openConditionMessage) {
        this.openCondition = openCondition;
        this.openConditionMessage = openConditionMessage;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(int cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }


    @Override
    public String toString() {
        return new StringJoiner(", ", FormMenu.class.getSimpleName() + "[", "]")
                .add("formCommand='" + formCommand + "'")
                .add("formTitle='" + formTitle + "'")
                .add("formContent='" + formContent + "'")
                .add("formType='" + formType + "'")
                .add("formButtons=" + formButtons)
                .toString();
    }
}


