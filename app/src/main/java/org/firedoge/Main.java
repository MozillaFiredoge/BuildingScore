package org.firedoge;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.firedoge.managers.Initializer;
import org.firedoge.utils.ConfigUtils;

public class Main extends JavaPlugin {

    public boolean isInDebugMode = false;

    private static Main instance;


    @Override
    public void onEnable() {
        getLogger().info("Plugin Enabled");
        // Register event listeners and commands here
        instance = this;
        ConfigUtils configUtils = new ConfigUtils();
        configUtils.loadTiers();
        saveDefaultConfig();
        PluginManager pm = getServer().getPluginManager();
        Initializer.init(pm, this);
    }
    public static Main getPlugin() {
        return instance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("buildingscore") || command.getName().equalsIgnoreCase("bs")) {
            if(args.length > 0 && args[0].equalsIgnoreCase("reload")){
                reloadConfig();
                sender.sendMessage("Configuration reloaded!");
            }
            if(args.length >= 1 && args[0].equalsIgnoreCase("debug")){
                if(args.length == 2 && args[1].equalsIgnoreCase("true")){
                    isInDebugMode = true;
                } else if(args.length == 2 && args[1].equalsIgnoreCase("false")){
                    isInDebugMode = false;
                } else {
                    sender.sendMessage("Usage: /buildingscore debug <true|false>");
                    return true;
                }
                sender.sendMessage("[BuildingScore] Debug info:" + " isActivated=" + isInDebugMode);
            }
            return true;
        }
        return super.onCommand(sender, command, label, args);
    }

}