package it.pintux.life.bungee;

import it.pintux.life.bungee.utils.BungeeConfig;
import it.pintux.life.bungee.utils.BungeeMessageConfig;
import it.pintux.life.bungee.utils.BungeePlayer;
import it.pintux.life.bungee.platform.BungeeCommandExecutor;
import it.pintux.life.bungee.platform.BungeeEconomyManager;
import it.pintux.life.bungee.platform.BungeeFormSender;
import it.pintux.life.bungee.platform.BungeePlayerChecker;
import it.pintux.life.bungee.platform.BungeeSoundManager;
import org.geysermc.floodgate.api.FloodgateApi;
import it.pintux.life.common.form.FormMenuUtil;
import it.pintux.life.common.utils.MessageConfig;
import it.pintux.life.common.utils.MessageData;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

public class BedrockGUI extends Plugin implements Listener {

    private FormMenuUtil formMenuUtil;
    private MessageData messageData;
    private Configuration mainConfig;
    private Configuration messageConfig;

    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerCommand(this, new BedrockCommand(this, "bedrockguiproxy"));
        getProxy().getPluginManager().registerListener(this, this);
        try {
            makeConfig("config.yml");
            makeConfig("messages.yml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        reloadData();
        new Metrics(this, 23364);
    }

    @Override
    public void onDisable() {
    }

    public void reloadData() {
        try {
            mainConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
            messageConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "messages.yml"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        MessageConfig configHandler = new BungeeMessageConfig(messageConfig);
        messageData = new MessageData(configHandler);

        // Initialize platform implementations
        BungeeCommandExecutor commandExecutor = new BungeeCommandExecutor();
        BungeeSoundManager soundManager = new BungeeSoundManager();
        BungeeEconomyManager economyManager = new BungeeEconomyManager();
        BungeeFormSender formSender = new BungeeFormSender();

        // Initialize FormMenuUtil with platform implementations
        formMenuUtil = new FormMenuUtil(new BungeeConfig(mainConfig), messageData, commandExecutor, soundManager, economyManager, formSender);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerPreprocessCommand(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer)) {
            return;
        }
        if (!(event.isCommand()) || !(event.isProxyCommand())) {
            return;
        }
        BungeePlayer player = new BungeePlayer((ProxiedPlayer) event.getSender());

        try {
            if (!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
                return;
            }
        } catch (Exception e) {
            return;
        }
        String message = event.getMessage();

        String commandWithoutSlash = message.substring(1).toLowerCase();

        String[] parts = commandWithoutSlash.split(" ");
        String commandName = parts[0];
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        formMenuUtil.getFormMenus().forEach((key, formMenu) -> {
            String formCommand = formMenu.getFormCommand();

            if (formCommand != null) {
                String[] formCommandParts = formCommand.split(" ");
                String baseCommand = formCommandParts[0];

                if (commandName.equalsIgnoreCase(baseCommand)) {
                    int requiredArgs = formCommandParts.length - 1;

                    if (args.length >= requiredArgs) {
                        event.setCancelled(true);
                        formMenuUtil.openForm(player, key, args);
                    } else {
                        player.sendMessage(messageData.getValue(MessageData.MENU_ARGS, Map.of("args", requiredArgs), null));
                    }
                }
            }
        });
    }

    public void makeConfig(String config) throws IOException {
        if (!getDataFolder().exists()) {
            getLogger().info("Created config folder: " + getDataFolder().mkdir());
        }

        File configFile = new File(getDataFolder(), config);

        if (!configFile.exists()) {
            FileOutputStream outputStream = new FileOutputStream(configFile);
            InputStream in = getResourceAsStream(config);
            in.transferTo(outputStream);
        }
    }


    public FormMenuUtil getFormMenuUtil() {
        return formMenuUtil;
    }

    public MessageData getMessageData() {
        return messageData;
    }
}