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
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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

        Metrics metrics = new Metrics(this, 20835);

        checkForUpdates();
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

        //vyjimka kvuli infinitestew pluginu
        if (itemType == Material.RABBIT_STEW) {
            return;
        }

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
            player.sendMessage("Stack refilling is turned off.");
        } else {
            enabledPlayers.add(playerUUID.toString());
            player.sendMessage("Stack refilling is turned on.");
        }

        config.set("enabledForPlayers", enabledPlayers);
        saveConfig();
        return true;
    }

    private void checkForUpdates() {
        try {
            String pluginName = this.getDescription().getName();
            URL url = new URL("https://www.ashkiano.com/version_check.php?plugin=" + pluginName);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                String jsonResponse = response.toString();
                JSONObject jsonObject = new JSONObject(jsonResponse);
                if (jsonObject.has("error")) {
                    this.getLogger().warning("Error when checking for updates: " + jsonObject.getString("error"));
                } else {
                    String latestVersion = jsonObject.getString("latest_version");

                    String currentVersion = this.getDescription().getVersion();
                    if (currentVersion.equals(latestVersion)) {
                        this.getLogger().info("This plugin is up to date!");
                    } else {
                        this.getLogger().warning("There is a newer version (" + latestVersion + ") available! Please update!");
                    }
                }
            } else {
                this.getLogger().warning("Failed to check for updates. Response code: " + responseCode);
            }
        } catch (Exception e) {
            this.getLogger().warning("Failed to check for updates. Error: " + e.getMessage());
        }
    }
}