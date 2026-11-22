package com.soundmanhunt.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class ArenaManager {

    private final SoundManhuntPlugin plugin;
    private final Map<String, ArenaRoom> arenas = new LinkedHashMap<>();

    public ArenaManager(SoundManhuntPlugin plugin) {
        this.plugin = plugin;
        initDefaultArenas();
    }

    private void initDefaultArenas() {
        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        if (world == null) {
            return;
        }
        arenas.put("manh1", new ArenaRoom("manh1", new Location(world, 0.5, 102.0, 0.5)));
        arenas.put("manh2", new ArenaRoom("manh2", new Location(world, 500.5, 102.0, 0.5)));
        arenas.put("manh3", new ArenaRoom("manh3", new Location(world, 1000.5, 102.0, 0.5)));
    }

    public Map<String, ArenaRoom> getArenas() {
        return arenas;
    }

    public ArenaRoom getArena(String name) {
        if (name == null) {
            return null;
        }
        return arenas.get(name.toLowerCase());
    }

    public ArenaRoom findFreeArena() {
        for (ArenaRoom room : arenas.values()) {
            if (!room.isBusy()) {
                return room;
            }
        }
        return null;
    }

    public void buildVoidPlatform(Location center, int radius) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        int baseY = center.getBlockY() - 2;
        int cx = center.getBlockX();
        int cz = center.getBlockZ();

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                Block floor = world.getBlockAt(x, baseY, z);
                boolean border = (Math.abs(x - cx) == radius || Math.abs(z - cz) == radius);
                floor.setType(border ? Material.GLOWSTONE : Material.GLASS, false);

                if (border) {
                    Block rail = world.getBlockAt(x, baseY + 1, z);
                    rail.setType(Material.GLASS_PANE, false);
                }
            }
        }
    }

    public void setBarrier(Location center, int radius, int height, boolean active) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        int startY = center.getBlockY() - 1;
        int cx = center.getBlockX();
        int cz = center.getBlockZ();

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                boolean perimeter = (Math.abs(x - cx) == radius || Math.abs(z - cz) == radius);
                if (!perimeter) {
                    continue;
                }
                for (int y = startY; y < startY + height; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (active) {
                        if (block.getType() == Material.AIR || block.getType() == Material.GLASS_PANE) {
                            block.setType(Material.BARRIER, false);
                        }
                    } else if (block.getType() == Material.BARRIER) {
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }
    }

    public void sendToVoidPlatform(Player player, String arenaName) {
        ArenaRoom room = getArena(arenaName);
        if (room == null) {
            player.sendMessage("§cLa sala §e" + arenaName + " §cno existe. Usa: §fmanh1§c, §fmanh2§c, §fmanh3§c.");
            return;
        }
        Location spawn = room.getSpawn();
        buildVoidPlatform(spawn, 6);
        setBarrier(spawn, 6, 4, true);
        player.teleport(spawn);
        player.setFallDistance(0f);
        player.sendMessage("§aEntraste a §e" + room.getName() + " §acon plataforma y barrera activa en espera.");
    }

    public static class ArenaRoom {
        private final String name;
        private final Location spawn;
        private UUID activePartyLeader;

        public ArenaRoom(String name, Location spawn) {
            this.name = name;
            this.spawn = spawn;
        }

        public String getName() {
            return name;
        }

        public Location getSpawn() {
            return spawn.clone();
        }

        public boolean isBusy() {
            return activePartyLeader != null;
        }

        public UUID getActivePartyLeader() {
            return activePartyLeader;
        }

        public void setActivePartyLeader(UUID activePartyLeader) {
            this.activePartyLeader = activePartyLeader;
        }
    }
}