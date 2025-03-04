package kindly.klan;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class BossBarManager {

    private KindlyHelpPlugin plugin;
    private List<BossBarInfo> bossBars;
    private int currentIndex = 0;
    private BukkitTask rotationTask;
    private BukkitTask progressTask;
    private BossBar activeBossBar;
    private boolean isChanging = false;

    public BossBarManager(KindlyHelpPlugin plugin) {
        this.plugin = plugin;
        this.bossBars = new ArrayList<>();
        loadBossBarMessages();
        startRotation();
    }
    
    /**
     * Carga los mensajes de BossBar desde la configuración
     */
    private void loadBossBarMessages() {
        bossBars.clear();
        
        // Verificar si las BossBars están habilitadas
        if (!plugin.getPluginConfig().getBoolean("bossbars.enabled", true)) {
            return;
        }
        
        // Cargar mensajes personalizados
        ConfigurationSection barsSection = plugin.getPluginConfig().getConfigurationSection("bossbars.messages");
        
        // Si no hay sección de mensajes, cargar algunos por defecto
        if (barsSection == null) {
            // Crear mensajes por defecto
            plugin.getLogger().info("No se encontró configuración de BossBar, usando valores por defecto.");
            
            // Añadir valores por defecto a la configuración para que el usuario los pueda editar
            ConfigurationSection mainSection = plugin.getPluginConfig().createSection("bossbars");
            mainSection.set("enabled", true);
            mainSection.set("showProgress", true);
            
            ConfigurationSection messagesSection = mainSection.createSection("messages");
            
            ConfigurationSection message1 = messagesSection.createSection("welcome");
            message1.set("text", "&6Bienvenido a nuestro servidor");
            message1.set("color", "YELLOW");
            message1.set("style", "SOLID");
            message1.set("duration", 10); // Duración en segundos
            
            ConfigurationSection message2 = messagesSection.createSection("enjoy");
            message2.set("text", "&aDisfruta tu estancia");
            message2.set("color", "GREEN");
            message2.set("style", "SEGMENTED_6");
            message2.set("duration", 10);
            
            ConfigurationSection message3 = messagesSection.createSection("social");
            message3.set("text", "&dSíguenos en redes sociales");
            message3.set("color", "PINK");
            message3.set("style", "SEGMENTED_10");
            message3.set("duration", 10);
            
            plugin.savePluginConfig();
            
            // Cargar los mensajes predeterminados
            bossBars.add(new BossBarInfo(
                ChatColor.translateAlternateColorCodes('&', "&6Bienvenido a nuestro servidor"),
                "YELLOW", "SOLID", 10
            ));
            bossBars.add(new BossBarInfo(
                ChatColor.translateAlternateColorCodes('&', "&aDisfruta tu estancia"),
                "GREEN", "SEGMENTED_6", 10
            ));
            bossBars.add(new BossBarInfo(
                ChatColor.translateAlternateColorCodes('&', "&dSíguenos en redes sociales"),
                "PINK", "SEGMENTED_10", 10
            ));
            return;
        }
        
        // Cargar cada mensaje de BossBar desde la configuración
        for (String key : barsSection.getKeys(false)) {
            ConfigurationSection barSection = barsSection.getConfigurationSection(key);
            if (barSection == null) continue;
            
            String message = ChatColor.translateAlternateColorCodes('&', 
                barSection.getString("text", "Mensaje de BossBar"));
                
            String colorName = barSection.getString("color", "PURPLE").toUpperCase();
            String styleName = barSection.getString("style", "SOLID").toUpperCase();
            int duration = barSection.getInt("duration", 10); // Valor predeterminado de 10 segundos
            
            // Validar duración mínima
            if (duration < 1) {
                plugin.getLogger().warning("La duración para el mensaje '" + key + "' es demasiado baja. Se establecerá a 1 segundo.");
                duration = 1;
            }
            
            bossBars.add(new BossBarInfo(message, colorName, styleName, duration));
        }
        
        plugin.getLogger().info("Cargados " + bossBars.size() + " mensajes de BossBar.");
    }
    
    /**
     * Inicia la rotación de los mensajes de BossBar
     */
    private void startRotation() {
        // Limpiar tareas anteriores
        stopAllTasks();
        
        // Si no hay mensajes o está deshabilitado, no hacer nada
        if (bossBars.isEmpty() || !plugin.getPluginConfig().getBoolean("bossbars.enabled", true)) {
            return;
        }
        
        // Reseteamos el índice
        currentIndex = 0;
        
        // Mostramos el primer BossBar inmediatamente
        showCurrentBossBar();
    }
    
    /**
     * Detiene todas las tareas relacionadas con BossBars
     */
    private void stopAllTasks() {
        // Cancelar tareas de rotación
        if (rotationTask != null) {
            rotationTask.cancel();
            rotationTask = null;
        }
        
        // Cancelar tareas de progreso
        if (progressTask != null) {
            progressTask.cancel();
            progressTask = null;
        }
        
        // Ocultar BossBar activa
        hideBossBar();
        
        // Resetear flag de cambio
        isChanging = false;
    }
    
    /**
     * Muestra el BossBar actual y programa su duración
     */
    private void showCurrentBossBar() {
        if (bossBars.isEmpty()) return;
        
        // Asegurar que el índice es válido
        if (currentIndex >= bossBars.size()) {
            currentIndex = 0;
        }
        
        // Si estamos en proceso de cambio, evitar cambios concurrentes
        if (isChanging) return;
        isChanging = true;
        
        // Obtener la información del BossBar actual
        BossBarInfo info = bossBars.get(currentIndex);
        
        try {
            // Ocultar BossBar anterior
            hideBossBar();
            
            // Obtener color y estilo
            BarColor color = BarColor.valueOf(info.getColorName());
            BarStyle style = BarStyle.valueOf(info.getStyleName());
            
            // Crear nueva BossBar
            activeBossBar = Bukkit.createBossBar(info.getMessage(), color, style);
            activeBossBar.setProgress(1.0); // Iniciar lleno
            
            // Añadir a todos los jugadores actuales
            for (Player player : Bukkit.getOnlinePlayers()) {
                activeBossBar.addPlayer(player);
            }
            
            // Mostrar la barra
            activeBossBar.setVisible(true);
            
            // Efecto de progreso si está habilitado
            if (plugin.getPluginConfig().getBoolean("bossbars.showProgress", true)) {
                startProgressBar(info.getDuration());
            }
            
            // Programar el cambio al siguiente mensaje
            final int durationTicks = info.getDuration();
            
            rotationTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Avanzar al siguiente índice
                currentIndex = (currentIndex + 1) % bossBars.size();
                isChanging = false;
                showCurrentBossBar(); // Mostrar el siguiente BossBar
            }, durationTicks);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error al mostrar BossBar:", e);
            isChanging = false;
        }
    }
    
    /**
     * Inicia la animación de progreso de la barra
     * @param durationTicks Duración total en ticks
     */
    private void startProgressBar(int durationTicks) {
        // Cancelar tarea anterior si existe
        if (progressTask != null) {
            progressTask.cancel();
        }
        
        // Crear nueva tarea de progreso
        progressTask = new BukkitRunnable() {
            private int elapsed = 0;
            
            @Override
            public void run() {
                if (activeBossBar == null) {
                    this.cancel();
                    return;
                }
                
                // Actualizar progreso
                double progress = 1.0 - ((double) elapsed / durationTicks);
                if (progress < 0) progress = 0;
                activeBossBar.setProgress(progress);
                
                // Incrementar tiempo transcurrido
                elapsed += 5; // Actualizar cada 5 ticks (1/4 segundo) para reducir carga
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }
    
    /**
     * Oculta la BossBar actual
     */
    private void hideBossBar() {
        if (activeBossBar != null) {
            activeBossBar.removeAll();
            activeBossBar.setVisible(false);
            activeBossBar = null;
        }
    }
    
    /**
     * Añade un jugador a la BossBar activa
     * @param player El jugador a añadir
     */
    public void addPlayer(Player player) {
        if (activeBossBar != null) {
            activeBossBar.addPlayer(player);
        }
    }
    
    /**
     * Elimina un jugador de la BossBar activa
     * @param player El jugador a eliminar
     */
    public void removePlayer(Player player) {
        if (activeBossBar != null) {
            activeBossBar.removePlayer(player);
        }
    }
    
    /**
     * Recarga la configuración de las BossBars
     */
    public void reload() {
        stopAllTasks();
        loadBossBarMessages();
        startRotation();
    }
    
    /**
     * Detiene las BossBars y limpia los recursos
     */
    public void shutdown() {
        stopAllTasks();
    }
    
    /**
     * Clase interna para almacenar información de una BossBar
     */
    private static class BossBarInfo {
        private String message;
        private String colorName;
        private String styleName;
        private int duration; // en ticks
        
        public BossBarInfo(String message, String colorName, String styleName, int duration) {
            this.message = message;
            this.colorName = colorName;
            this.styleName = styleName;
            this.duration = duration * 20; // Convertir segundos a ticks
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getColorName() {
            return colorName;
        }
        
        public String getStyleName() {
            return styleName;
        }
        
        public int getDuration() {
            return duration;
        }
    }
}