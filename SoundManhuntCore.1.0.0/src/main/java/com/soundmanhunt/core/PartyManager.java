package com.soundmanhunt.core;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class PartyManager {

    private final SoundManhuntPlugin plugin;
    private final Map<UUID, Party> partiesByLeader = new HashMap<>();
    private final Map<UUID, UUID> playerToLeader = new HashMap<>();
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();
    private final Map<UUID, MatchSession> activeMatchesByPlayer = new HashMap<>();

    public static class Party {
        private final UUID leader;
        private final Set<UUID> members = new LinkedHashSet<>();
        private int huntersCount = 1;

        public Party(UUID leader) {
            this.leader = leader;
            this.members.add(leader);
        }

        public UUID getLeader() {
            return leader;
        }

        public Set<UUID> getMembers() {
            return members;
        }

        public int getHuntersCount() {
            return huntersCount;
        }

        public void setHuntersCount(int huntersCount) {
            this.huntersCount = Math.max(1, Math.min(10, huntersCount));
        }
    }

    public PartyManager(SoundManhuntPlugin plugin) {
        this.plugin = plugin;
    }

    public Party getPartyOf(UUID playerId) {
        UUID leader = playerToLeader.get(playerId);
        return leader != null ? partiesByLeader.get(leader) : null;
    }

    public void createParty(Player leader) {
        if (getPartyOf(leader.getUniqueId()) != null) {
            leader.sendMessage("§cYa estas en una party.");
            return;
        }
        Party party = new Party(leader.getUniqueId());
        partiesByLeader.put(leader.getUniqueId(), party);
        playerToLeader.put(leader.getUniqueId(), leader.getUniqueId());
        leader.sendMessage("§aParty creada. Usa §e/party invitar <jugador> §ay §e/party modo <1-10>§a.");
    }

    public void invitePlayer(Player leader, Player target) {
        Party party = partiesByLeader.get(leader.getUniqueId());
        if (party == null) {
            leader.sendMessage("§cNo eres lider de ninguna party.");
            return;
        }
        if (getPartyOf(target.getUniqueId()) != null) {
            leader.sendMessage("§c" + target.getName() + " ya esta en una party.");
            return;
        }
        pendingInvites.put(target.getUniqueId(), leader.getUniqueId());
        leader.sendMessage("§aInvitacion enviada a §e" + target.getName() + "§a.");
        target.sendMessage("§aTe invitaron a la party de §e" + leader.getName() + "§a. Usa §e/party aceptar§a.");
    }

    public void acceptInvite(Player player) {
        UUID leaderId = pendingInvites.remove(player.getUniqueId());
        if (leaderId == null) {
            player.sendMessage("§cNo tienes invitaciones pendientes.");
            return;
        }
        Party party = partiesByLeader.get(leaderId);
        if (party == null) {
            player.sendMessage("§cLa party ya no existe.");
            return;
        }
        if (getPartyOf(player.getUniqueId()) != null) {
            player.sendMessage("§cYa estas en una party.");
            return;
        }
        party.getMembers().add(player.getUniqueId());
        playerToLeader.put(player.getUniqueId(), leaderId);
        for (UUID id : party.getMembers()) {
            Player member = Bukkit.getPlayer(id);
            if (member != null) {
                member.sendMessage("§e" + player.getName() + " §ase unio a la party.");
            }
        }
    }

    public void leaveParty(Player player) {
        UUID uuid = player.getUniqueId();
        Party party = getPartyOf(uuid);
        if (party == null) {
            player.sendMessage("§cNo estas en ninguna party.");
            return;
        }
        party.getMembers().remove(uuid);
        playerToLeader.remove(uuid);
        player.sendMessage("§eSaliste de la party.");

        if (party.getLeader().equals(uuid)) {
            partiesByLeader.remove(uuid);
            for (UUID memberId : new ArrayList<>(party.getMembers())) {
                playerToLeader.remove(memberId);
                Player m = Bukkit.getPlayer(memberId);
                if (m != null) {
                    m.sendMessage("§cEl lider disolvio la party.");
                }
            }
            party.getMembers().clear();
        }
    }

    public void setMode(Player leader, int hunters) {
        Party party = partiesByLeader.get(leader.getUniqueId());
        if (party == null) {
            leader.sendMessage("§cSolo el lider puede cambiar el modo.");
            return;
        }
        if (hunters < 1 || hunters > 10) {
            leader.sendMessage("§cUsa un numero entre 1 y 10.");
            return;
        }
        party.setHuntersCount(hunters);
        leader.sendMessage("§aModo configurado a §e1vs" + hunters + "§a.");
    }

    public void startMatch(Player leader) {
        Party party = partiesByLeader.get(leader.getUniqueId());
        if (party == null) {
            leader.sendMessage("§cCrea una party primero con §e/party crear§c.");
            return;
        }
        List<Player> online = new ArrayList<>();
        for (UUID id : party.getMembers()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                online.add(p);
            }
        }
        if (online.size() < 2) {
            leader.sendMessage("§cNecesitas al menos 2 jugadores conectados.");
            return;
        }

        Collections.shuffle(online);
        Player runner = online.get(0);
        int huntersLimit = Math.min(party.getHuntersCount(), online.size() - 1);
        List<Player> hunters = new ArrayList<>(online.subList(1, 1 + huntersLimit));

        MatchSession session = new MatchSession(plugin, runner.getUniqueId());
        for (Player h : hunters) {
            session.addHunter(h.getUniqueId());
        }

        for (UUID participantId : session.getAllParticipants()) {
            activeMatchesByPlayer.put(participantId, session);
        }

        session.start();
    }

    public MatchSession getMatch(UUID playerId) {
        return activeMatchesByPlayer.get(playerId);
    }

    public void unregisterMatch(MatchSession session) {
        if (session == null) return;
        for (UUID id : session.getAllParticipants()) {
            activeMatchesByPlayer.remove(id);
        }
    }

    public void endAllMatches(String reason) {
        for (MatchSession session : new HashSet<>(activeMatchesByPlayer.values())) {
            session.finish(reason);
        }
        activeMatchesByPlayer.clear();
    }
}