package it.pintux.life.essentialsaddon.provider;

import it.pintux.life.essentialsaddon.api.TpaProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;

public final class HuskHomesTpaProvider implements TpaProvider {
    private static final String RECEIVE_EVENT = "net.william278.huskhomes.event.ReceiveTeleportRequestEvent";
    private static final String REPLY_EVENT = "net.william278.huskhomes.event.ReplyTeleportRequestEvent";
    private static final long REQUEST_TTL_MILLIS = 60_000L;

    private final Object api;
    private final ClassLoader huskLoader;
    private final PendingRequestLog pending = new PendingRequestLog(REQUEST_TTL_MILLIS, System::currentTimeMillis);

    public HuskHomesTpaProvider() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("HuskHomes");
        if (plugin == null) throw new IllegalStateException("HuskHomes not found");
        try {
            this.huskLoader = plugin.getClass().getClassLoader();
            Class<?> apiClass = huskLoader.loadClass("net.william278.huskhomes.api.HuskHomesAPI");
            this.api = apiClass.getMethod("getInstance").invoke(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize HuskHomesTpaProvider", e);
        }
    }

    @Override
    public String getProviderId() { return "HuskHomes"; }

    @Override
    public boolean isReady() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("HuskHomes");
        return api != null && plugin != null && plugin.isEnabled();
    }

    @Override
    public boolean registerRequestListener(Plugin plugin, RequestListener listener) {
        Class<? extends Event> receiveEvent = loadEvent(RECEIVE_EVENT);
        if (receiveEvent == null) {
            return false;
        }
        Listener owner = new Listener() {
        };
        EventExecutor receiveExecutor = (ignored, event) -> handleReceive(event, listener);
        try {
            Bukkit.getPluginManager().registerEvent(
                    receiveEvent, owner, EventPriority.MONITOR, receiveExecutor, plugin, true);
        } catch (Exception | LinkageError failure) {
            return false;
        }

        Class<? extends Event> replyEvent = loadEvent(REPLY_EVENT);
        if (replyEvent != null) {
            EventExecutor replyExecutor = (ignored, event) -> handleReply(event);
            try {
                Bukkit.getPluginManager().registerEvent(
                        replyEvent, owner, EventPriority.MONITOR, replyExecutor, plugin, true);
            } catch (Exception | LinkageError ignored) {
            }
        }
        return true;
    }

    @Override
    public boolean sendTpaRequest(Player sender, Player target) {
        return dispatch(sender, "tpa " + target.getName());
    }

    @Override
    public boolean sendTpahereRequest(Player sender, Player target) {
        return dispatch(sender, "tpahere " + target.getName());
    }

    @Override
    public boolean acceptTpa(Player target) {
        return respond(target, "tpaccept");
    }

    @Override
    public boolean denyTpa(Player target) {
        return respond(target, "tpdecline");
    }

    @Override
    public boolean cancelTpa(Player sender) {
        return false;
    }

    @Override
    public List<String> getPendingRequests(Player player) {
        return pending.senders(player.getName());
    }

    @Override
    public boolean hasPendingRequest(Player player) {
        return !getPendingRequests(player).isEmpty();
    }

    @Override
    public String getPendingRequestSender(Player player) {
        return pending.newestSender(player.getName());
    }

    private boolean respond(Player target, String command) {
        String senderName = getPendingRequestSender(target);
        boolean dispatched = dispatch(target, senderName == null ? command : command + " " + senderName);
        if (dispatched) {
            pending.forget(target.getName(), senderName);
        }
        return dispatched;
    }

    private boolean dispatch(Player player, String command) {
        if (!isReady()) return false;
        try {
            return player.performCommand(command);
        } catch (Exception e) {
            return false;
        }
    }

    private void handleReceive(Object event, RequestListener listener) {
        String recipientName = resolveName(event, "getRecipient");
        String senderName = senderName(event);
        if (recipientName == null || senderName == null) {
            return;
        }
        pending.record(recipientName, senderName);
        Player target = Bukkit.getPlayerExact(recipientName);
        if (target != null) {
            listener.onRequest(target, senderName);
        }
    }

    private void handleReply(Object event) {
        String recipientName = resolveName(event, "getRecipient");
        if (recipientName == null) {
            return;
        }
        pending.forget(recipientName, senderName(event));
    }

    private String senderName(Object event) {
        String senderName = resolveName(event, "getSender", "getRequester");
        if (senderName != null) {
            return senderName;
        }
        return resolveName(call(event, "getRequest"), "getRequesterName", "getRequester");
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Event> loadEvent(String className) {
        try {
            Class<?> loaded = huskLoader.loadClass(className);
            if (!Event.class.isAssignableFrom(loaded)) return null;
            return (Class<? extends Event>) loaded;
        } catch (Exception | LinkageError failure) {
            return null;
        }
    }

    private String resolveName(Object holder, String... methodNames) {
        Object value = call(holder, methodNames);
        if (value instanceof String name) {
            return name.isBlank() ? null : name;
        }
        Object username = call(value, "getUsername", "getName");
        return username instanceof String name && !name.isBlank() ? name : null;
    }

    private Object call(Object holder, String... methodNames) {
        if (holder == null) return null;
        for (String methodName : methodNames) {
            try {
                Method method = holder.getClass().getMethod(methodName);
                method.setAccessible(true);
                return method.invoke(holder);
            } catch (Exception | LinkageError ignored) {
            }
        }
        return null;
    }
}
