package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionSystem;



import it.pintux.life.common.platform.PlatformSoundManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SoundActionHandler extends BaseActionHandler {
    private final PlatformSoundManager soundManager;

    public SoundActionHandler(PlatformSoundManager soundManager) {
        this.soundManager = soundManager;
    }

    @Override
    public String getActionType() {
        return "sound";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionData, ActionSystem.ActionContext context) {

        ActionSystem.ActionResult validationResult = validateBasicParameters(player, actionData);
        if (validationResult != null) {
            return validationResult;
        }

        try {

            if (isNewCurlyBraceFormat(actionData, "sound")) {
                return executeNewFormat(player, actionData, context);
            }


            List<String> sounds = parseActionData(actionData, context, player);

            if (sounds.isEmpty()) {
                Map<String, Object> errorReplacements = createReplacements("error", "No valid sounds found");
                return createFailureResult(MessageData.EXECUTION_ERROR, errorReplacements, player);
            }


            if (sounds.size() == 1) {
                return executeSingleSound(player, sounds.get(0), null);
            }


            return executeMultipleSoundsFromList(sounds, player);

        } catch (Exception e) {
            logError("sound execution", actionData, player, e);
            Map<String, Object> errorReplacements = createReplacements("error", "Error executing sound: " + e.getMessage());
            return createFailureResult(MessageData.EXECUTION_ERROR, errorReplacements, player, e);
        }
    }


    private ActionSystem.ActionResult executeNewFormat(FormPlayer player, String actionData, ActionSystem.ActionContext context) {
        try {
            List<String> sounds = parseNewFormatValues(actionData);

            if (sounds.isEmpty()) {
                return createFailureResult(MessageData.EXECUTION_ERROR, createReplacements("error", "No sounds found in new format"), player);
            }


            List<String> processedSounds = new ArrayList<>();
            for (String sound : sounds) {
                String processedSound = processPlaceholders(sound, context, player);
                processedSounds.add(processedSound);
            }


            if (processedSounds.size() == 1) {
                return executeSingleSound(player, processedSounds.get(0), null);
            }


            return executeMultipleSoundsFromList(processedSounds, player);

        } catch (Exception e) {
            logger.error("Error executing new format sound action for player " + player.getName() + ": " + e.getMessage());
            return createFailureResult(MessageData.EXECUTION_ERROR, createReplacements("error", "Error parsing new sound format: " + e.getMessage()), player);
        }
    }


    private ActionSystem.ActionResult executeMultipleSoundsFromList(List<String> sounds, FormPlayer player) {
        int successCount = 0;
        int totalCount = sounds.size();
        StringBuilder results = new StringBuilder();

        for (int i = 0; i < sounds.size(); i++) {
            String sound = sounds.get(i);

            try {
                logger.info("Playing sound " + (i + 1) + "/" + totalCount + " for player " + player.getName() + ": " + sound);

                ActionSystem.ActionResult result = executeSingleSound(player, sound, null);

                if (result.isSuccess()) {
                    successCount++;
                    results.append("âś“ Sound ").append(i + 1).append(": ").append(sound).append(" - Success");
                } else {
                    results.append("âś— Sound ").append(i + 1).append(": ").append(sound).append(" - Failed");
                }

                if (i < sounds.size() - 1) {
                    results.append("\n");

                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

            } catch (Exception e) {
                results.append("âś— Sound ").append(i + 1).append(": ").append(sound).append(" - Error: ").append(e.getMessage());
                logger.error("Error playing sound " + (i + 1) + " for player " + player.getName(), e);
                if (i < sounds.size() - 1) {
                    results.append("\n");
                }
            }
        }

        String finalMessage = String.format("Played %d/%d sounds successfully:\n%s",
                successCount, totalCount, results.toString());

        Map<String, Object> replacements = new HashMap<>();
        replacements.put("message", finalMessage);
        replacements.put("success_count", successCount);
        replacements.put("total_count", totalCount);

        if (successCount == totalCount) {
            return createSuccessResult("ACTION_SUCCESS", replacements, player);
        } else if (successCount > 0) {
            return createSuccessResult("ACTION_PARTIAL_SUCCESS", replacements, player);
        } else {
            return createFailureResult(MessageData.EXECUTION_ERROR, replacements, player);
        }
    }


    private ActionSystem.ActionResult executeSingleSound(FormPlayer player, String soundData, ActionSystem.ActionContext context) {

        String processedData = processPlaceholders(soundData.trim(), context, player);


        String[] parts = processedData.split(":");
        String soundName = parts[0];
        float volume = 1.0f;
        float pitch = 1.0f;

        if (parts.length > 1) {
            try {
                volume = Float.parseFloat(parts[1]);
            } catch (NumberFormatException e) {
                logger.warn("Invalid volume value '" + parts[1] + "' for sound: " + soundName);
                volume = 1.0f;
            }
        }

        if (parts.length > 2) {
            try {
                pitch = Float.parseFloat(parts[2]);
            } catch (NumberFormatException e) {
                logger.warn("Invalid pitch value '" + parts[2] + "' for sound: " + soundName);
                pitch = 1.0f;
            }
        }


        boolean success = soundManager.playSound(player, soundName, volume, pitch);

        if (success) {
            logSuccess("sound", soundName + " (vol:" + volume + ", pitch:" + pitch + ")", player);
            return createSuccessResult("ACTION_SOUND_SUCCESS",
                    createReplacements("sound", soundName), player);
        } else {
            logFailure("sound", soundName, player);
            return createFailureResult("ACTION_SOUND_FAILED",
                    createReplacements("sound", soundName), player);
        }
    }


    private boolean validateParameters(FormPlayer player, String actionData) {
        return player != null && actionData != null && !actionData.trim().isEmpty();
    }

    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }

        String trimmed = actionValue.trim();

        // Support new unified format "sound { ... }"
        if (isNewCurlyBraceFormat(trimmed, "sound")) {
            try {
                java.util.List<String> values = parseNewFormatValues(trimmed);
                for (String v : values) {
                    if (!isValidSingleSound(v.trim())) {
                        return false;
                    }
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {

            String listContent = trimmed.substring(1, trimmed.length() - 1);
            String[] sounds = listContent.split(",\\s*");
            for (String sound : sounds) {
                if (!isValidSingleSound(sound.trim())) {
                    return false;
                }
            }
            return true;
        } else {
            return isValidSingleSound(trimmed);
        }
    }

    private boolean isValidSingleSound(String soundData) {
        if (soundData.isEmpty()) return false;

        String[] parts = soundData.split(":");
        if (parts.length == 0 || parts[0].trim().isEmpty()) {
            return false;
        }


        if (parts.length > 1) {
            try {
                Float.parseFloat(parts[1]);
            } catch (NumberFormatException e) {
                return false;
            }
        }

        if (parts.length > 2) {
            try {
                Float.parseFloat(parts[2]);
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String getDescription() {
        return "Plays sounds to players with support for volume and pitch control";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{
                "New Format Examples:",
                "sound { - \"ui.button.click\" }",
                "sound { - \"entity.experience_orb.pickup:0.5:1.2\" }",
                "sound { - \"ui.button.click\" - \"entity.experience_orb.pickup:0.8:1.0\" - \"block.note_block.harp:1.0:0.5\" }",
                "sound { - \"entity.player.levelup:1.0:1.5\" - \"ui.toast.challenge_complete:0.7:1.0\" }"
        };
    }
}


