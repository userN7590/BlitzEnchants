# Blitz Enchants 🔮

A custom enchantment system designed for the Blitz Raids Minecraft server, focused on rune-based progression. Built with extensibility and performance in mind, this plugin uses clean Java OOP principles, custom event handling, GUI logic, and configuration-driven behavior.

## 📦 Features

- 🔹 **Rune Fusion System**  
  Players combine 5 runes of equal rarity to forge enchantments. Each rune contains custom metadata tracked via `PersistentDataContainer`.

- 🌟 **Tiered Enchantments**  
  Supports multiple rarity tiers (Basic, Advanced, Legendary), each unlocking more powerful enchantments. Tier logic is handled using enums and factory design patterns.

- 🧪 **Custom GUI (Inventory API)**  
  Fully dynamic 6-row inventory GUI using `BukkitRunnable` and `InventoryClickEvent` to manage slot states and crafting logic.

- ⚙️ **Asynchronous & Thread-Safe**  
  Heavy operations such as config loading and rune validation use async tasks to avoid main thread lag.

- 📁 **YAML-Based Config System**  
  Enchantments, crafting rules, and GUI titles are defined in external `.yml` config files, making it easy to expand without touching the code.

## 🧠 Technical Highlights

### ✅ Java Concepts Used

| Concept                     | Application                                                                 |
|----------------------------|-----------------------------------------------------------------------------|
| Object-Oriented Programming| Used for enchantment/rune class hierarchy and event separation              |
| Enums                      | Used for enchantment rarity levels and crafting outcomes                    |
| Interfaces                 | Modularize crafting logic via `EnchantmentCraftHandler` interface           |
| Event Handling             | Custom listener for GUI events and crafting triggers                        |
| Data Persistence           | `PersistentDataContainer` for storing custom rune metadata on ItemStacks    |
| Reflection (Optional)      | (If applicable) Used for dynamic enchantment registration or mapping        |
| Dependency Injection       | Clean instantiation of services (e.g., GUIManager, RuneRegistry)            |

### 🔧 Example: Rune Metadata System

```java
NamespacedKey rarityKey = new NamespacedKey(this, "rarity");
ItemMeta meta = item.getItemMeta();
meta.getPersistentDataContainer().set(rarityKey, PersistentDataType.STRING, "ADVANCED");
item.setItemMeta(meta);
