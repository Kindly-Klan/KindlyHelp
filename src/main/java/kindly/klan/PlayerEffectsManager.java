package kindly.klan;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.logging.Level;

public class PlayerEffectsManager {

    private KindlyHelpPlugin plugin;
    private Map<String, EffectInfo> configuredEffects;
    private boolean effectsEnabled;
    private BukkitTask effectsTask;
    private Set<UUID> disabledPlayers = new HashSet<>();

    public PlayerEffectsManager(KindlyHelpPlugin plugin) {
        this.plugin = plugin;
        this.configuredEffects = new HashMap<>();
        loadEffectsConfiguration();
        startEffectsTask();
    }

    /**
     * Carga la configuración de efectos desde config.yml
     */
    private void loadEffectsConfiguration() {
        configuredEffects.clear();
        
        // Verifica si los efectos están habilitados globalmente
        effectsEnabled = plugin.getPluginConfig().getBoolean("playerEffects.enabled", true);
        
        ConfigurationSection effectsSection = plugin.getPluginConfig().getConfigurationSection("playerEffects.effects");
        
        // Si no existe la sección de efectos, crea una por defecto
        if (effectsSection == null) {
            plugin.getLogger().info("No se encontró configuración de efectos para jugadores, creando valores por defecto.");
            
            ConfigurationSection mainSection = plugin.getPluginConfig().createSection("playerEffects");
            mainSection.set("enabled", true);
            mainSection.set("refreshRate", 20); // Ticks (1 segundo)
            
            ConfigurationSection effects = mainSection.createSection("effects");
            
            // Efecto de velocidad
            ConfigurationSection speedEffect = effects.createSection("speed");
            speedEffect.set("type", "SPEED");
            speedEffect.set("enabled", true);
            speedEffect.set("amplifier", 0); // Nivel 1
            speedEffect.set("particles", true);
            speedEffect.set("ambient", true);
            speedEffect.set("icon", true);
            
            // Efecto de visión nocturna
            ConfigurationSection nightVisionEffect = effects.createSection("nightvision");
            nightVisionEffect.set("type", "NIGHT_VISION");
            nightVisionEffect.set("enabled", false);
            nightVisionEffect.set("amplifier", 0);
            nightVisionEffect.set("particles", false);
            nightVisionEffect.set("ambient", true);
            nightVisionEffect.set("icon", true);
            
            // Efecto de salto
            ConfigurationSection jumpEffect = effects.createSection("jump");
            jumpEffect.set("type", "JUMP");
            jumpEffect.set("enabled", false);
            jumpEffect.set("amplifier", 1); // Nivel 2
            jumpEffect.set("particles", true);
            jumpEffect.set("ambient", true);
            jumpEffect.set("icon", true);
            
            // Efecto de resistencia al fuego
            ConfigurationSection fireResistEffect = effects.createSection("fireresist");
            fireResistEffect.set("type", "FIRE_RESISTANCE");
            fireResistEffect.set("enabled", false);
            fireResistEffect.set("amplifier", 0);
            fireResistEffect.set("particles", false);
            fireResistEffect.set("ambient", true);
            fireResistEffect.set("icon", true);
            
            // Efecto de respiración acuática
            ConfigurationSection waterBreathingEffect = effects.createSection("waterbreathing");
            waterBreathingEffect.set("type", "WATER_BREATHING");
            waterBreathingEffect.set("enabled", false);
            waterBreathingEffect.set("amplifier", 0);
            waterBreathingEffect.set("particles", false);
            waterBreathingEffect.set("ambient", true);
            waterBreathingEffect.set("icon", true);
            
            // Guardar la configuración
            plugin.savePluginConfig();
            effectsSection = effects;
        }
        
        // Cargar cada efecto configurado
        for (String key : effectsSection.getKeys(false)) {
            ConfigurationSection effectSection = effectsSection.getConfigurationSection(key);
            if (effectSection == null) continue;
            
            try {
                // Obtener el tipo de efecto
                String effectTypeName = effectSection.getString("type");
                if (effectTypeName == null) {
                    plugin.getLogger().warning("Tipo de efecto no especificado para " + key);
                    continue;
                }
                
                PotionEffectType effectType = PotionEffectType.getByName(effectTypeName);
                if (effectType == null) {
                    plugin.getLogger().warning("Tipo de efecto no válido: " + effectTypeName);
                    continue;
                }
                
                // Configuración del efecto
                boolean effectEnabled = effectSection.getBoolean("enabled", true);
                int amplifier = effectSection.getInt("amplifier", 0);
                boolean showParticles = effectSection.getBoolean("particles", true);
                boolean ambient = effectSection.getBoolean("ambient", true);
                boolean icon = effectSection.getBoolean("icon", true);
                
                // Crear objeto de información del efecto
                EffectInfo info = new EffectInfo(
                    effectType,
                    effectEnabled,
                    amplifier,
                    showParticles,
                    ambient,
                    icon
                );
                
                configuredEffects.put(key, info);
                plugin.getLogger().info("Efecto cargado: " + key + " (" + effectTypeName + ") - " + (effectEnabled ? "Activado" : "Desactivado"));
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error al cargar el efecto " + key, e);
            }
        }
    }
    
    /**
     * Inicia la tarea programada para aplicar efectos a los jugadores
     */
    private void startEffectsTask() {
        // Detener tarea anterior si existe
        stopEffectsTask();
        
        // Si los efectos están desactivados globalmente, no iniciar la tarea
        if (!effectsEnabled) {
            plugin.getLogger().info("Efectos de jugador desactivados globalmente");
            return;
        }
        
        // Obtener la tasa de actualización de los efectos (en ticks)
        int refreshRate = plugin.getPluginConfig().getInt("playerEffects.refreshRate", 20);
        refreshRate = Math.max(1, refreshRate); // Al menos 1 tick
        
        // Iniciar nueva tarea
        effectsTask = new BukkitRunnable() {
            @Override
            public void run() {
                applyEffectsToPlayers();
            }
        }.runTaskTimer(plugin, 20, refreshRate); // Esperar 1 segundo antes de la primera ejecución
    }
    
    /**
     * Detiene la tarea de efectos
     */
    private void stopEffectsTask() {
        if (effectsTask != null) {
            effectsTask.cancel();
            effectsTask = null;
        }
    }
    
    /**
     * Aplica los efectos a todos los jugadores online
     */
    private void applyEffectsToPlayers() {
        // Si no hay efectos habilitados, no hacer nada
        if (configuredEffects.isEmpty() || !effectsEnabled) {
            return;
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Saltar jugadores que tienen los efectos desactivados
            if (disabledPlayers.contains(player.getUniqueId())) {
                continue;
            }
            
            // Verificar permisos y aplicar cada efecto
            for (Map.Entry<String, EffectInfo> entry : configuredEffects.entrySet()) {
                String effectKey = entry.getKey();
                EffectInfo info = entry.getValue();
                
                // Verificar si el efecto está habilitado
                if (!info.isEnabled()) {
                    continue;
                }
                
                // Verificar permiso específico para este efecto
                if (!player.hasPermission("kindlyhelp.effects." + effectKey) && 
                    !player.hasPermission("kindlyhelp.effects.*") && 
                    !player.isOp()) {
                    continue;
                }
                
                // Aplicar el efecto al jugador
                player.addPotionEffect(new PotionEffect(
                    info.getType(),
                    refreshRate() + 20, // Duración ligeramente mayor a la tasa de refresco para evitar parpadeo
                    info.getAmplifier(),
                    info.isAmbient(),
                    info.hasParticles(),
                    info.showIcon()
                ));
            }
        }
    }
    
    /**
     * Obtiene la tasa de actualización de efectos en ticks
     * @return La tasa de actualización en ticks
     */
    public int refreshRate() {
        return plugin.getPluginConfig().getInt("playerEffects.refreshRate", 20);
    }
    
    /**
     * Recarga la configuración de efectos
     */
    public void reload() {
        stopEffectsTask();
        loadEffectsConfiguration();
        startEffectsTask();
    }
    
    /**
     * Limpia todos los efectos de un jugador específico
     * @param player El jugador del que limpiar los efectos
     */
    public void clearEffects(Player player) {
        for (EffectInfo info : configuredEffects.values()) {
            player.removePotionEffect(info.getType());
        }
    }
    
    /**
     * Deshabilita los efectos para un jugador específico
     * @param player El jugador para quien deshabilitar los efectos
     */
    public void disableEffectsForPlayer(Player player) {
        disabledPlayers.add(player.getUniqueId());
        clearEffects(player);
    }
    
    /**
     * Habilita los efectos para un jugador específico
     * @param player El jugador para quien habilitar los efectos
     */
    public void enableEffectsForPlayer(Player player) {
        disabledPlayers.remove(player.getUniqueId());
    }
    
    /**
     * Verifica si un jugador tiene los efectos deshabilitados
     * @param player El jugador a verificar
     * @return true si el jugador tiene los efectos deshabilitados
     */
    public boolean hasDisabledEffects(Player player) {
        return disabledPlayers.contains(player.getUniqueId());
    }
    
    /**
     * Cambia el estado de los efectos para un jugador (activar/desactivar)
     * @param player El jugador a modificar
     * @return true si los efectos fueron habilitados, false si fueron deshabilitados
     */
    public boolean togglePlayerEffects(Player player) {
        if (disabledPlayers.contains(player.getUniqueId())) {
            enableEffectsForPlayer(player);
            return true;
        } else {
            disableEffectsForPlayer(player);
            return false;
        }
    }
    
    /**
     * Cambia el estado de habilitación de un efecto específico
     * @param effectKey Clave del efecto a modificar
     * @return true si el efecto fue habilitado, false si fue deshabilitado o no existe
     */
    public boolean toggleEffect(String effectKey) {
        EffectInfo info = configuredEffects.get(effectKey);
        if (info == null) return false;
        
        boolean newState = !info.isEnabled();
        info.setEnabled(newState);
        
        // Actualizar configuración
        plugin.getPluginConfig().set("playerEffects.effects." + effectKey + ".enabled", newState);
        plugin.savePluginConfig();
        
        return newState;
    }
    
    /**
     * Cambia el estado de todos los efectos
     * @param enabled El nuevo estado para todos los efectos
     */
    public void setAllEffects(boolean enabled) {
        for (Map.Entry<String, EffectInfo> entry : configuredEffects.entrySet()) {
            entry.getValue().setEnabled(enabled);
            plugin.getPluginConfig().set("playerEffects.effects." + entry.getKey() + ".enabled", enabled);
        }
        plugin.savePluginConfig();
    }
    
    /**
     * Cambia el estado global de los efectos
     * @param enabled El nuevo estado global
     */
    public void setGlobalEffectsEnabled(boolean enabled) {
        effectsEnabled = enabled;
        plugin.getPluginConfig().set("playerEffects.enabled", enabled);
        plugin.savePluginConfig();
        
        if (enabled) {
            startEffectsTask();
        } else {
            stopEffectsTask();
            // Limpiar efectos de todos los jugadores online
            for (Player player : Bukkit.getOnlinePlayers()) {
                clearEffects(player);
            }
        }
    }
    
    /**
     * Obtiene un mapa de los efectos configurados
     * @return Mapa con clave/EffectInfo
     */
    public Map<String, EffectInfo> getConfiguredEffects() {
        return new HashMap<>(configuredEffects);
    }
    
    /**
     * Verifica si los efectos están habilitados globalmente
     * @return true si los efectos están habilitados
     */
    public boolean areEffectsEnabled() {
        return effectsEnabled;
    }
    
    /**
     * Apaga el administrador de efectos y limpia recursos
     */
    public void shutdown() {
        stopEffectsTask();
        for (Player player : Bukkit.getOnlinePlayers()) {
            clearEffects(player);
        }
    }

    /**
     * Clase interna para almacenar información de los efectos de poción
     */
    public static class EffectInfo {
        private final PotionEffectType type;
        private boolean enabled;
        private final int amplifier;
        private final boolean particles;
        private final boolean ambient;
        private final boolean icon;
        
        public EffectInfo(PotionEffectType type, boolean enabled, int amplifier, boolean particles, boolean ambient, boolean icon) {
            this.type = type;
            this.enabled = enabled;
            this.amplifier = amplifier;
            this.particles = particles;
            this.ambient = ambient;
            this.icon = icon;
        }
        
        public PotionEffectType getType() {
            return type;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public int getAmplifier() {
            return amplifier;
        }
        
        public boolean hasParticles() {
            return particles;
        }
        
        public boolean isAmbient() {
            return ambient;
        }
        
        public boolean showIcon() {
            return icon;
        }
    }
}