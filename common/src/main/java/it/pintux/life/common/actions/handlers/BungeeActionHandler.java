package it.pintux.life.common.actions.handlers;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.platform.PlatformPlayerManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.ValidationUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BungeeActionHandler extends BaseActionHandler {

    private final PlatformPlayerManager playerManager;
    private static final String CHANNEL = "BungeeCord";

    private static final Pattern SUBCHANNEL_PATTERN = Pattern.compile(
            "subchannel:\\s*\"([^\"]+)\""
    );

    public BungeeActionHandler(PlatformPlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    @Override
    public String getActionType() {
        return "bungee";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionData, ActionSystem.ActionContext context) {
        if (ValidationUtils.isNullOrEmpty(actionData)) {
            return createFailureResult(MessageData.EXECUTION_ERROR, createReplacements("error", "Action data cannot be empty"), player);
        }

        try {
            String subchannel;
            List<String> args;

            // Check if new format
            if (isNewCurlyBraceFormat(actionData, "bungee")) {
                 Matcher matcher = SUBCHANNEL_PATTERN.matcher(actionData);
                 if (matcher.find()) {
                     subchannel = matcher.group(1);
                 } else {
                     return createFailureResult(MessageData.EXECUTION_ERROR, createReplacements("error", "No subchannel specified"), player);
                 }
                 args = parseNewFormatValues(actionData);
            } else {
                // Legacy format: bungee: SubChannel arg1 arg2
                String processed = processPlaceholders(actionData, context, player);
                String[] parts = processed.split(" ");
                if (parts.length == 0) {
                     return createFailureResult(MessageData.EXECUTION_ERROR, createReplacements("error", "No subchannel specified"), player);
                }
                subchannel = parts[0];
                args = new java.util.ArrayList<>();
                for (int i = 1; i < parts.length; i++) {
                    args.add(parts[i]);
                }
            }

            // Process placeholders
            subchannel = processPlaceholders(subchannel, context, player);
            for (int i = 0; i < args.size(); i++) {
                args.set(i, processPlaceholders(args.get(i), context, player));
            }

            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF(subchannel);
            for (String arg : args) {
                out.writeUTF(arg);
            }

            playerManager.sendByteArray(player, CHANNEL, out.toByteArray());
            
            return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Sent BungeeCord message: " + subchannel), player);

        } catch (Exception e) {
            logger.error("Error executing bungee action: " + e.getMessage());
            return createFailureResult(MessageData.EXECUTION_ERROR, createReplacements("error", e.getMessage()), player);
        }
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return !ValidationUtils.isNullOrEmpty(actionValue);
    }

    @Override
    public String getDescription() {
        return "Sends a plugin message to the BungeeCord channel";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{
            "Legacy Format Examples:",
            "bungee: Connect lobby",
            "bungee: ConnectOther {player} survival",
            "bungee: KickPlayer {player} \"Reason for kick\"",
            "bungee: Message {player} \"Hello from BungeeCord\"",
            "",
            "New Format Examples:",
            "bungee { subchannel: \"Connect\" - \"lobby\" }",
            "bungee { subchannel: \"ConnectOther\" - \"{player}\" - \"survival\" }",
            "bungee { subchannel: \"Message\" - \"{player}\" - \"§aWelcome to the server!\" }",
            "bungee { subchannel: \"KickPlayer\" - \"{player}\" - \"§cYou have been kicked!\" }"
        };
    }
}
