package it.pintux.life.essentialsaddon.service;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.essentialsaddon.api.BedrockPlayerDetector;
import it.pintux.life.essentialsaddon.config.EssentialsAddonConfiguration;
import it.pintux.life.essentialsaddon.model.PetView;
import it.pintux.life.essentialsaddon.model.ShopPetView;
import it.pintux.life.essentialsaddon.model.SkilltreeView;
import it.pintux.life.essentialsaddon.util.BukkitFormPlayer;
import it.pintux.life.essentialsaddon.util.PetActionPayloads;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public final class BedrockPetService {

    private final Logger logger;
    private final EssentialsAddonConfiguration configuration;
    private final PetCatalogService petCatalog;
    private final BedrockPlayerDetector detector;

    public BedrockPetService(Logger logger, EssentialsAddonConfiguration configuration,
                             PetCatalogService petCatalog, BedrockPlayerDetector detector) {
        this.logger = logger;
        this.configuration = configuration;
        this.petCatalog = petCatalog;
        this.detector = detector;
    }

    public boolean shouldHandle(Player player) {
        return player != null && detector.isBedrockPlayer(player);
    }

    // ---------------- Pet list ----------------

    public void openPetList(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!ensureReady(player)) return;

        petCatalog.listOwnedPets(player, pets -> {
            if (pets.isEmpty()) {
                player.sendMessage(configuration.petNoPets());
                return;
            }
            BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(configuration.petListTitle());
            form.content(configuration.petListContent());
            for (PetView pet : pets) {
                String label = configuration.render(configuration.petListButton(),
                        Map.of("pet_name", safe(pet.name()), "pet_type", prettyType(pet.petType())));
                if (pet.active()) {
                    label = label + configuration.petActiveSuffix();
                }
                form.button(label, fp -> api.executeActionString(fp,
                        "essentials_pet_info:" + PetActionPayloads.encodePet(pet.uuid()),
                        context("pet-list", pet.uuid().toString())));
            }
            form.send(new BukkitFormPlayer(player));
            playSound(player, configuration.soundFormOpen());
        });
    }

    // ---------------- Pet modal ----------------

    public void openPetModal(Player player, UUID petUuid) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;

        petCatalog.listOwnedPets(player, pets -> {
            PetView pet = pets.stream().filter(p -> p.uuid().equals(petUuid)).findFirst().orElse(null);
            if (pet == null) {
                player.sendMessage(configuration.petNoPets());
                return;
            }
            String content = configuration.render(configuration.petInfoContent(), Map.of(
                    "pet_type", prettyType(pet.petType()),
                    "level", pet.hasLevel() ? String.valueOf(pet.level()) : "—",
                    "hp", trim(pet.health()),
                    "max_hp", pet.maxHealth() >= 0 ? trim(pet.maxHealth()) : "?",
                    "hunger", trim(pet.hunger()),
                    "skilltree", safe(pet.skilltreeName())
            ));
            String title = configuration.render(configuration.petInfoTitle(), Map.of("pet_name", safe(pet.name())));
            String firstButton = pet.active() ? configuration.petPutAwayButton() : configuration.petCallButton();

            api.createModalForm(title)
                    .button1(firstButton, fp -> {
                        if (pet.active()) {
                            api.executeActionString(fp, "essentials_pet_sendaway:" + PetActionPayloads.encodePet(pet.uuid()),
                                    context("pet-modal", pet.uuid().toString()));
                        } else {
                            api.executeActionString(fp, "essentials_pet_call:" + PetActionPayloads.encodePet(pet.uuid()),
                                    context("pet-modal", pet.uuid().toString()));
                        }
                    })
                    .button2(configuration.petSkilltreeButton(), fp ->
                            api.executeActionString(fp, "essentials_pet_skilltree_menu:" + PetActionPayloads.encodePet(pet.uuid()),
                                    context("pet-modal", pet.uuid().toString())))
                    .content(content)
                    .send(new BukkitFormPlayer(player));
        });
    }

    public void callPet(Player player, UUID petUuid) {
        petCatalog.call(player, petUuid, success -> {
            if (success) {
                player.sendMessage(configuration.petCallSuccess());
                playSound(player, configuration.soundFormOpen());
            } else {
                player.sendMessage(configuration.petCallFailed());
                playSound(player, configuration.soundActionFailed());
            }
        });
    }

    public void putAwayPet(Player player, UUID petUuid) {
        boolean ok = petCatalog.putAway(player, petUuid);
        if (ok) {
            player.sendMessage(configuration.petPutAwaySuccess());
            playSound(player, configuration.soundFormOpen());
        } else {
            player.sendMessage(configuration.petPutAwayFailed());
            playSound(player, configuration.soundActionFailed());
        }
    }

    // ---------------- Skilltree ----------------

    public void openSkilltreeForm(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!ensureReady(player)) return;

        List<SkilltreeView> trees = petCatalog.listSkilltrees(player);
        if (trees.isEmpty()) {
            player.sendMessage(configuration.petNoActivePet());
            return;
        }
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(configuration.petSkilltreeTitle());
        form.content(configuration.petSkilltreeContent());
        for (SkilltreeView tree : trees) {
            String label = configuration.render(configuration.petSkilltreeOption(),
                    Map.of("skilltree", safe(tree.displayName())));
            if (tree.current()) {
                label = label + configuration.petSkilltreeCurrentSuffix();
            }
            form.button(label, fp -> api.executeActionString(fp,
                    "essentials_pet_skilltree_set:" + PetActionPayloads.encodeSkilltree("active", tree.name()),
                    context("pet-skilltree", tree.name())));
        }
        form.send(new BukkitFormPlayer(player));
        playSound(player, configuration.soundFormOpen());
    }

    public void setSkilltree(Player player, String skilltreeName) {
        boolean ok = petCatalog.setSkilltree(player, skilltreeName);
        if (ok) {
            player.sendMessage(configuration.petSkilltreeSetSuccess());
            playSound(player, configuration.soundFormOpen());
        } else {
            player.sendMessage(configuration.petSkilltreeSetFailed());
            playSound(player, configuration.soundActionFailed());
        }
    }

    // ---------------- Shop ----------------

    public void openPetShop(Player player) {
        openPetShop(player, "");
    }

    public void openPetShop(Player player, String shopId) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) return;
        if (!ensureReady(player)) return;

        petCatalog.listShopEntries(player, shopId, entries -> {
            if (entries.isEmpty()) {
                player.sendMessage(configuration.petNoPets());
                return;
            }
            BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(configuration.petShopTitle());
            form.content(configuration.petShopContent());
            for (ShopPetView entry : entries) {
                String label = configuration.render(configuration.petShopButton(),
                        Map.of("pet_name", safe(entry.displayName()), "price", trim(entry.price())));
                if (entry.owned()) {
                    label = label + configuration.petShopOwnedSuffix();
                }
                final ShopPetView fixed = entry;
                form.button(label, fp -> api.executeActionString(fp,
                        "essentials_pet_shop_open:" + (fixed.shopId() == null ? "" : fixed.shopId()),
                        context("pet-shop", fixed.petId())));
            }
            form.send(new BukkitFormPlayer(player));
            playSound(player, configuration.soundFormOpen());
        });
    }

    public void openNativeShop(Player player, String shopId) {
        petCatalog.openNativeShop(player, shopId, ok -> {
            if (!ok) {
                player.sendMessage(configuration.petNotReady());
                playSound(player, configuration.soundActionFailed());
            }
            // on success MyPet's native shop GUI opens (Geyser renders it for Bedrock) — no message needed
        });
    }

    // ---------------- helpers ----------------

    private boolean ensureReady(Player player) {
        if (petCatalog.isReady()) {
            return true;
        }
        player.sendMessage(configuration.petNotReady());
        return false;
    }

    private BedrockGUIApi requireApi(Player player) {
        try {
            BedrockGUIApi api = BedrockGUIApi.getInstance();
            if (api == null) {
                player.sendMessage(configuration.noBedrockGui());
            }
            return api;
        } catch (IllegalStateException e) {
            player.sendMessage(configuration.noBedrockGui());
            return null;
        }
    }

    private ActionSystem.ActionContext context(String source, String metadata) {
        return ActionSystem.ActionContext.builder()
                .menuName(source)
                .formType("bedrock-pet")
                .metadata("feature", metadata)
                .build();
    }

    private String prettyType(String raw) {
        if (raw == null || raw.isBlank()) return "Unknown";
        String lower = raw.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private String trim(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void playSound(Player player, String soundName) {
        if (!configuration.soundsEnabled() || player == null || !player.isOnline()) return;
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase().replace('.', '_'));
            player.playSound(player.getLocation(), sound, configuration.soundVolume(), configuration.soundPitch());
        } catch (IllegalArgumentException ignored) {
        }
    }
}
