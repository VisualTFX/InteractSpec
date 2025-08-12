package com.visualtfx.interactspec;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class InteractSpecCommand implements CommandExecutor {

    private final InteractSpec plugin;

    // Constructor to get the main plugin instance
    public InteractSpecCommand(InteractSpec plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the command was run by the console
        if (sender instanceof ConsoleCommandSender) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        // Check if the player has OP permissions
        if (!player.isOp()) {
            player.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        // Toggle the spec mode based on the player's current state
        if (plugin.isSpectating(player)) {
            plugin.disableSpecMode(player);
        } else {
            plugin.enableSpecMode(player);
        }

        return true;
    }
}