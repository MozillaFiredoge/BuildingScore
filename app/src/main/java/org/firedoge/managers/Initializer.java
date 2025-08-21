package org.firedoge.managers;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.firedoge.commands.ExportStructureCommand;
import org.firedoge.listeners.BlockListener;
import org.firedoge.listeners.MiniVillageGenerator;
import org.firedoge.listeners.SelectionListener;

public class Initializer{
    
    public static void init(PluginManager pm, JavaPlugin plugin) {
        plugin.getCommand("exportstructure").setExecutor(new ExportStructureCommand());
        pm.registerEvents(new BlockListener(), plugin);
        pm.registerEvents(new MiniVillageGenerator(), plugin);
        pm.registerEvents(new SelectionListener(), plugin);
        DifficultyManager dm = new DifficultyManager();
        dm.run();
    }
}

