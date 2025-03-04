package kindly.klan;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public final class KindlyHelpPlugin extends JavaPlugin implements Listener {

    private File configFile;
    private FileConfiguration config;
    private Map<Player, Location> lastSafeLocation = new HashMap<>();
    private Map<Player, Long> teleportCooldown = new HashMap<>();
    private final List<CreditSection> creditSections = Arrays.asList(
        new CreditSection("&6Proyecto por", "&eKindly Klan"),
        new CreditSection("&6Powered by", "&eHolyHosting &f"),
        new CreditSection("&6Game Programmer", "&e@Warloise"),
        new CreditSection("&6Game Design", "&e@Javivi09"),
        new CreditSection("&6Level Design", "&e@Connaro"),
        new CreditSection("&6Modelaje 2D/3D", "&e@KazekiBB"),
        new CreditSection("&6Diseño Gráfico", "&e@Zhamarkii Studios")
    );

    // Nueva variable para rastrear jugadores con visibilidad desactivada
    private Set<UUID> hiddenPlayersToggle = new HashSet<>();

    // Agregar ServerNavigator como variable
    private ServerNavigator serverNavigator;

    // Agregar JumpPadManager como campo
    private JumpPadManager jumpPadManager;
    
    // Agregar BossBarManager como campo
    private BossBarManager bossBarManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        configFile = new File(getDataFolder(), "config.yml");
        createConfig(); // Crear configuración antes de cargarla
        config = YamlConfiguration.loadConfiguration(configFile);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new LobbyItemsListener(this), this);

        // Inicializar ServerNavigator
        serverNavigator = new ServerNavigator(this);
        getServer().getPluginManager().registerEvents(new ServerNavigatorListener(this, serverNavigator), this);

        // Inicializar JumpPadManager
        jumpPadManager = new JumpPadManager(this);
        getServer().getPluginManager().registerEvents(jumpPadManager, this);
        
        // Inicializar BossBarManager
        bossBarManager = new BossBarManager(this);

        // Registrar canales de BungeeCord para el navegador de servidores
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        getCommand("kindlyhelp").setExecutor(new KindlyHelpCommandExecutor(this));
        getCommand("kindlyhelp").setTabCompleter(new KindlyHelpTabCompleter());

        // Registrar comando de JumpPad
        JumpPadCommandExecutor jumpPadCommandExecutor = new JumpPadCommandExecutor(this, jumpPadManager);
        getCommand("jumppad").setExecutor(jumpPadCommandExecutor);
        getCommand("jumppad").setTabCompleter(jumpPadCommandExecutor);

        // Schedule sponsor message every 15 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                sendSponsorMessage();
            }
        }.runTaskTimer(this, 0, 18000); // 18000 ticks = 15 minutes

        getLogger().info("KindlyHelp plugin habilitado ^^");
    }

    @Override
    public void onDisable() {
        if (config != null) {
            savePluginConfig();
        }

        // Guardar configuración de JumpPads
        if (jumpPadManager != null) {
            jumpPadManager.saveJumpPadConfig();
        }
        
        // Detener sistema de BossBar
        if (bossBarManager != null) {
            bossBarManager.shutdown();
        }
    }

    private void createConfig() {
        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Establecer valores por defecto en la configuración
        setDefaultConfigValue("joinMessage", "&e%player% se unió");
        setDefaultConfigValue("quitMessage", "&e%player% salió");
        setDefaultConfigValue("spawnLocation", null);
        setDefaultConfigValue("voidSpawnEnabled", false);
        setDefaultConfigValue("spawnTeleport.enabled", true);
        setDefaultConfigValue("spawnTeleport.onlyFirstTime", true);
        
        // Configuración para mensajes de patrocinador
        setDefaultConfigValue("sponsor.enabled", true);
        setDefaultConfigValue("sponsor.interval", 900); // segundos (15 minutos)
        setDefaultConfigValue("sponsor.title", "&6Patrocinador");
        setDefaultConfigValue("sponsor.description", "&eVisita nuestro patrocinador en www.example.com");
        
        // Configuración para mensaje de bienvenida con ASCII art
        setDefaultConfigValue("welcomeMessage.enabled", true);
        setDefaultConfigValue("welcomeMessage.title", "&6¡Bienvenido a Nuestro Servidor!");
        setDefaultConfigValue("welcomeMessage.subtitle", "&e¡Esperamos que te diviertas!");
        setDefaultConfigValue("welcomeMessage.showAsciiArt", true);
        setDefaultConfigValue("welcomeMessage.asciiArtColor", "&a");
        
        setDefaultConfigValue("chatFormat", "%player%&7: &f%message%");

        // Configuración para el sistema de visibilidad de jugadores
        setDefaultConfigValue("playerVisibility.enabled", true);
        setDefaultConfigValue("playerVisibility.toggleItem", "CLOCK");
        setDefaultConfigValue("playerVisibility.showPlayersName", "&aMostrar jugadores");
        setDefaultConfigValue("playerVisibility.hidePlayersName", "&cOcultar jugadores");
        setDefaultConfigValue("lobbyItemsEnabled", true);

        // Configuración para el navegador de servidores
        setDefaultConfigValue("navigator.enabled", true);
        setDefaultConfigValue("navigator.title", "&8Navegador de Servidores");
        setDefaultConfigValue("navigator.item", "COMPASS");
        setDefaultConfigValue("navigator.itemName", "&bServidores");
        setDefaultConfigValue("navigator.itemSlot", 4);
        setDefaultConfigValue("navigator.rows", 6); // Cantidad de filas en el inventario
        setDefaultConfigValue("navigator.useGlassBorder", true); // Usar borde de cristal
        setDefaultConfigValue("navigator.borderColor", "BLACK"); // Color del borde
        setDefaultConfigValue("navigator.showCloseButton", true); // Mostrar botón de cierre
        setDefaultConfigValue("navigator.showInfoButton", true); // Mostrar botón de información

        // Configuración de servidores de ejemplo
        if (!config.contains("navigator.servers.lobby")) {
            config.set("navigator.servers.lobby.name", "&aLobby");
            config.set("navigator.servers.lobby.description", "&7El lobby principal\\n&7¡Diviértete!");
            config.set("navigator.servers.lobby.address", "lobby");
            config.set("navigator.servers.lobby.icon", "NETHER_STAR");
            config.set("navigator.servers.lobby.slot", 11);
        }
        if (!config.contains("navigator.servers.survival")) {
            config.set("navigator.servers.survival.name", "&eSurvival");
            config.set("navigator.servers.survival.description", "&7Juega en modo supervivencia\\n&7¡Construye y explora!");
            config.set("navigator.servers.survival.address", "survival");
            config.set("navigator.servers.survival.icon", "GRASS_BLOCK");
            config.set("navigator.servers.survival.slot", 13);
        }
        if (!config.contains("navigator.servers.skyblock")) {
            config.set("navigator.servers.skyblock.name", "&bSkyBlock");
            config.set("navigator.servers.skyblock.description", "&7Comienza en una isla flotante\\n&7¡Sobrevive y prospera!");
            config.set("navigator.servers.skyblock.address", "skyblock");
            config.set("navigator.servers.skyblock.icon", "DIAMOND");
            config.set("navigator.servers.skyblock.slot", 15);
        }
        
        // Configuración para el sistema de BossBars
        setDefaultConfigValue("bossbars.enabled", true);
        setDefaultConfigValue("bossbars.showProgress", true);
        
        if (!config.contains("bossbars.messages.welcome")) {
            config.set("bossbars.messages.welcome.text", "&6Bienvenido al servidor");
            config.set("bossbars.messages.welcome.color", "YELLOW");
            config.set("bossbars.messages.welcome.style", "SOLID");
            config.set("bossbars.messages.welcome.duration", 60); // segundos
        }
        
        if (!config.contains("bossbars.messages.info")) {
            config.set("bossbars.messages.info.text", "&aUsa &e/kh navigator &apara abrir el selector de servidores");
            config.set("bossbars.messages.info.color", "GREEN");
            config.set("bossbars.messages.info.style", "SEGMENTED_6");
            config.set("bossbars.messages.info.duration", 45); // segundos
        }
        
        if (!config.contains("bossbars.messages.social")) {
            config.set("bossbars.messages.social.text", "&dVisita nuestra web: &fwww.ejemplo.com");
            config.set("bossbars.messages.social.color", "PINK");
            config.set("bossbars.messages.social.style", "SEGMENTED_10");
            config.set("bossbars.messages.social.duration", 50); // segundos
        }

        savePluginConfig();
    }

    // Método auxiliar para establecer valores por defecto en la configuración
    private void setDefaultConfigValue(String path, Object defaultValue) {
        if (!config.contains(path)) {
            config.set(path, defaultValue);
        }
    }

    public void sendSponsorMessage() {
        // Verificar si los mensajes de patrocinador están habilitados
        if (!config.getBoolean("sponsor.enabled", true)) {
            return;
        }
        
        String title = ChatColor.translateAlternateColorCodes('&', 
            config.getString("sponsor.title", "&6Patrocinador"));
        String description = ChatColor.translateAlternateColorCodes('&', 
            config.getString("sponsor.description", "&eVisita nuestro patrocinador en www.example.com"));
        description = description.replace("\\n", "\n");
        
        String message = String.format("%s\n%s", centerText(title), centerText(description));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }

    private String centerText(String text) {
        int maxWidth = 80; // Maximum width of the chat line
        int textWidth = ChatColor.stripColor(text).length();
        int padding = (maxWidth - textWidth) / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < padding; i++) {
            sb.append(" ");
        }
        sb.append(text);
        return sb.toString();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean isFirstJoin = !player.hasPlayedBefore();
        
        // Mensaje de join personalizado
        String joinMessage = ChatColor.translateAlternateColorCodes('&', config.getString("joinMessage", "&e%player% se unió"));
        event.setJoinMessage(joinMessage.replace("%player%", player.getName()));

        // Teletransportar al spawn según la configuración
        if (config.getBoolean("spawnTeleport.enabled", true)) {
            if (!config.getBoolean("spawnTeleport.onlyFirstTime", true) || isFirstJoin) {
                String worldName = config.getString("spawnLocation.world");
                if (worldName != null) {
                    Location spawnLocation = new Location(
                        Bukkit.getWorld(worldName),
                        config.getDouble("spawnLocation.x"),
                        config.getDouble("spawnLocation.y"),
                        config.getDouble("spawnLocation.z"),
                        (float) config.getDouble("spawnLocation.yaw"),
                        (float) config.getDouble("spawnLocation.pitch")
                    );
                    player.teleport(spawnLocation);
                }
            }
        }

        // Mostrar mensaje de bienvenida si está habilitado
        if (config.getBoolean("welcomeMessage.enabled", true)) {
            String title = ChatColor.translateAlternateColorCodes('&', 
                config.getString("welcomeMessage.title", "&6¡Bienvenido a Nuestro Servidor!"));
            String subtitle = ChatColor.translateAlternateColorCodes('&', 
                config.getString("welcomeMessage.subtitle", "&e¡Esperamos que te diviertas!"));
            
            player.sendTitle(title, subtitle, 10, 70, 20);
            
            // Mostrar arte ASCII si está habilitado
            if (config.getBoolean("welcomeMessage.showAsciiArt", true)) {
                showPlayerAsciiArt(player);
            }
        }

        // Dar ítems de lobby si está habilitado
        if (config.getBoolean("lobbyItemsEnabled", true)) {
            giveLobbyItems(player);
        }

        // Ocultar jugadores que tienen el toggle activado
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (hiddenPlayersToggle.contains(onlinePlayer.getUniqueId())) {
                onlinePlayer.hidePlayer(this, player);
            }
        }
        
        // Añadir jugador a la BossBar
        if (bossBarManager != null) {
            bossBarManager.addPlayer(player);
        }
    }
    
    /**
     * Muestra un arte ASCII representando la cabeza del jugador
     * @param player El jugador para mostrar el arte
     */
    private void showPlayerAsciiArt(Player player) {
        String artColor = ChatColor.translateAlternateColorCodes('&', 
            config.getString("welcomeMessage.asciiArtColor", "&a"));
        
        // Obtener la URL de la skin del jugador
        String skinUrl = "https://crafatar.com/avatars/" + player.getUniqueId() + "?overlay";
        
        // Descargar la imagen de la skin
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                BufferedImage skinImage = ImageIO.read(new URL(skinUrl));
                
                // Convertir la imagen a arte ASCII
                String[] asciiArt = convertSkinToAsciiArt(skinImage, artColor);
                
                // Enviar el ASCII art al jugador
                Bukkit.getScheduler().runTask(this, () -> {
                    player.sendMessage(" ");
                    for (String line : asciiArt) {
                        player.sendMessage(centerText(line));
                    }
                    
                    // Enviar nombre del jugador bajo el arte
                    String nameDisplay = artColor + "◄ " + ChatColor.GOLD + player.getName() + artColor + " ►";
                    player.sendMessage(centerText(nameDisplay));
                    player.sendMessage(" ");
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Convierte una imagen de skin a arte ASCII
     * @param skinImage La imagen de la skin
     * @param artColor El color principal del arte ASCII
     * @return Un array de strings representando el arte ASCII
     */
    private String[] convertSkinToAsciiArt(BufferedImage skinImage, String artColor) {
        // Caracteres ASCII ordenados por intensidad (de menos a más denso)
        final String[] ASCII_CHARS = {" ", ".", ":", "-", "=", "+", "*", "#", "%", "@"};
        
        // Dimensiones del arte ASCII (ajustar según sea necesario)
        int asciiWidth = 12;
        int asciiHeight = 6;
        
        // Tamaño original de la imagen de la skin
        int width = skinImage.getWidth();
        int height = skinImage.getHeight();
        
        // Relación de aspecto para mantener proporciones
        double ratio = (double) width / height;
        asciiWidth = (int) (asciiHeight * ratio * 2);
        
        // Redimensionar si es necesario para que quepa en el chat
        if (asciiWidth > 20) asciiWidth = 20;
        
        String[] result = new String[asciiHeight];
        
        for (int y = 0; y < asciiHeight; y++) {
            StringBuilder line = new StringBuilder();
            line.append("  "); // Padding inicial
            
            for (int x = 0; x < asciiWidth; x++) {
                // Mapear coordenadas ASCII a coordenadas de imagen
                int imgX = x * width / asciiWidth;
                int imgY = y * height / asciiHeight;
                
                // Obtener color del pixel
                int pixel = skinImage.getRGB(imgX, imgY);
                
                // Extraer componentes RGB
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = pixel & 0xff;
                int alpha = (pixel >> 24) & 0xff;
                
                // Calcular brillo (0-255)
                int brightness = (red + green + blue) / 3;
                
                // Si el pixel es transparente, usar espacio
                if (alpha < 128) {
                    line.append(" ");
                } else {
                    // Mapear brillo a índice de caracteres ASCII
                    int charIndex = brightness * (ASCII_CHARS.length - 1) / 255;
                    
                    // Determinar el color del caracter basado en el pixel
                    ChatColor pixelColor = getClosestChatColor(red, green, blue);
                    
                    // Agregar el caracter coloreado
                    line.append(pixelColor).append(ASCII_CHARS[charIndex]);
                }
            }
            
            result[y] = line.toString();
        }
        
        return result;
    }
    
    /**
     * Determina el color de chat más cercano a un color RGB
     * @param r Componente rojo (0-255)
     * @param g Componente verde (0-255)
     * @param b Componente azul (0-255)
     * @return El ChatColor más cercano al color RGB dado
     */
    private ChatColor getClosestChatColor(int r, int g, int b) {
        // Tabla de colores RGB aproximados para ChatColor
        final ChatColor[] COLORS = {
            ChatColor.BLACK,       // 0, 0, 0
            ChatColor.DARK_BLUE,   // 0, 0, 170
            ChatColor.DARK_GREEN,  // 0, 170, 0
            ChatColor.DARK_AQUA,   // 0, 170, 170
            ChatColor.DARK_RED,    // 170, 0, 0
            ChatColor.DARK_PURPLE, // 170, 0, 170
            ChatColor.GOLD,        // 255, 170, 0
            ChatColor.GRAY,        // 170, 170, 170
            ChatColor.DARK_GRAY,   // 85, 85, 85
            ChatColor.BLUE,        // 85, 85, 255
            ChatColor.GREEN,       // 85, 255, 85
            ChatColor.AQUA,        // 85, 255, 255
            ChatColor.RED,         // 255, 85, 85
            ChatColor.LIGHT_PURPLE,// 255, 85, 255
            ChatColor.YELLOW,      // 255, 255, 85
            ChatColor.WHITE        // 255, 255, 255
        };
        
        final int[][] COLOR_RGB = {
            {0, 0, 0},       // BLACK
            {0, 0, 170},     // DARK_BLUE
            {0, 170, 0},     // DARK_GREEN
            {0, 170, 170},   // DARK_AQUA
            {170, 0, 0},     // DARK_RED
            {170, 0, 170},   // DARK_PURPLE
            {255, 170, 0},   // GOLD
            {170, 170, 170}, // GRAY
            {85, 85, 85},    // DARK_GRAY
            {85, 85, 255},   // BLUE
            {85, 255, 85},   // GREEN
            {85, 255, 255},  // AQUA
            {255, 85, 85},   // RED
            {255, 85, 255},  // LIGHT_PURPLE
            {255, 255, 85},  // YELLOW
            {255, 255, 255}  // WHITE
        };
        
        ChatColor closestColor = ChatColor.WHITE;
        int minDistance = Integer.MAX_VALUE;
        
        for (int i = 0; i < COLORS.length; i++) {
            int dr = r - COLOR_RGB[i][0];
            int dg = g - COLOR_RGB[i][1];
            int db = b - COLOR_RGB[i][2];
            
            // Distancia euclídea al cuadrado
            int distance = dr*dr + dg*dg + db*db;
            
            if (distance < minDistance) {
                minDistance = distance;
                closestColor = COLORS[i];
            }
        }
        
        return closestColor;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Mensaje de quit personalizado
        String quitMessage = ChatColor.translateAlternateColorCodes('&', config.getString("quitMessage", "&e%player% salió"));
        event.setQuitMessage(quitMessage.replace("%player%", player.getName()));
        
        // Limpiar datos del jugador
        lastSafeLocation.remove(player);
        teleportCooldown.remove(player);
        
        // Eliminar jugador de la BossBar
        if (bossBarManager != null) {
            bossBarManager.removePlayer(player);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (config.getBoolean("voidSpawnEnabled")) {
            Location to = event.getTo();
            if (to.getY() < 0) {
                Location lastLocation = lastSafeLocation.get(player);
                if (lastLocation != null) {
                    player.teleport(lastLocation);
                    player.sendMessage(ChatColor.RED + "Donde vas? Tranqui, te he salvado la vida.");
                    teleportCooldown.put(player, System.currentTimeMillis());
                }
            } else if (to.getBlock().getRelative(0, -1, 0).getType().isSolid()) {
                lastSafeLocation.put(player, to);
            }
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (teleportCooldown.containsKey(player)) {
            long cooldownTime = teleportCooldown.get(player);
            if (System.currentTimeMillis() - cooldownTime < 5000) { // 5 seconds cooldown
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "No puedes teletransportarte tan rápido!");
            } else {
                teleportCooldown.remove(player);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL && teleportCooldown.containsKey(player)) {
                event.setCancelled(true);
                teleportCooldown.remove(player);
            }
        }
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        // Verificar si es parte de la configuración de un JumpPad
        if (jumpPadManager.handleChatMessage(player, message) || jumpPadManager.handleMaterialMessage(player, message)) {
            event.setCancelled(true);
            return;
        }
        
        // Si no es para un JumpPad, procesar normalmente
        String format = ChatColor.translateAlternateColorCodes('&', config.getString("chatFormat", "%player%&7: &f%message%"));
        format = format.replace("%player%", player.getDisplayName()).replace("%message%", "%2$s");
        event.setFormat(format);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !config.getBoolean("playerVisibility.enabled", true)) return;

        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            // Verificar si es el ítem de toggle de visibilidad
            try {
                Material toggleMaterial = Material.valueOf(config.getString("playerVisibility.toggleItem", "CLOCK"));
                if (item.getType() == toggleMaterial && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    event.setCancelled(true);
                    togglePlayerVisibility(player);
                }
            } catch (IllegalArgumentException e) {
                getLogger().warning("Material de ítem de visibilidad no válido en config.yml");
            }
        }
    }

    public void togglePlayerVisibility(Player player) {
        boolean currentlyHidden = hiddenPlayersToggle.contains(player.getUniqueId());

        if (currentlyHidden) {
            // Mostrar jugadores
            hiddenPlayersToggle.remove(player.getUniqueId());
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                player.showPlayer(this, onlinePlayer);
            }
            player.sendMessage(ChatColor.GREEN + "¡Jugadores visibles!");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        } else {
            // Ocultar jugadores
            hiddenPlayersToggle.add(player.getUniqueId());
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.equals(player)) {
                    player.hidePlayer(this, onlinePlayer);
                }
            }
            player.sendMessage(ChatColor.RED + "¡Jugadores ocultos!");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f);
        }

        // Actualizar el ítem en la mano del jugador
        try {
            Material toggleMaterial = Material.valueOf(config.getString("playerVisibility.toggleItem", "CLOCK"));
            ItemStack toggleItem = new ItemStack(toggleMaterial, 1);
            ItemMeta meta = toggleItem.getItemMeta();

            String itemName = currentlyHidden ? 
                ChatColor.translateAlternateColorCodes('&', config.getString("playerVisibility.hidePlayersName", "&cOcultar jugadores")) :
                ChatColor.translateAlternateColorCodes('&', config.getString("playerVisibility.showPlayersName", "&aMostrar jugadores"));

            meta.setDisplayName(itemName);
            toggleItem.setItemMeta(meta);

            player.getInventory().setItem(8, toggleItem);
        } catch (IllegalArgumentException e) {
            getLogger().warning("Material de ítem de visibilidad no válido en config.yml");
        }
    }

    /**
     * Entrega los ítems de lobby a un jugador
     * @param player El jugador que recibirá los ítems
     */
    public void giveLobbyItems(Player player) {
        // Limpiar inventario del jugador
        player.getInventory().clear();
        
        // Si la visibilidad de jugadores está habilitada, dar el ítem de toggle
        if (config.getBoolean("playerVisibility.enabled", true)) {
            try {
                Material toggleMaterial = Material.valueOf(config.getString("playerVisibility.toggleItem", "CLOCK"));
                ItemStack toggleItem = new ItemStack(toggleMaterial, 1);
                ItemMeta meta = toggleItem.getItemMeta();

                boolean playerHidden = hiddenPlayersToggle.contains(player.getUniqueId());
                String itemName = playerHidden ? 
                    ChatColor.translateAlternateColorCodes('&', config.getString("playerVisibility.showPlayersName", "&aMostrar jugadores")) :
                    ChatColor.translateAlternateColorCodes('&', config.getString("playerVisibility.hidePlayersName", "&cOcultar jugadores"));

                meta.setDisplayName(itemName);
                toggleItem.setItemMeta(meta);

                player.getInventory().setItem(8, toggleItem);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Material de ítem de visibilidad no válido en config.yml");
            }
        }
        
        // Si el navegador de servidores está habilitado, dar el ítem
        if (config.getBoolean("navigator.enabled", true)) {
            try {
                Material navigatorMaterial = Material.valueOf(config.getString("navigator.item", "COMPASS"));
                ItemStack navigatorItem = new ItemStack(navigatorMaterial, 1);
                ItemMeta meta = navigatorItem.getItemMeta();
                
                String navigatorName = ChatColor.translateAlternateColorCodes('&', 
                    config.getString("navigator.itemName", "&bServidores"));
                
                meta.setDisplayName(navigatorName);
                navigatorItem.setItemMeta(meta);
                
                int slot = config.getInt("navigator.itemSlot", 4);
                player.getInventory().setItem(slot, navigatorItem);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Material de ítem del navegador no válido en config.yml");
            }
        }
    }

    public void reloadPluginConfig() {
        // Recargar configuración principal
        if (configFile != null) {
            config = YamlConfiguration.loadConfiguration(configFile);
            getLogger().info("Configuración del plugin recargada.");
        }
        
        // Recargar configuración de JumpPads
        if (jumpPadManager != null) {
            jumpPadManager.loadJumpPadConfig();
            jumpPadManager.startParticleEffects();
            getLogger().info("Configuración de JumpPads recargada.");
        }
        
        // Recargar configuración de BossBars
        if (bossBarManager != null) {
            bossBarManager.reload();
            getLogger().info("Configuración de BossBars recargada.");
        }
    }

    public void savePluginConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            getLogger().severe("No se pudo guardar la configuración: " + e.getMessage());
        }
    }

    public FileConfiguration getPluginConfig() {
        return config;
    }

    public void showCredits(Player target) {
        new BukkitRunnable() {
            int index = 0;
            int fadeIn = 10;
            int stay = 40;
            int fadeOut = 10;
            
            @Override
            public void run() {
                if (index >= creditSections.size()) {
                    this.cancel();
                    return;
                }
                
                CreditSection section = creditSections.get(index);
                String title = ChatColor.translateAlternateColorCodes('&', section.title);
                String subtitle = ChatColor.translateAlternateColorCodes('&', section.subtitle);
                
                target.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
                target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                
                index++;
            }
        }.runTaskTimer(this, 0L, 60L); // 60L = 3 seconds between each credit section
    }

    private static class CreditSection {
        String title;
        String subtitle;
        
        CreditSection(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    public ServerNavigator getServerNavigator() {
        return serverNavigator;
    }

    public JumpPadManager getJumpPadManager() {
        return jumpPadManager;
    }
    
    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }
}