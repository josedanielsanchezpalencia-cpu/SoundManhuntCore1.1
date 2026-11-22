package com.soundmanhunt.core;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class GameListener implements Listener {

    private final SoundManhuntPlugin plugin;
    private final PartyManager partyManager;
    private final ArenaManager arenaManager;

    public GameListener(SoundManhuntPlugin plugin, PartyManager partyManager, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.partyManager = partyManager;
        this.arenaManager = arenaManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        if (to.getY() < 60.0) {
            MatchSession session = partyManager.getSessionByPlayer(player.getUniqueId());
            if (session == null || !session.isRunning()) {
                ArenaManager.ArenaRoom room = partyManager.getAssignedArena(player.getUniqueId());
                if (room == null) {
                    room = arenaManager.getArena("manh1");
                }
                if (room != null) {
                    arenaManager.buildVoidPlatform(room.getSpawn(), 6);
                    player.teleport(room.getSpawn());
                    player.setFallDistance(0f);
                    player.sendMessage("§e¡Caíste al vacío! Regresaste a la plataforma de §a" + room.getName() + "§e.");
                }
            }
        }
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL || event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            MatchSession session = partyManager.getSessionByPlayer(player.getUniqueId());
            if (session == null || !session.isRunning()) {
                event.setCancelled(true);
            }
        }
    }
}