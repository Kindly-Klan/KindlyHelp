package kindly.klan;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class KindlyHelpCommandExecutor implements CommandExecutor, TabCompleter {

    private final KindlyHelpPlugin plugin;

    public KindlyHelpCommandExecutor(KindlyHelpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("kindlyhelp")) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "Uso: /kindlyhelp <reload|setspawn|tptospawn|togglevoidspawn|sendtestannounce|showcredits|getlobbyitems|hideplayers|showplayers|opennav|navigator|bossbar>");
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("kindlyhelp.reload")) {
                    plugin.reloadPluginConfig();
                    sender.sendMessage(ChatColor.GREEN + "Configuración recargada!");
                } else {
                    sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("setspawn")) {
                if (sender instanceof Player && sender.hasPermission("kindlyhelp.setspawn")) {
                    Player player = (Player) sender;
                    Location location = player.getLocation();
                    plugin.getPluginConfig().set("spawnLocation.world", location.getWorld().getName());
                    plugin.getPluginConfig().set("spawnLocation.x", location.getX());
                    plugin.getPluginConfig().set("spawnLocation.y", location.getY());
                    plugin.getPluginConfig().set("spawnLocation.z", location.getZ());
                    plugin.getPluginConfig().set("spawnLocation.yaw", location.getYaw());
                    plugin.getPluginConfig().set("spawnLocation.pitch", location.getPitch());
                    plugin.savePluginConfig();
                    sender.sendMessage(ChatColor.GREEN + "Ubicación de spawn establecida.");
                } else {
                    sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("tptospawn")) {
                if (args.length == 2 && sender.hasPermission("kindlyhelp.tptospawn")) {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target != null) {
                        String worldName = plugin.getPluginConfig().getString("spawnLocation.world");
                        if (worldName != null) {
                            Location spawnLocation = new Location(
                                    Bukkit.getWorld(worldName),
                                    plugin.getPluginConfig().getDouble("spawnLocation.x"),
                                    plugin.getPluginConfig().getDouble("spawnLocation.y"),
                                    plugin.getPluginConfig().getDouble("spawnLocation.z"),
                                    (float) plugin.getPluginConfig().getDouble("spawnLocation.yaw"),
                                    (float) plugin.getPluginConfig().getDouble("spawnLocation.pitch")
                            );
                            target.teleport(spawnLocation);
                            sender.sendMessage(ChatColor.GREEN + "Teletransportado " + target.getName() + " al spawn.");
                        } else {
                            sender.sendMessage(ChatColor.RED + "La ubicación de spawn no está establecida.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Uso: /kindlyhelp tptospawn <jugador>");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("togglevoidspawn")) {
                if (sender.hasPermission("kindlyhelp.togglevoidspawn")) {
                    boolean currentState = plugin.getPluginConfig().getBoolean("voidSpawnEnabled");
                    plugin.getPluginConfig().set("voidSpawnEnabled", !currentState);
                    plugin.savePluginConfig();
                    sender.sendMessage(ChatColor.GREEN + "VoidSpawn " + (currentState ? "desactivado" : "activado") + "!");
                } else {
                    sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("sendtestannounce")) {
                if (sender.hasPermission("kindlyhelp.sendtestannounce")) {
                    plugin.sendSponsorMessage();
                    sender.sendMessage(ChatColor.GREEN + "Anuncio de prueba enviado.");
                } else {
                    sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("showcredits")) {
                if (sender.hasPermission("kindlyhelp.showcredits")) {
                    if (args.length == 2) {
                        if (args[1].equalsIgnoreCase("ALL")) {
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                plugin.showCredits(player);
                            }
                            sender.sendMessage(ChatColor.GREEN + "Mostrando créditos a todos los jugadores.");
                        } else {
                            Player target = Bukkit.getPlayer(args[1]);
                            if (target != null) {
                                plugin.showCredits(target);
                                sender.sendMessage(ChatColor.GREEN + "Mostrando créditos a " + target.getName());
                            } else {
                                sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
                            }
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Uso: /kindlyhelp showcredits <ALL/jugador>");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("getlobbyitems")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (sender.hasPermission("kindlyhelp.getlobbyitems") || sender.isOp()) {
                        plugin.giveLobbyItems(player);
                        sender.sendMessage(ChatColor.GREEN + "Ítems de lobby entregados!");
                    } else {
                        sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Este comando solo puede ser ejecutado por un jugador.");
                }
                return true;
            }
            
            if (args[0].equalsIgnoreCase("opennav") || args[0].equalsIgnoreCase("navigator")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (sender.hasPermission("kindlyhelp.navigator") || sender.isOp()) {
                        plugin.getServerNavigator().openNavigator(player);
                    } else {
                        sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Este comando solo puede ser ejecutado por un jugador.");
                }
                return true;
            }
            
            if (args[0].equalsIgnoreCase("hideplayers")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (sender.hasPermission("kindlyhelp.togglevisibility") || sender.isOp()) {
                        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                            if (!onlinePlayer.equals(player)) {
                                player.hidePlayer(plugin, onlinePlayer);
                            }
                        }
                        sender.sendMessage(ChatColor.RED + "¡Jugadores ocultos!");
                    } else {
                        sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Este comando solo puede ser ejecutado por un jugador.");
                }
                return true;
            }
            
            if (args[0].equalsIgnoreCase("showplayers")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (sender.hasPermission("kindlyhelp.togglevisibility") || sender.isOp()) {
                        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                            player.showPlayer(plugin, onlinePlayer);
                        }
                        sender.sendMessage(ChatColor.GREEN + "¡Jugadores visibles!");
                    } else {
                        sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Este comando solo puede ser ejecutado por un jugador.");
                }
                return true;
            }
            
            // Nuevo comando para gestionar BossBars
            if (args[0].equalsIgnoreCase("bossbar")) {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Uso: /kindlyhelp bossbar <reload|toggle|show|hide>");
                    return true;
                }
                
                if (args[1].equalsIgnoreCase("reload")) {
                    if (sender.hasPermission("kindlyhelp.bossbar.admin") || sender.isOp()) {
                        plugin.getBossBarManager().reload();
                        sender.sendMessage(ChatColor.GREEN + "BossBars recargadas correctamente.");
                    } else {
                        sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
                    }
                    return true;
                }
                
                if (args[1].equalsIgnoreCase("toggle")) {
                    if (sender.hasPermission("kindlyhelp.bossbar.admin") || sender.isOp()) {
                        boolean currentState = plugin.getPluginConfig().getBoolean("bossbars.enabled", true);
                        plugin.getPluginConfig().set("bossbars.enabled", !currentState);
                        plugin.savePluginConfig();
                        plugin.getBossBarManager().reload();
                        
                        sender.sendMessage(ChatColor.GREEN + "BossBars " + 
                            (currentState ? "desactivadas" : "activadas") + "!");
                    } else {
                        sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
                    }
                    return true;
                }
                
                if (args[1].equalsIgnoreCase("hide")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        plugin.getBossBarManager().removePlayer(player);
                        sender.sendMessage(ChatColor.YELLOW + "BossBar oculta para ti.");
                    } else {
                        sender.sendMessage(ChatColor.RED + "Este comando solo puede ser ejecutado por un jugador.");
                    }
                    return true;
                }
                
                if (args[1].equalsIgnoreCase("show")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        plugin.getBossBarManager().addPlayer(player);
                        sender.sendMessage(ChatColor.GREEN + "BossBar visible para ti.");
                    } else {
                        sender.sendMessage(ChatColor.RED + "Este comando solo puede ser ejecutado por un jugador.");
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("kindlyhelp")) {
            if (args.length == 1) {
                completions.add("reload");
                completions.add("setspawn");
                completions.add("tptospawn");
                completions.add("togglevoidspawn");
                completions.add("sendtestannounce");
                completions.add("showcredits");
                completions.add("getlobbyitems");
                completions.add("hideplayers");
                completions.add("showplayers");
                completions.add("opennav");
                completions.add("navigator");
                completions.add("bossbar");
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("tptospawn")) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                } else if (args[0].equalsIgnoreCase("showcredits")) {
                    completions.add("ALL");
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                } else if (args[0].equalsIgnoreCase("bossbar")) {
                    completions.add("reload");
                    completions.add("toggle");
                    completions.add("show");
                    completions.add("hide");
                }
            }
        }
        return completions;
    }
}