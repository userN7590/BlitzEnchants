package com.fil.blitzenchants;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class Main extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private final Map<String, Enchantment> enchantmentMap = new HashMap<>();
    private final Map<String, Integer> enchantmentPriority = new HashMap<>();
    private final Set<String> validEnchantments = new HashSet<>();
    private final String CATALYST_NAME = ChatColor.DARK_PURPLE + "Catalyst";
    private final String STAR_INDICATOR = ChatColor.WHITE + " [⭐]";
    private final Map<Player, ItemStack> catalystTargets = new HashMap<>();

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        config = this.getConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("giveblitzbook").setExecutor(new GiveBlitzBookCommand());
        getCommand("blitzenchant").setExecutor(new BlitzEnchantCommand());
        getCommand("givecatalyst").setExecutor(new GiveCatalystCommand());
        initializeEnchantmentMap();
    }

    private void initializeEnchantmentMap() {
        for (Enchantment ench : Enchantment.values()) {
            String enchName = ench.getKey().getKey().toUpperCase();
            enchantmentMap.put(enchName, ench);
            validEnchantments.add(enchName);
            int priority = config.getInt("enchantments." + enchName.toLowerCase() + ".priority", 100);
            enchantmentPriority.put(enchName, priority);
        }
        Bukkit.getLogger().info("[BlitzEnchants] Loaded " + enchantmentMap.size() + " enchantments.");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack book = event.getCursor();
        ItemStack target = event.getCurrentItem();

        if (book == null || target == null || target.getType() == Material.AIR) return;

        // Enchantment Book Logic
        if (book.getType() == Material.BOOK && book.hasItemMeta() && book.getItemMeta().hasDisplayName()) {
            event.setCancelled(true);
            applyEnchantment(player, book, target);
        }

        // Catalyst Logic
        if (book.getType() == Material.NETHER_STAR && book.hasItemMeta() && book.getItemMeta().getDisplayName().equals(CATALYST_NAME)) {
            event.setCancelled(true);
            if (hasCatalystApplied(target)) {
                player.sendMessage(ChatColor.RED + "This item has already been enhanced with a Catalyst!");
                return;
            }
            if (target.getEnchantments().isEmpty()) {
                player.sendMessage(ChatColor.RED + "This item has no enchantments to upgrade!");
                return;
            }
            catalystTargets.put(player, target);
            openCatalystMenu(player, target);
            book.setAmount(book.getAmount() - 1); // Remove catalyst after use
            player.setItemOnCursor(null); // Ensure catalyst is removed from hand
        }
    }

    private void applyEnchantment(Player player, ItemStack book, ItemStack target) {
        String enchantmentName = ChatColor.stripColor(book.getItemMeta().getDisplayName()).replace("Enchantment Book | ", "");
        Enchantment enchantment = enchantmentMap.get(enchantmentName.toUpperCase());

        if (enchantment == null) {
            player.sendMessage(ChatColor.RED + "This enchantment does not exist!");
            return;
        }

        if (!enchantment.canEnchantItem(target)) {
            player.sendMessage(ChatColor.RED + "This enchantment cannot be applied to this item!");
            return;
        }

        int enchantLevel = config.getInt("enchantments." + enchantmentName.toLowerCase() + ".level", 1);
        target.addUnsafeEnchantment(enchantment, enchantLevel);
        updateItemLore(target);
        book.setAmount(book.getAmount() - 1);
        player.sendMessage(ChatColor.GREEN + "You have applied " + enchantmentName + ChatColor.GREEN + " to your item!");
    }

    private void updateItemLore(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        List<String> lore = new ArrayList<>();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        NamespacedKey catalystKey = new NamespacedKey(this, "catalyst_enchant");
        String boostedEnchant = data.get(catalystKey, PersistentDataType.STRING);

        for (String key : enchantmentPriority.keySet()) {
            if (item.containsEnchantment(enchantmentMap.get(key))) {
                String customLore = ChatColor.translateAlternateColorCodes('&',
                        config.getString("enchantments." + key.toLowerCase() + ".applied_lore", ChatColor.GRAY + key));
                if (boostedEnchant != null && boostedEnchant.equals(key)) {
                    customLore += STAR_INDICATOR;
                }
                lore.add(customLore);
            }
        }
        lore.sort(Comparator.comparingInt(o -> enchantmentPriority.getOrDefault(o.toUpperCase(), 100)));
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private boolean hasCatalystApplied(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer data = meta.getPersistentDataContainer();
        NamespacedKey catalystKey = new NamespacedKey(this, "catalyst_enchant");
        return data.has(catalystKey, PersistentDataType.STRING);
    }

    private void openCatalystMenu(Player player, ItemStack item) {
        Inventory menu = Bukkit.createInventory(player, 9, ChatColor.DARK_PURPLE + "Select Enchantment");
        for (Enchantment enchantment : item.getEnchantments().keySet()) {
            ItemStack enchItem = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta meta = enchItem.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + enchantment.getKey().getKey().replace("_", " ").toUpperCase());
            enchItem.setItemMeta(meta);
            menu.addItem(enchItem);
        }
        player.openInventory(menu);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        catalystTargets.remove(event.getPlayer());
    }

    @EventHandler
    public void onCatalystMenuClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.DARK_PURPLE + "Select Enchantment")) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;

            ItemStack target = catalystTargets.get(player);
            if (target == null) return;

            Enchantment selectedEnchant = enchantmentMap.get(ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).toUpperCase());
            if (selectedEnchant == null) return;

            int newLevel = target.getEnchantmentLevel(selectedEnchant) + 1;
            target.addUnsafeEnchantment(selectedEnchant, newLevel);
            ItemMeta meta = target.getItemMeta();
            PersistentDataContainer data = meta.getPersistentDataContainer();
            NamespacedKey catalystKey = new NamespacedKey(this, "catalyst_enchant");
            data.set(catalystKey, PersistentDataType.STRING, selectedEnchant.getKey().getKey().toUpperCase());
            target.setItemMeta(meta);
            updateItemLore(target);
            catalystTargets.remove(player);
            player.closeInventory();
            player.sendMessage(ChatColor.GREEN + "Catalyst successfully applied to " + selectedEnchant.getKey().getKey().replace("_", " ").toUpperCase());
        }
    }

    class GiveCatalystCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }
            Player player = (Player) sender;
            ItemStack catalyst = new ItemStack(Material.NETHER_STAR);
            ItemMeta meta = catalyst.getItemMeta();
            meta.setDisplayName(CATALYST_NAME);
            catalyst.setItemMeta(meta);
            player.getInventory().addItem(catalyst);
            player.sendMessage(ChatColor.GREEN + "You have received a " + CATALYST_NAME + ChatColor.GREEN + "!");
            return true;
        }
    }

    class BlitzEnchantCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                Main.this.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "BlitzEnchants configuration reloaded!");
                return true;
            }
            sender.sendMessage(ChatColor.RED + "Usage: /blitzenchant reload");
            return true;
        }
    }

    class GiveBlitzBookCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /giveblitzbook <enchantment> <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found!");
                return true;
            }

            String enchantmentName = args[0].toUpperCase();
            if (!validEnchantments.contains(enchantmentName)) {
                sender.sendMessage(ChatColor.RED + "Invalid enchantment! Available: " + validEnchantments);
                return true;
            }

            ItemStack book = new ItemStack(Material.BOOK);
            ItemMeta meta = book.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + "Enchantment Book | " + enchantmentName);
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Drag this onto an item to apply " + enchantmentName + "."));
            book.setItemMeta(meta);

            target.getInventory().addItem(book);
            sender.sendMessage(ChatColor.GREEN + "Given " + enchantmentName + " book to " + target.getName() + "!");
            return true;
        }
    }
}
