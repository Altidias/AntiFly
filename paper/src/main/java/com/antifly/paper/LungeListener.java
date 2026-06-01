package com.antifly.paper;

import com.antifly.common.AntiFlyConstants;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.plugin.EventExecutor;


final class LungeListener implements Listener {
    private static final String EVENT_CLASS_NAME = "io.papermc.paper.event.entity.EntityLungeEvent";

    private final AntiFlyPlugin plugin;

    private LungeListener(AntiFlyPlugin plugin) {
        this.plugin = plugin;
    }

    static boolean tryRegister(AntiFlyPlugin plugin) {
        Class<? extends Event> eventClass;
        MethodHandle getLungePower;
        try {
            Class<?> raw = Class.forName(EVENT_CLASS_NAME);
            eventClass = raw.asSubclass(Event.class);
            getLungePower = MethodHandles.publicLookup()
                .findVirtual(raw, "getLungePower", MethodType.methodType(int.class));
        } catch (ClassNotFoundException ex) {
            return false;
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().warning("EntityLungeEvent exists but could not be bound: " + ex);
            return false;
        }

        LungeListener listener = new LungeListener(plugin);
        EventExecutor executor = (ignored, event) -> {
            if (!eventClass.isInstance(event) || !(event instanceof EntityEvent entityEvent)) {
                return;
            }
            if (!(entityEvent.getEntity() instanceof Player player)) {
                return;
            }
            try {
                int power = (int) getLungePower.invoke(event);
                listener.onLunge(player, power);
            } catch (Throwable t) {
                plugin.getLogger().warning("Failed to handle EntityLungeEvent: " + t);
            }
        };
        plugin.getServer().getPluginManager().registerEvent(
            eventClass, listener, EventPriority.MONITOR, executor, plugin, true);
        return true;
    }

    private void onLunge(Player player, int power) {
        if (!plugin.isAntiFlyEnabled()) {
            return;
        }
        AntiFlyPlugin.PlayerState state = plugin.getState(player);
        state.lungeGraceTicks = plugin.getSettings().spearLungeGraceTicks;
        state.lungeAllowance = AntiFlyConstants.SPEAR_LUNGE_SPEED_PER_LEVEL * Math.max(1, power)
            + AntiFlyConstants.SPEAR_LUNGE_ALLOWANCE_BUFFER;
    }
}
