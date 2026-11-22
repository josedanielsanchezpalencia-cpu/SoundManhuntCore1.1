package com.soundmanhunt.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;

import java.util.*;

public class MatchSession {

    private final SoundManhuntPlugin plugin;
    private final UUID runnerId;
    private final Set<UUID> hunterIds = new LinkedHashSet<>();
    private final Map<String, Location> lastKnownByWorld = new HashMap<>();
    private boolean finished = false;

    public MatchSession(SoundManhuntPlugin plugin, UUID runnerId) {
        this.plugin = plugin;
        this.runnerId = runnerId;
    }

    public void addHunter(UUID hunterId) {
        hunterIds.add(hunterId);
    }

    public UUID getRunnerId() {
        return runnerId;
    }

    public boolean isHunter(UUID playerId) {
        return hunterIds.contains(playerId);
    }

    public Set<UUID> getAllParticipants() {
        Set<UUID> all = new LinkedHashSet<>();
        all.add(runnerId);
        all.addAll(hunterIds);
        return all;
    }

    public void start() {
        Player runner = Bukkit.getPlayer(runnerId);
        if (runner != null) {
            updateRunnerLocation(runner.getLocation());
            runner.sendMessage("§aEres el §lCORREDOR§a. ¡Sobrevive y derrota al Dragon!");
        }
        for (UUID hId : hunterIds) {
            Player hunter = Bukkit.getPlayer(hId);
            if (hunter != null) {
                giveCompass(hunter);
                hunter.sendMessage("§cEres §lCAZADOR§c. Usa tu brujula para rastrear al corredor.");
            }
        }
    }

    public void updateRunnerLocation(Location loc) {
        if (loc != null && loc.getWorld() != null) {
            lastKnownByWorld.put(loc.getWorld().getName(), loc.clone());
        }
    }

    public void giveCompass(Player hunter) {
        ItemStack compass = new ItemStack(Material.COMPASS, 1);
        CompassMeta meta = (CompassMeta) compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c§lBrujula Rastreadora");
            compass.setItemMeta(meta);
        }
        hunter.getInventory().addItem(compass);
        updateCompass(hunter);
    }

    public void updateCompass(Player hunter) {
        if (hunter == null || !hunter.isOnline()) return;
        Player runner = Bukkit.getPlayer(runnerId);
        if (runner != null && runner.isOnline()) {
            updateRunnerLocation(runner.getLocation());
            if (runner.getWorld().equals(hunter.getWorld())) {
                Location target = runner.getLocation().clone();
                setCompassTarget(hunter, target);
                int dist = (int) hunter.getLocation().distance(target);
                hunter.sendActionBar("§cCorredor a §e" + dist + "m");
                return;
            }
        }
        Location fallback = lastKnownByWorld.get(hunter.getWorld().getName());
        if (fallback != null) {
            setCompassTarget(hunter, fallback);
            hunter.sendActionBar("§eApuntando a ultima posicion conocida en este mundo.");
        } else {
            hunter.sendActionBar("§eCorredor en otra dimension.");
        }
    }

    private void setCompassTarget(Player hunter, Location target) {
        for (ItemStack item : hunter.getInventory().getContents()) {
            if (item != null && item.getType() == Material.COMPASS) {
                CompassMeta meta = (CompassMeta) item.getItemMeta();
                if (meta != null) {
                    meta.setLodestone(target);
                    meta.setLodestoneTracked(false);
                    item.setItemMeta(meta);
                }
            }
        }
    }

    public void finish(String reason) {
        if (finished) return;
        finished = true;
        plugin.getPartyManager().unregisterMatch(this);
        for (UUID id : getAllParticipants()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                p.sendMessage("§6[Manhunt] §e" + reason);
            }
        }
    }
}