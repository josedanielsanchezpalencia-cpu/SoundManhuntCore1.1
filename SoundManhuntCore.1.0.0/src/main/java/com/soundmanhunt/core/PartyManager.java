package com.soundmanhunt.core;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PartyManager {

    private final SoundManhuntPlugin plugin;
    private final Map<UUID, Set<UUID>> parties = new HashMap<>();
    private final Map<UUID, UUID> playerToLeader = new HashMap<>();
    private final Map<UUID, String> playerArena = new HashMap<>();
    private final Map<UUID, MatchSession> activeSessions = new HashMap<>();

    public PartyManager(SoundManhuntPlugin plugin) {
        this.plugin = plugin;
    }

    public void createParty(Player leader) {
        UUID leaderId = leader.getUniqueId();
        if (playerToLeader.containsKey(leaderId)) {
            leader.sendMessage("§cYa perteneces a una party. Usa §e/party salir §cprimero.");
            return;
        }
        Set<UUID> members = new HashSet<>();
        members.add(leaderId);
        parties.put(leaderId, members);
        playerToLeader.put(leaderId, leaderId);
        leader.sendMessage("§aParty creada. Usa §e/party invitar <jugador> §ay §e/sala <manh1|manh2|manh3>§a.");
    }

    public void invitePlayer(Player leader, Player target) {
        UUID leaderId = leader.getUniqueId();
        if (!parties.containsKey(leaderId)) {
            createParty(leader);
        }
        UUID targetId = target.getUniqueId();
        if (playerToLeader.containsKey(targetId)) {
            leader.sendMessage("§c" + target.getName() + " ya está en una party.");
            return;
        }
        parties.get(leaderId).add(targetId);
        playerToLeader.put(targetId, leaderId);

        String assigned = playerArena.get(leaderId);
        if (assigned != null) {
            playerArena.put(targetId, assigned);
        }

        leader.sendMessage("§aAñadiste a §e" + target.getName() + " §aa tu party.");
        target.sendMessage("§aTe uniste a la party de §e" + leader.getName() + "§a.");
    }

    public void leaveParty(Player player) {
        UUID playerId = player.getUniqueId();
        UUID leaderId = playerToLeader.remove(playerId);
        playerArena.remove(playerId);

        if (leaderId == null) {
            player.sendMessage("§cNo estás en ninguna party.");
            return;
        }

        if (leaderId.equals(playerId)) {
            MatchSession session = activeSessions.remove(leaderId);
            if (session != null) {
                session.stop();
            }
            Set<UUID> members = parties.remove(leaderId);
            if (members != null) {
                for (UUID memberId : members) {
                    playerToLeader.remove(memberId);
                    playerArena.remove(memberId);
                    Player member = Bukkit.getPlayer(memberId);
                    if (member != null && !memberId.equals(playerId)) {
                        member.sendMessage("§cEl líder disolvió la party.");
                    }
                }
            }
            player.sendMessage("§eHas disuelto tu party.");
        } else {
            Set<UUID> members = parties.get(leaderId);
            if (members != null) {
                members.remove(playerId);
            }
            player.sendMessage("§eSaliste de la party.");
        }
    }

    public void assignArena(UUID playerId, String arenaName) {
        UUID leaderId = playerToLeader.getOrDefault(playerId, playerId);
        String normalized = arenaName.toLowerCase();
        playerArena.put(leaderId, normalized);

        Set<UUID> members = parties.get(leaderId);
        if (members != null) {
            for (UUID memberId : members) {
                playerArena.put(memberId, normalized);
            }
        }
    }

    public ArenaManager.ArenaRoom getAssignedArena(UUID playerId) {
        String arenaName = playerArena.get(playerId);
        if (arenaName == null) {
            UUID leaderId = playerToLeader.get(playerId);
            if (leaderId != null) {
                arenaName = playerArena.get(leaderId);
            }
        }
        return plugin.getArenaManager().getArena(arenaName);
    }

    public void startMatch(Player leader) {
        UUID leaderId = leader.getUniqueId();
        if (!parties.containsKey(leaderId)) {
            createParty(leader);
        }
        if (activeSessions.containsKey(leaderId)) {
            leader.sendMessage("§cYa tienes una partida activa. Usa §e/manhunt detener §cprimero.");
            return;
        }

        ArenaManager.ArenaRoom room = getAssignedArena(leaderId);
        if (room == null) {
            room = plugin.getArenaManager().findFreeArena();
        }
        if (room == null) {
            leader.sendMessage("§cNo hay salas libres en este momento (manh1, manh2, manh3 ocupadas).");
            return;
        }
        if (room.isBusy() && !leaderId.equals(room.getActivePartyLeader())) {
            leader.sendMessage("§cLa sala §e" + room.getName() + " §cestá ocupada. Elige otra con §e/sala <manh1|manh2|manh3>§c.");
            return;
        }

        room.setActivePartyLeader(leaderId);
        assignArena(leaderId, room.getName());

        List<UUID> members = new ArrayList<>(parties.get(leaderId));
        UUID runnerId = members.get(0);

        for (UUID memberId : members) {
            Player member = Bukkit.getPlayer(memberId);
            if (member == null || !member.isOnline()) {
                continue;
            }
            member.teleport(room.getSpawn());
            member.setFallDistance(0f);

            if (memberId.equals(runnerId)) {
                member.sendMessage("§a¡Eres el §lRUNNER§a! Corre al abrirse la barrera.");
            } else {
                member.sendMessage("§c¡Eres un §lCAZADOR§c! Espera a que caiga la barrera.");
                ItemStack compass = new ItemStack(Material.COMPASS);
                ItemMeta meta = compass.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§cBrújula Rastreadora");
                    compass.setItemMeta(meta);
                }
                member.getInventory().addItem(compass);
            }
        }

        MatchSession session = new MatchSession(plugin, plugin.getArenaManager(), room, runnerId);
        activeSessions.put(leaderId, session);
        session.start();
    }

    public void stopMatch(Player player) {
        UUID leaderId = playerToLeader.getOrDefault(player.getUniqueId(), player.getUniqueId());
        MatchSession session = activeSessions.remove(leaderId);
        if (session == null) {
            player.sendMessage("§cNo hay ninguna partida activa en tu party.");
            return;
        }
        session.stop();
        player.sendMessage("§ePartida detenida y sala §a" + session.getArena().getName() + " §eliberada.");
    }

    public MatchSession getSessionByPlayer(UUID playerId) {
        UUID leaderId = playerToLeader.getOrDefault(playerId, playerId);
        return activeSessions.get(leaderId);
    }

    public void clearAll() {
        for (MatchSession session : activeSessions.values()) {
            session.stop();
        }
        activeSessions.clear();
        parties.clear();
        playerToLeader.clear();
        playerArena.clear();
    }
}
