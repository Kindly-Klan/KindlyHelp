package kindly.klan;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ServerNavigator {

    private KindlyHelpPlugin plugin;
    private String inventoryTitle;

    public ServerNavigator(KindlyHelpPlugin plugin) {
        this.plugin = plugin;
        this.inventoryTitle = ChatColor.translateAlternateColorCodes('&', 
            plugin.getPluginConfig().getString("navigator.title", "&8Navegador de Servidores"));
    }

    /**
     * Abre el navegador de servidores para un jugador
     * @param player El jugador para quien abrir el navegador
     */
    public void openNavigator(Player player) {
        ConfigurationSection serversSection = plugin.getPluginConfig().getConfigurationSection("navigator.servers");
        if (serversSection == null) {
            player.sendMessage(ChatColor.RED + "No hay servidores configurados.");
            return;
        }

        // Obtener configuración de diseño
        int rows = plugin.getPluginConfig().getInt("navigator.rows", 6);
        boolean useGlassPane = plugin.getPluginConfig().getBoolean("navigator.useGlassBorder", true);
        String borderColor = plugin.getPluginConfig().getString("navigator.borderColor", "BLACK");
        
        // Crear inventario más grande (máximo 6 filas)
        rows = Math.min(6, Math.max(3, rows)); // Entre 3 y 6 filas
        Inventory inventory = Bukkit.createInventory(null, rows * 9, inventoryTitle);
        
        // Añadir bordes de cristal si está activado
        if (useGlassPane) {
            Material glassMaterial;
            try {
                glassMaterial = Material.valueOf(borderColor + "_STAINED_GLASS_PANE");
            } catch (IllegalArgumentException e) {
                glassMaterial = Material.BLACK_STAINED_GLASS_PANE;
            }
            
            ItemStack glass = new ItemStack(glassMaterial);
            ItemMeta glassMeta = glass.getItemMeta();
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
            
            // Primera y última fila completas
            for (int i = 0; i < 9; i++) {
                inventory.setItem(i, glass.clone());
                inventory.setItem((rows - 1) * 9 + i, glass.clone());
            }
            
            // Bordes laterales
            for (int i = 1; i < rows - 1; i++) {
                inventory.setItem(i * 9, glass.clone());
                inventory.setItem(i * 9 + 8, glass.clone());
            }
        }

        // Añadir servidores
        for (String serverKey : serversSection.getKeys(false)) {
            ConfigurationSection serverSection = serversSection.getConfigurationSection(serverKey);
            if (serverSection != null) {
                ItemStack serverItem = createServerItem(serverSection, serverKey);
                int slot = serverSection.getInt("slot", -1);
                
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, serverItem);
                } else {
                    inventory.addItem(serverItem);
                }
            }
        }
        
        // Añadir botón de cierre
        if (plugin.getPluginConfig().getBoolean("navigator.showCloseButton", true)) {
            ItemStack closeButton = new ItemStack(Material.BARRIER);
            ItemMeta closeMeta = closeButton.getItemMeta();
            closeMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', 
                plugin.getPluginConfig().getString("navigator.closeButtonText", "&cCerrar")));
            closeButton.setItemMeta(closeMeta);
            
            int closeSlot = plugin.getPluginConfig().getInt("navigator.closeButtonSlot", (rows * 9) - 5);
            if (closeSlot >= 0 && closeSlot < inventory.getSize()) {
                inventory.setItem(closeSlot, closeButton);
            }
        }
        
        // Añadir botón de información
        if (plugin.getPluginConfig().getBoolean("navigator.showInfoButton", true)) {
            ItemStack infoButton = new ItemStack(Material.BOOK);
            ItemMeta infoMeta = infoButton.getItemMeta();
            infoMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', 
                plugin.getPluginConfig().getString("navigator.infoButtonText", "&eInformación")));
            
            List<String> infoLore = new ArrayList<>();
            List<String> configLore = plugin.getPluginConfig().getStringList("navigator.infoButtonLore");
            if (configLore.isEmpty()) {
                infoLore.add(ChatColor.GRAY + "Selecciona un servidor para conectarte");
                infoLore.add(ChatColor.GRAY + "Haz clic en una opción para unirte");
            } else {
                for (String line : configLore) {
                    infoLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
            }
            
            infoMeta.setLore(infoLore);
            infoButton.setItemMeta(infoMeta);
            
            int infoSlot = plugin.getPluginConfig().getInt("navigator.infoButtonSlot", 4);
            if (infoSlot >= 0 && infoSlot < inventory.getSize()) {
                inventory.setItem(infoSlot, infoButton);
            }
        }

        player.openInventory(inventory);
    }
    
    /**
     * Crea un item para representar un servidor en el navegador
     * @param serverSection Sección de configuración del servidor
     * @param serverKey Clave única del servidor
     * @return ItemStack configurado para el servidor
     */
    private ItemStack createServerItem(ConfigurationSection serverSection, String serverKey) {
        String serverName = serverSection.getString("name", "Server");
        String serverDescription = serverSection.getString("description", "");
        String serverAddress = serverSection.getString("address", "");
        String iconType = serverSection.getString("iconType", "MATERIAL").toUpperCase();
        
        ItemStack item;
        
        // Determinar el tipo de ícono (material normal o cabeza)
        if (iconType.equals("HEAD") || iconType.equals("SKULL")) {
            String skullOwner = serverSection.getString("skullOwner", "Steve");
            item = createCustomSkull(skullOwner);
        } else if (iconType.equals("CUSTOM_HEAD") || iconType.equals("TEXTURE")) {
            String textureValue = serverSection.getString("textureValue", "");
            if (!textureValue.isEmpty()) {
                item = createTexturedSkull(textureValue);
            } else {
                item = new ItemStack(Material.PLAYER_HEAD);
            }
        } else {
            // Material normal
            String materialName = serverSection.getString("icon", "GRASS_BLOCK");
            try {
                Material material = Material.valueOf(materialName.toUpperCase());
                item = new ItemStack(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Material no válido para el servidor " + serverKey + ": " + materialName);
                item = new ItemStack(Material.GRASS_BLOCK); // Material por defecto
            }
        }
        
        // Configurar metadatos del ítem
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', serverName));
        
        // Configurar lore (descripción)
        List<String> lore = new ArrayList<>();
        
        // Añadir jugadores online si está configurado
        if (serverSection.getBoolean("showOnlinePlayers", false)) {
            int onlinePlayers = serverSection.getInt("onlinePlayers", 0);
            int maxPlayers = serverSection.getInt("maxPlayers", 100);
            lore.add(ChatColor.translateAlternateColorCodes('&', 
                serverSection.getString("onlinePlayersFormat", "&7Jugadores: &e%online%/%max%")
                    .replace("%online%", String.valueOf(onlinePlayers))
                    .replace("%max%", String.valueOf(maxPlayers))
            ));
        }
        
        // Añadir descripción
        for (String line : serverDescription.split("\\\\n")) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        
        // Añadir estado del servidor si está configurado
        if (serverSection.contains("status")) {
            String status = serverSection.getString("status", "online");
            String statusText;
            
            switch (status.toLowerCase()) {
                case "online":
                    statusText = ChatColor.translateAlternateColorCodes('&', 
                        serverSection.getString("statusOnline", "&aEn línea"));
                    break;
                case "offline":
                    statusText = ChatColor.translateAlternateColorCodes('&', 
                        serverSection.getString("statusOffline", "&cFuera de línea"));
                    break;
                case "maintenance":
                    statusText = ChatColor.translateAlternateColorCodes('&', 
                        serverSection.getString("statusMaintenance", "&6En mantenimiento"));
                    break;
                default:
                    statusText = ChatColor.translateAlternateColorCodes('&', status);
                    break;
            }
            
            lore.add(" ");
            lore.add(statusText);
        }
        
        // Añadir mensaje de conexión
        lore.add(" ");
        lore.add(ChatColor.translateAlternateColorCodes('&', 
            serverSection.getString("connectMessage", "&eHaz clic para conectar")));
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Crea una cabeza con el skin de un jugador específico
     * @param playerName Nombre del jugador
     * @return ItemStack de la cabeza
     */
    private ItemStack createCustomSkull(String playerName) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (playerName != null && !playerName.isEmpty()) {
            meta.setOwner(playerName);
        }
        skull.setItemMeta(meta);
        return skull;
    }
    
    /**
     * Crea una cabeza con una textura personalizada basada en un valor Base64
     * @param textureValue Valor Base64 de la textura
     * @return ItemStack de la cabeza personalizada
     */
    private ItemStack createTexturedSkull(String textureValue) {
        // Implementación básica para cabezas con texturas personalizadas
        // Para una implementación completa, se necesitaría reflection o librerías adicionales
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        return skull;
    }
    
    /**
     * Conecta a un jugador a un servidor específico
     * @param player El jugador a conectar
     * @param serverName El nombre del servidor en BungeeCord
     */
    public void connectToServer(Player player, String serverName) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            
            player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
            
            // Mensaje personalizado de conexión
            String connectMessage = ChatColor.translateAlternateColorCodes('&', 
                plugin.getPluginConfig().getString("navigator.connectMessage", 
                    "&aConectando a &e%server%&a...")
                    .replace("%server%", serverName));
            
            player.sendMessage(connectMessage);
        } catch (IOException e) {
            player.sendMessage(ChatColor.RED + "Error al conectar con el servidor.");
            e.printStackTrace();
        }
    }
    
    /**
     * Verifica si un inventario es el navegador de servidores
     * @param title El título del inventario a verificar
     * @return true si es el navegador de servidores
     */
    public boolean isNavigatorInventory(String title) {
        return title.equals(inventoryTitle);
    }
    
    /**
     * Obtiene el nombre del servidor asociado a un ítem en el inventario
     * @param item El ítem a verificar
     * @return El nombre del servidor o null si no se encuentra
     */
    public String getServerNameFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return null;
        }
        
        // Si es un botón de cierre, devolver una señal especial
        if (item.getType() == Material.BARRIER && 
            item.getItemMeta().getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', 
            plugin.getPluginConfig().getString("navigator.closeButtonText", "&cCerrar")))) {
            return "CLOSE_MENU";
        }
        
        // Si es un botón de información, ignorarlo
        if (item.getType() == Material.BOOK && 
            item.getItemMeta().getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', 
            plugin.getPluginConfig().getString("navigator.infoButtonText", "&eInformación")))) {
            return null;
        }
        
        String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        ConfigurationSection serversSection = plugin.getPluginConfig().getConfigurationSection("navigator.servers");
        
        if (serversSection == null) {
            return null;
        }
        
        for (String serverKey : serversSection.getKeys(false)) {
            ConfigurationSection serverSection = serversSection.getConfigurationSection(serverKey);
            if (serverSection != null) {
                String serverName = ChatColor.stripColor(
                    ChatColor.translateAlternateColorCodes('&', 
                    serverSection.getString("name", "Server")));
                
                if (serverName.equals(displayName)) {
                    return serverSection.getString("address", "");
                }
            }
        }
        
        return null;
    }
}