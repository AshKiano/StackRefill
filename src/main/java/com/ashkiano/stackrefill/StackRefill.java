package com.ashkiano.stackrefill;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

public class StackRefill extends JavaPlugin implements Listener, CommandExecutor {

    private String permission;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        permission = getConfig().getString("permission", "stackrefill.use");
        getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("stackrefill").setExecutor(this);

    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (shouldRefill(event.getPlayer())) {
            refillStack(event.getPlayer().getInventory(), event.getItemInHand().getType());
        }
    }

    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        if (shouldRefill(event.getPlayer())) {
            refillStack(event.getPlayer().getInventory(), event.getItem().getType());
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getItem() != null && event.getItem().getType() == Material.FIREWORK_ROCKET) {
            if (shouldRefill(event.getPlayer())) {
                refillStack(event.getPlayer().getInventory(), Material.FIREWORK_ROCKET);
            }
        }
    }

    private boolean shouldRefill(Player player) {
        if (!player.hasPermission(permission)) {
            return false;
        }
        FileConfiguration config = getConfig();
        List<String> enabledPlayers = config.getStringList("enabledForPlayers");
        return enabledPlayers.contains(player.getUniqueId().toString());
    }

    private void refillStack(PlayerInventory inventory, Material itemType) {
        for (int i = 0; i < 9; i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType() != itemType) {
                continue;
            }

            if (item.getAmount() == 1) {
                // Zvláštní zpracování pro jídlo
                if (itemType.isEdible()) {
                    for (int j = 9; j < 36; j++) {
                        ItemStack stack = inventory.getItem(j);
                        if (stack != null && stack.getType().isEdible() && stack.getType() != itemType) {
                            inventory.setItem(i, stack);
                            inventory.clear(j);
                            return;
                        }
                    }
                } else {
                    // Standardní zpracování pro ostatní materiály
                    for (int j = 9; j < 36; j++) {
                        ItemStack stack = inventory.getItem(j);
                        if (stack != null && stack.getType() == itemType) {
                            inventory.setItem(i, stack);
                            inventory.clear(j);
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission(permission)) {
            player.sendMessage("You do not have permission to use this command.");
            return true;
        }

        FileConfiguration config = getConfig();
        List<String> enabledPlayers = config.getStringList("enabledForPlayers");
        UUID playerUUID = player.getUniqueId();

        if (enabledPlayers.contains(playerUUID.toString())) {
            enabledPlayers.remove(playerUUID.toString());
            player.sendMessage("Stack refill feature is now disabled.");
        } else {
            enabledPlayers.add(playerUUID.toString());
            player.sendMessage("Stack refill feature is now enabled.");
        }

        config.set("enabledForPlayers", enabledPlayers);
        saveConfig();
        return true;
    }
}