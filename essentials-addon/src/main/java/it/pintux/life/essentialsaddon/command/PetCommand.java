package it.pintux.life.essentialsaddon.command;

import it.pintux.life.essentialsaddon.BedrockEssentialsAddonPlugin;
import it.pintux.life.essentialsaddon.service.BedrockPetService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Executor for the addon's {@code /pet} command. Bedrock players get the Bedrock pet form;
 * Java players (or when the MyPet module is off) are forwarded to MyPet's own {@code /petlist}.
 */
public final class PetCommand implements CommandExecutor {

    private final BedrockEssentialsAddonPlugin plugin;

    public PetCommand(BedrockEssentialsAddonPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        BedrockPetService service = plugin.getBedrockPetService();
        if (service != null && service.shouldHandle(player)) {
            service.openPetList(player);
            return true;
        }

        StringBuilder petList = new StringBuilder("petlist");
        for (String arg : args) {
            petList.append(' ').append(arg);
        }
        player.performCommand(petList.toString());
        return true;
    }
}
