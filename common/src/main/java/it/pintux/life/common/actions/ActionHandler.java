package it.pintux.life.common.actions;

import it.pintux.life.common.utils.FormPlayer;

import java.util.Map;


public interface ActionHandler {


    String getActionType();


    ActionResult execute(FormPlayer player, String actionValue, ActionContext context);


    boolean isValidAction(String actionValue);


    String getDescription();


    String[] getUsageExamples();
}
