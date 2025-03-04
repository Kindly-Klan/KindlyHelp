package kindly.klan;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.block.Action;
import org.bukkit.util.Vector;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

public class JumpPadManager implements Listener {

    private KindlyHelpPlugin plugin;
    private File jumpPadFile;
    private FileConfiguration jumpPadConfig;
    
    // Mapa para rastrear jugadores en el modo de configuración
    private Map<UUID, JumpPadSetupSession> setupSessions = new HashMap<>();
    
    // Para evitar procesamiento repetido de JumpPads
    private Set<UUID> recentlyLaunched = new HashSet<>();
    
    // Tarea para mostrar partículas
    private BukkitTask particleTask;

    public JumpPadManager(KindlyHelpPlugin plugin) {
        this.plugin = plugin;
        loadJumpPadConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startParticleEffects();
    }
    
    /**
     * Inicia la tarea de mostrar partículas en los JumpPads
     */
    public void startParticleEffects() {
        // Cancelar tarea anterior si existe
        if (particleTask != null) {
            particleTask.cancel();
        }
        
        // Crear nueva tarea para mostrar partículas
        particleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ConfigurationSection jumpPadsSection = jumpPadConfig.getConfigurationSection("jumppads");
            if (jumpPadsSection == null) return;
            
            for (String id : jumpPadsSection.getKeys(false)) {
                ConfigurationSection padSection = jumpPadsSection.getConfigurationSection(id);
                if (padSection == null) continue;
                
                // Obtener ubicación del JumpPad
                String worldName = padSection.getString("world");
                if (worldName == null) continue;
                
                org.bukkit.World world = Bukkit.getWorld(worldName);
                if (world == null) continue;
                
                int x = padSection.getInt("x");
                int y = padSection.getInt("y");
                int z = padSection.getInt("z");
                
                // Mostrar partículas sobre el JumpPad
                Location particleLoc = new Location(world, x + 0.5, y + 1.2, z + 0.5);
                
                // Tipo de partícula basado en la potencia del JumpPad
                double power = padSection.getDouble("power", 1.5);
                Particle particleType;
                
                if (power > 4) {
                    particleType = Particle.FLAME;
                } else if (power > 2) {
                    particleType = Particle.VILLAGER_HAPPY;
                } else {
                    particleType = Particle.END_ROD;
                }
                
                // Mostrar espiral de partículas
                double radius = 0.5;
                for (int i = 0; i < 8; i++) {
                    double angle = Math.PI * 2 * i / 8;
                    double x1 = particleLoc.getX() + radius * Math.cos(angle);
                    double z1 = particleLoc.getZ() + radius * Math.sin(angle);
                    Location loc = new Location(world, x1, particleLoc.getY() + (i * 0.05), z1);
                    world.spawnParticle(particleType, loc, 1, 0, 0, 0, 0);
                }
            }
        }, 0L, 10L); // Ejecutar cada 10 ticks (0.5 segundo)
    }
    
    /**
     * Carga la configuración de JumpPads desde un archivo
     */
    public void loadJumpPadConfig() {
        jumpPadFile = new File(plugin.getDataFolder(), "jumppads.yml");
        if (!jumpPadFile.exists()) {
            try {
                jumpPadFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("No se pudo crear el archivo jumppads.yml: " + e.getMessage());
            }
        }
        jumpPadConfig = YamlConfiguration.loadConfiguration(jumpPadFile);
    }
    
    /**
     * Guarda la configuración de los JumpPads
     */
    public void saveJumpPadConfig() {
        try {
            jumpPadConfig.save(jumpPadFile);
        } catch (IOException e) {
            plugin.getLogger().severe("No se pudo guardar el archivo jumppads.yml: " + e.getMessage());
        }
    }
    
    /**
     * Inicia una sesión de configuración de JumpPad para un jugador
     * @param player El jugador iniciando la configuración
     * @param id El ID del JumpPad a configurar
     */
    public void startJumpPadSetup(Player player, String id) {
        if (setupSessions.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Ya estás configurando un JumpPad. Usa /jumppad cancel para cancelar.");
            return;
        }
        
        setupSessions.put(player.getUniqueId(), new JumpPadSetupSession(id));
        player.sendMessage(ChatColor.GREEN + "Configuración de JumpPad iniciada con ID: " + id);
        player.sendMessage(ChatColor.GOLD + "Paso 1: " + ChatColor.WHITE + "Haz clic derecho en el bloque que servirá como JumpPad.");
    }
    
    /**
     * Cancela una sesión de configuración de JumpPad
     * @param player El jugador cuya sesión se cancelará
     */
    public void cancelJumpPadSetup(Player player) {
        if (setupSessions.remove(player.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + "Configuración de JumpPad cancelada.");
        } else {
            player.sendMessage(ChatColor.RED + "No estás configurando ningún JumpPad.");
        }
    }
    
    /**
     * Maneja el evento de interacción cuando un jugador está configurando un JumpPad
     * @param event El evento de interacción
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        JumpPadSetupSession session = setupSessions.get(player.getUniqueId());
        
        // Si el jugador está en una sesión de configuración y hace clic con el botón derecho en un bloque
        if (session != null && event.getAction() == Action.RIGHT_CLICK_BLOCK && event.hasBlock()) {
            event.setCancelled(true);
            Block clickedBlock = event.getClickedBlock();
            
            // Paso 1: Seleccionar ubicación del JumpPad
            if (session.step == 1) {
                session.padLocation = clickedBlock.getLocation();
                session.step = 2; // Cambiar al siguiente paso correctamente
                player.sendMessage(ChatColor.GREEN + "Ubicación del JumpPad establecida en: " + 
                    formatLocation(session.padLocation));
                player.sendMessage(ChatColor.GOLD + "Paso 2: " + ChatColor.WHITE + 
                    "Haz clic derecho en el bloque al que quieres que el JumpPad lleve al jugador.");
                
                // Mostrar partículas para indicar la selección
                player.spawnParticle(Particle.VILLAGER_HAPPY, 
                    session.padLocation.clone().add(0.5, 1.0, 0.5), 
                    20, 0.5, 0.5, 0.5, 0);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                return;
            } 
            // Paso 2: Seleccionar ubicación de destino (solo si ya pasó por el paso 1)
            else if (session.step == 2) {
                // Verificar que no sea el mismo bloque que el JumpPad
                if (isSameLocation(clickedBlock.getLocation(), session.padLocation)) {
                    player.sendMessage(ChatColor.RED + "No puedes seleccionar el mismo bloque como destino. Elige otro bloque.");
                    return;
                }
                
                session.targetLocation = clickedBlock.getLocation().add(0.5, 1.0, 0.5);
                session.step = 3;
                player.sendMessage(ChatColor.GREEN + "Ubicación de destino establecida en: " + 
                    formatLocation(session.targetLocation));
                player.sendMessage(ChatColor.GOLD + "Paso 3: " + ChatColor.WHITE + 
                    "Especifica la potencia del salto (recomendado: 1.0-3.0).");
                player.sendMessage(ChatColor.GRAY + "Escribe la potencia en el chat o escribe 'default' para usar 1.5");
                
                // Mostrar línea de partículas entre origen y destino
                drawParticleLine(player, 
                    session.padLocation.clone().add(0.5, 1.0, 0.5), 
                    session.targetLocation);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                return;
            }
        }

        // Verificar si el jugador pisó un JumpPad
        if (event.getAction() == Action.PHYSICAL || 
            ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) 
              && event.hasBlock() && session == null)) {
            
            // Solo procesar si el jugador no está en modo configuración
            if (session == null && event.hasBlock()) {
                checkAndLaunchJumpPad(player, event.getClickedBlock());
            }
        }
    }
    
    /**
     * Devuelve una representación formateada de una ubicación
     * @param loc La ubicación a formatear
     * @return La representación como String
     */
    private String formatLocation(Location loc) {
        return String.format("%s (%.1f, %.1f, %.1f)", 
            loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }
    
    /**
     * Comprueba si dos ubicaciones representan el mismo bloque
     * @param loc1 Primera ubicación
     * @param loc2 Segunda ubicación
     * @return true si son el mismo bloque
     */
    private boolean isSameLocation(Location loc1, Location loc2) {
        return loc1.getWorld().equals(loc2.getWorld()) &&
               loc1.getBlockX() == loc2.getBlockX() &&
               loc1.getBlockY() == loc2.getBlockY() &&
               loc1.getBlockZ() == loc2.getBlockZ();
    }
    
    /**
     * Dibuja una línea de partículas entre dos ubicaciones
     * @param player El jugador que verá las partículas
     * @param start La ubicación inicial
     * @param end La ubicación final
     */
    private void drawParticleLine(Player player, Location start, Location end) {
        Vector direction = end.toVector().subtract(start.toVector());
        double length = direction.length();
        direction.normalize();
        
        double step = 0.5; // Distancia entre partículas
        for (double i = 0; i < length; i += step) {
            Vector point = direction.clone().multiply(i);
            Location particleLoc = start.clone().add(point);
            player.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
        }
    }
    
    /**
     * Maneja el movimiento del jugador para detectar cuando pisa un JumpPad
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Solo procesar si el jugador realmente se movió a un nuevo bloque
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
            event.getFrom().getBlockY() != event.getTo().getBlockY() ||
            event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            
            Player player = event.getPlayer();
            
            // Evitar lanzar al jugador repetidas veces en un corto período
            if (recentlyLaunched.contains(player.getUniqueId())) {
                return;
            }
            
            Block standingBlock = event.getTo().getBlock().getRelative(0, -1, 0);
            checkAndLaunchJumpPad(player, standingBlock);
        }
    }
    
    /**
     * Verifica si el bloque es un JumpPad y lanza al jugador si es así
     * @param player El jugador a lanzar
     * @param block El bloque a comprobar
     */
    private void checkAndLaunchJumpPad(Player player, Block block) {
        if (block == null) return;
        
        // Evitar procesar si está en modo setup
        if (setupSessions.containsKey(player.getUniqueId())) return;
        
        ConfigurationSection jumpPadsSection = jumpPadConfig.getConfigurationSection("jumppads");
        if (jumpPadsSection == null) return;
        
        for (String id : jumpPadsSection.getKeys(false)) {
            ConfigurationSection padSection = jumpPadsSection.getConfigurationSection(id);
            if (padSection == null) continue;
            
            // Obtener ubicación del JumpPad
            String worldName = padSection.getString("world");
            int x = padSection.getInt("x");
            int y = padSection.getInt("y");
            int z = padSection.getInt("z");
            
            // Verificar si el bloque es el JumpPad
            if (block.getWorld().getName().equals(worldName) &&
                block.getX() == x && block.getY() == y && block.getZ() == z) {
                
                // Obtener ubicación objetivo
                String targetWorld = padSection.getString("targetWorld");
                double targetX = padSection.getDouble("targetX");
                double targetY = padSection.getDouble("targetY");
                double targetZ = padSection.getDouble("targetZ");
                
                launchPlayer(player, targetWorld, targetX, targetY, targetZ, padSection.getDouble("power", 1.5));
                
                // Agregar al conjunto de recientemente lanzados
                recentlyLaunched.add(player.getUniqueId());
                
                // Eliminar del conjunto después de un tiempo
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    recentlyLaunched.remove(player.getUniqueId());
                }, 20L); // 1 segundo de cooldown
                
                break;
            }
        }
    }
    
    /**
     * Lanza a un jugador desde un JumpPad hacia una ubicación objetivo
     * @param player El jugador a lanzar
     * @param targetWorld El mundo de destino
     * @param targetX Coordenada X de destino
     * @param targetY Coordenada Y de destino
     * @param targetZ Coordenada Z de destino
     * @param power La potencia del lanzamiento
     */
    private void launchPlayer(Player player, String targetWorld, double targetX, double targetY, double targetZ, double power) {
        // Si el mundo objetivo es diferente, teletransportar al jugador
        if (!player.getWorld().getName().equals(targetWorld)) {
            Location targetLoc = new Location(Bukkit.getWorld(targetWorld), targetX, targetY, targetZ);
            player.teleport(targetLoc);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            
            // Efecto visual en destino
            player.getWorld().spawnParticle(Particle.PORTAL, targetLoc, 50, 0.5, 0.5, 0.5, 0.1);
            return;
        }
        
        // Calcular el vector de lanzamiento
        Location playerLoc = player.getLocation();
        Vector direction = new Vector(targetX - playerLoc.getX(), 
                                      targetY - playerLoc.getY(), 
                                      targetZ - playerLoc.getZ());
        
        // Normalizar y aplicar potencia
        direction.normalize().multiply(power);
        
        // Añadir componente vertical para trayectoria parabólica
        if (direction.getY() < 0.5) {
            direction.setY(Math.max(0.5, direction.getY() + 0.5));
        }
        
        // Aplicar el vector al jugador
        player.setVelocity(direction);
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
        
        // Efectos visuales en lanzamiento
        player.getWorld().spawnParticle(Particle.CLOUD, playerLoc, 30, 0.2, 0.1, 0.2, 0.1);
        
        // Efectos visuales trazados a lo largo del camino
        traceLaunchPath(player, playerLoc, player.getLocation().add(direction.multiply(2)), power);
        
        // Prevenir daño de caída del jugador
        preventFallDamage(player);
    }
    
    /**
     * Traza partículas a lo largo de la trayectoria de lanzamiento
     * @param player El jugador lanzado
     * @param start La posición de inicio
     * @param direction La dirección de lanzamiento
     * @param power La potencia del lanzamiento
     */
    private void traceLaunchPath(Player player, Location start, Location end, double power) {
        new BukkitRunnable() {
            private int ticks = 0;
            private final int maxTicks = 20; // Duración de las partículas
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    this.cancel();
                    return;
                }
                
                // Calcular posición actual del jugador
                Location playerLoc = player.getLocation();
                
                // Mostrar partículas en la posición actual
                Particle particleType = power > 2.5 ? Particle.FLAME : Particle.END_ROD;
                player.getWorld().spawnParticle(particleType, 
                    playerLoc.clone().subtract(0, 0.5, 0), 
                    5, 0.1, 0.1, 0.1, 0.01);
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
    
    /**
     * Previene daño de caída temporalmente para un jugador
     * @param player El jugador a proteger
     */
    private void preventFallDamage(Player player) {
        // Programar la eliminación del flag de prevención de daño de caída
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.setFallDistance(0);
        }, 100L); // 5 segundos de protección
    }
    
    /**
     * Procesa un mensaje de chat para configuración de JumpPad
     * @param player El jugador que envió el mensaje
     * @param message El mensaje enviado
     * @return true si el mensaje fue manejado como parte de la configuración
     */
    public boolean handleChatMessage(Player player, String message) {
        JumpPadSetupSession session = setupSessions.get(player.getUniqueId());
        if (session == null || session.step != 3) {
            return false;
        }
        
        // Manejar el paso 3: Configurar la potencia
        try {
            double power;
            if (message.equalsIgnoreCase("default")) {
                power = 1.5;
            } else {
                power = Double.parseDouble(message);
                if (power <= 0 || power > 10.0) {
                    player.sendMessage(ChatColor.RED + "La potencia debe estar entre 0.1 y 10.0.");
                    return true;
                }
            }
            
            session.power = power;
            session.step++;
            
            // Preguntar por el material del JumpPad (paso 4)
            player.sendMessage(ChatColor.GREEN + "Potencia establecida en: " + power);
            player.sendMessage(ChatColor.GOLD + "Paso 4: " + ChatColor.WHITE + "Especifica el material del JumpPad.");
            player.sendMessage(ChatColor.GRAY + "Escribe el nombre del material o 'default' para usar EMERALD_BLOCK");
            
            return true;
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Por favor, ingresa un número válido o 'default'.");
            return true;
        }
    }
    
    /**
     * Procesa un mensaje de chat para el material del JumpPad
     * @param player El jugador que envió el mensaje
     * @param message El mensaje enviado
     * @return true si el mensaje fue manejado como parte de la configuración
     */
    public boolean handleMaterialMessage(Player player, String message) {
        JumpPadSetupSession session = setupSessions.get(player.getUniqueId());
        if (session == null || session.step != 4) {
            return false;
        }
        
        // Manejar el paso 4: Material del JumpPad
        Material material;
        if (message.equalsIgnoreCase("default")) {
            material = Material.EMERALD_BLOCK;
        } else {
            try {
                material = Material.valueOf(message.toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Material no válido. Escribe 'default' o un nombre de material válido.");
                player.sendMessage(ChatColor.GRAY + "Ejemplo: SLIME_BLOCK, GOLD_BLOCK, DIAMOND_BLOCK");
                return true;
            }
        }
        
        // Guardar el JumpPad y finalizar
        saveJumpPad(session, player, material);
        return true;
    }
    
    /**
     * Guarda un JumpPad en la configuración
     * @param session La sesión de configuración
     * @param player El jugador que configuró el JumpPad
     * @param material El material del JumpPad
     */
    private void saveJumpPad(JumpPadSetupSession session, Player player, Material material) {
        String id = session.id;
        Location padLoc = session.padLocation;
        Location targetLoc = session.targetLocation;
        
        ConfigurationSection jumpPadsSection = jumpPadConfig.getConfigurationSection("jumppads");
        if (jumpPadsSection == null) {
            jumpPadsSection = jumpPadConfig.createSection("jumppads");
        }
        
        // Eliminar configuración anterior si existía
        if (jumpPadsSection.contains(id)) {
            jumpPadsSection.set(id, null);
        }
        
        // Crear la nueva configuración
        ConfigurationSection padSection = jumpPadsSection.createSection(id);
        
        // Guardar ubicación del JumpPad
        padSection.set("world", padLoc.getWorld().getName());
        padSection.set("x", padLoc.getBlockX());
        padSection.set("y", padLoc.getBlockY());
        padSection.set("z", padLoc.getBlockZ());
        
        // Guardar ubicación de destino
        padSection.set("targetWorld", targetLoc.getWorld().getName());
        padSection.set("targetX", targetLoc.getX());
        padSection.set("targetY", targetLoc.getY());
        padSection.set("targetZ", targetLoc.getZ());
        
        // Guardar potencia
        padSection.set("power", session.power);
        
        // Guardar material
        padSection.set("material", material.name());
        
        // Guardar configuración
        saveJumpPadConfig();
        
        // Aplicar el material al bloque
        padLoc.getBlock().setType(material);
        
        // Finalizar sesión de configuración
        setupSessions.remove(player.getUniqueId());
        
        player.sendMessage(ChatColor.GREEN + "¡JumpPad configurado y activado correctamente!");
        player.sendMessage(ChatColor.GREEN + "ID: " + id + " | Material: " + material.name() + " | Potencia: " + session.power);
        
        // Efecto visual de confirmación
        player.getWorld().spawnParticle(Particle.TOTEM, 
            padLoc.clone().add(0.5, 1.5, 0.5), 
            50, 0.5, 0.5, 0.5, 0.1);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }
    
    /**
     * Elimina un JumpPad por su ID
     * @param id El ID del JumpPad a eliminar
     * @return true si se eliminó correctamente
     */
    public boolean removeJumpPad(String id) {
        ConfigurationSection jumpPadsSection = jumpPadConfig.getConfigurationSection("jumppads");
        if (jumpPadsSection == null || !jumpPadsSection.contains(id)) {
            return false;
        }
        
        jumpPadsSection.set(id, null);
        saveJumpPadConfig();
        return true;
    }
    
    /**
     * Obtiene una lista de todos los IDs de JumpPads
     * @return Array de IDs de JumpPads
     */
    public String[] getJumpPadIds() {
        ConfigurationSection jumpPadsSection = jumpPadConfig.getConfigurationSection("jumppads");
        if (jumpPadsSection == null) {
            return new String[0];
        }
        
        return jumpPadsSection.getKeys(false).toArray(new String[0]);
    }
    
    /**
     * Clase interna para manejar la sesión de configuración de un JumpPad
     */
    private class JumpPadSetupSession {
        String id;
        int step = 1;
        Location padLocation;
        Location targetLocation;
        double power = 1.5;
        
        public JumpPadSetupSession(String id) {
            this.id = id;
        }
    }
}