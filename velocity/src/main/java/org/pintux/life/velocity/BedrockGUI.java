package org.pintux.life.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.pintux.life.common.FloodgateUtil;
import org.pintux.life.common.api.BedrockGuiAPI;
import org.pintux.life.common.form.FormMenuUtil;
import org.pintux.life.common.utils.FormPlayer;
import org.pintux.life.common.utils.MessageConfig;
import org.pintux.life.common.utils.MessageData;
import org.pintux.life.velocity.utils.VelocityConfig;
import org.pintux.life.velocity.utils.VelocityPlayer;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

@Plugin(
        id = "bedrockgui-velocity",
        name = "BedrockGUI-Velocity",
        version = "1.7.4",
        description = "BedrockGUI plugin for Velocity",
        authors = {"pintux"}
)
public class BedrockGUI {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private FormMenuUtil formMenuUtil;
    private MessageData messageData;
    private static BedrockGUI instance;
    private BedrockGuiAPI api;

    @Inject
    public BedrockGUI(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getCommandManager().register("bedrockguivelocity", new BedrockCommand(this), "bguivelocity");
        server.getEventManager().register(this, this);
        try {
            makeConfig("config.yml");
            makeConfig("messages.yml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        reloadData();
        //new Metrics(this, server, logger, dataDirectory, 23364);
        instance = this;
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        instance = null;
    }

    public void reloadData() {
        ConfigurationNode mainConfig;
        ConfigurationNode messageConfig;
        try {
            YamlConfigurationLoader mainLoader = YamlConfigurationLoader.builder()
                    .path(dataDirectory.resolve("config.yml"))
                    .build();
            YamlConfigurationLoader messageLoader = YamlConfigurationLoader.builder()
                    .path(dataDirectory.resolve("messages.yml"))
                    .build();
            mainConfig = mainLoader.load();
            messageConfig = messageLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        MessageConfig configHandler = null;
        messageData = new MessageData(configHandler);
        formMenuUtil = new FormMenuUtil(new VelocityConfig(mainConfig), messageData);
    }

    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        if (!event.getMessage().startsWith("/")) {
            return;
        }

        Player player = event.getPlayer();
        FormPlayer formPlayer = new VelocityPlayer(player);

        if (!FloodgateUtil.isFloodgate(player.getUniqueId())) {
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
                        event.setResult(PlayerChatEvent.ChatResult.denied());
                        formMenuUtil.openForm(formPlayer, key, args);
                    } else {
                        formPlayer.sendMessage(messageData.getValue(MessageData.MENU_ARGS, Map.of("args", requiredArgs), null));
                    }
                }
            }
        });
    }

    public void makeConfig(String config) throws IOException {
        File dataFolder = dataDirectory.toFile();
        if (!dataFolder.exists()) {
            logger.info("Created config folder: " + dataFolder.mkdir());
        }

        File configFile = new File(dataFolder, config);

        if (!configFile.exists()) {
            FileOutputStream outputStream = new FileOutputStream(configFile);
            InputStream in = getClass().getClassLoader().getResourceAsStream(config);
            if (in != null) {
                in.transferTo(outputStream);
            }
        }
    }

    public static BedrockGUI getInstance() {
        return instance;
    }

    public FormMenuUtil getFormMenuUtil() {
        return formMenuUtil;
    }

    public MessageData getMessageData() {
        return messageData;
    }

    public BedrockGuiAPI getApi() {
        if (api == null) {
            api = new BedrockGuiAPI(formMenuUtil);
        }
        return api;
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }
}