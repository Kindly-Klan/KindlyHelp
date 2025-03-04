package kindly.klan;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class JumpPadCommandExecutor implements CommandExecutor, TabCompleter {

    private final KindlyHelpPlugin plugin;
    private final JumpPadManager jumpPadManager;

    public JumpPadCommandExecutor(KindlyHelpPlugin plugin, JumpPadManager jumpPadManager) {
        this.plugin = plugin;
        this.jumpPadManager = jumpPadManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("jumppad")) {
            // Verificar permisos
            if (!sender.hasPermission("kindlyhelp.jumppad")) {
                sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
                return true;
            }

            // Verificar si hay argumentos
            if (args.length == 0) {
                showHelp(sender);
                return true;
            }

            // Manejar comandos
            switch (args[0].toLowerCase()) {
                case "create":
                case "new":
                    handleCreate(sender, args);
                    break;
                case "remove":
                case "delete":
                    handleRemove(sender, args);
                    break;
                case "list":
                    handleList(sender);
                    break;
                case "cancel":
                    handleCancel(sender);
                    break;
                case "help":
                    showHelp(sender);
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "Comando no reconocido. Usa /jumppad help para ver la ayuda.");
            }

            return true;
        }

        return false;
    }

    /**
     * Muestra ayuda sobre comandos de JumpPad
     * @param sender El destinatario del mensaje
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "=== Ayuda de JumpPad ===");
        sender.sendMessage(ChatColor.GOLD + "/jumppad create <id>" + ChatColor.WHITE + " - Crea un nuevo JumpPad");
        sender.sendMessage(ChatColor.GOLD + "/jumppad remove <id>" + ChatColor.WHITE + " - Elimina un JumpPad existente");
        sender.sendMessage(ChatColor.GOLD + "/jumppad list" + ChatColor.WHITE + " - Lista todos los JumpPads");
        sender.sendMessage(ChatColor.GOLD + "/jumppad cancel" + ChatColor.WHITE + " - Cancela la creación de un JumpPad");
        sender.sendMessage(ChatColor.GOLD + "/jumppad help" + ChatColor.WHITE + " - Muestra esta ayuda");
    }

    /**
     * Inicia el proceso de creación de un JumpPad
     * @param sender El comando emisor
     * @param args Los argumentos del comando
     */
    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser usado por un jugador.");
            return;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso: /jumppad create <id>");
            return;
        }

        String id = args[1];
        jumpPadManager.startJumpPadSetup(player, id);
    }

    /**
     * Maneja el comando para eliminar un JumpPad
     * @param sender El comando emisor
     * @param args Los argumentos del comando
     */
    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /jumppad remove <id>");
            return;
        }

        String id = args[1];
        if (jumpPadManager.removeJumpPad(id)) {
            sender.sendMessage(ChatColor.GREEN + "JumpPad eliminado: " + id);
        } else {
            sender.sendMessage(ChatColor.RED + "No se encontró ningún JumpPad con el ID: " + id);
        }
    }

    /**
     * Lista todos los JumpPads existentes
     * @param sender El destinatario de la lista
     */
    private void handleList(CommandSender sender) {
        String[] ids = jumpPadManager.getJumpPadIds();
        
        if (ids.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "No hay JumpPads configurados.");
            return;
        }
        
        sender.sendMessage(ChatColor.YELLOW + "=== JumpPads (" + ids.length + ") ===");
        for (String id : ids) {
            sender.sendMessage(ChatColor.GREEN + "- " + id);
        }
    }

    /**
     * Cancela el proceso de creación de un JumpPad
     * @param sender El comando emisor
     */
    private void handleCancel(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser usado por un jugador.");
            return;
        }

        jumpPadManager.cancelJumpPadSetup((Player) sender);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("jumppad")) {
            return Collections.emptyList();
        }

        // Completar subcomandos
        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("create", "remove", "list", "cancel", "help");
            return filterCompletions(subcommands, args[0]);
        }

        // Completar IDs de JumpPads para el comando "remove"
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return filterCompletions(Arrays.asList(jumpPadManager.getJumpPadIds()), args[1]);
        }

        return Collections.emptyList();
    }

    /**
     * Filtra completaciones según lo que el usuario ha escrito
     * @param options Las opciones disponibles
     * @param input La entrada del usuario
     * @return Lista de opciones filtradas
     */
    private List<String> filterCompletions(List<String> options, String input) {
        if (input.isEmpty()) {
            return options;
        }

        String lowerInput = input.toLowerCase();
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(lowerInput))
                .collect(Collectors.toList());
    }
}