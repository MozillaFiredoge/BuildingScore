package org.firedoge.managers;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.firedoge.listeners.BlockListener;

public class Initializer{
    
    public static void init(PluginManager pm, JavaPlugin plugin){
        // Register your event listeners here
        pm.registerEvents(new BlockListener(), plugin);
        DifficultyManager dm = new DifficultyManager();
        dm.run();
    }
}
