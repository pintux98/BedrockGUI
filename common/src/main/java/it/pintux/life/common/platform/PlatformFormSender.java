package it.pintux.life.common.platform;

import it.pintux.life.common.utils.FormPlayer;
import org.geysermc.cumulus.form.Form;

import java.util.UUID;


public interface PlatformFormSender {


    boolean sendForm(FormPlayer player, Form form);


    boolean isBedrockPlayer(UUID playerUuid);


    boolean isFormSystemAvailable();
}

