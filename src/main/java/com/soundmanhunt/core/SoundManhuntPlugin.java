package com.soundmanhunt.core;

import org.bukkit.plugin.java.JavaPlugin;

public class SoundManhuntPlugin extends JavaPlugin {

    private PartyManager partyManager;
    private ArenaManager arenaManager;

    @Override
    public void onEnable() {
        this.arenaManager = new ArenaManager(this);
        this.partyManager = new PartyManager(this);

        CommandHandler commandHandler = new CommandHandler(this, partyManager, arenaManager);
        if (getCommand("party") != null) {
            getCommand("party").setExecutor(commandHandler);
        }
        if (getCommand("manhunt") != null) {
            getCommand("manhunt").setExecutor(commandHandler);
        }
        if (getCommand("sala") != null) {
            getCommand("sala").setExecutor(commandHandler);
        }

        getServer().getPluginManager().registerEvents(new GameListener(this, partyManager, arenaManager), this);
        getLogger().info("SoundManhuntCore habilitado con salas manh1, manh2, manh3 y plataforma en el vacio.");
    }

    @Override
    public void onDisable() {
        if (partyManager != null) {
            partyManager.clearAll();
        }
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }
}