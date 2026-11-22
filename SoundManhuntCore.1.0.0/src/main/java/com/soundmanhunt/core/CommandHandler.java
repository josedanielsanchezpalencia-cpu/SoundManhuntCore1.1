package com.soundmanhunt.core;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {

    private final SoundManhuntPlugin plugin;
    private final PartyManager partyManager;
    private final ArenaManager arenaManager;

    public CommandHandler(SoundManhuntPlugin plugin, PartyManager partyManager, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.partyManager = partyManager;
        this.arenaManager = arenaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando solo puede usarse dentro del juego.");
            return true;
        }

        String cmd = command.getName().toLowerCase();

        if (cmd.equals("sala")) {
            if (args.length < 1) {
                player.sendMessage("§eUso: §f/sala <manh1|manh2|manh3>");
                return true;
            }
            String targetRoom = args[0].toLowerCase();
            if (!targetRoom.equals("manh1") && !targetRoom.equals("manh2") && !targetRoom.equals("manh3")) {
                player.sendMessage("§cSala inválida. Usa: §fmanh1§c, §fmanh2§c o §fmanh3§c.");
                return true;
            }
            partyManager.assignArena(player.getUniqueId(), targetRoom);
            arenaManager.sendToVoidPlatform(player, targetRoom);
            return true;
        }

        if (cmd.equals("party")) {
            if (args.length == 0) {
                player.sendMessage("§eUso: §f/party <crear|invitar|iniciar|salir> [jugador]");
                return true;
            }
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "crear" -> partyManager.createParty(player);
                case "invitar" -> {
                    if (args.length < 2) {
                        player.sendMessage("§eUso: §f/party invitar <jugador>");
                        return true;
                    }
                    Player target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) {
                        player.sendMessage("§cEse jugador no está conectado.");
                        return true;
                    }
                    partyManager.invitePlayer(player, target);
                }
                case "iniciar" -> partyManager.startMatch(player);
                case "salir" -> partyManager.leaveParty(player);
                default -> player.sendMessage("§eSubcomando desconocido. Usa: §fcrear, invitar, iniciar, salir");
            }
            return true;
        }

        if (cmd.equals("manhunt")) {
            if (args.length == 0) {
                player.sendMessage("§eUso: §f/manhunt <sala|iniciar|detener> [manh1|manh2|manh3]");
                return true;
            }
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "sala" -> {
                    if (args.length < 2) {
                        player.sendMessage("§eUso: §f/manhunt sala <manh1|manh2|manh3>");
                        return true;
                    }
                    String room = args[1].toLowerCase();
                    partyManager.assignArena(player.getUniqueId(), room);
                    arenaManager.sendToVoidPlatform(player, room);
                }
                case "iniciar" -> partyManager.startMatch(player);
                case "detener" -> partyManager.stopMatch(player);
                default -> player.sendMessage("§eUso: §f/manhunt <sala|iniciar|detener> [manh1|manh2|manh3]");
            }
            return true;
        }

        return false;
    }
}