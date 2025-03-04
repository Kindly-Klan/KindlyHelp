package kindly.klan;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class KindlyHelpTabCompleter implements TabCompleter {

    private static final List<String> COMMANDS = Arrays.asList(
        "reload", "setspawn", "tptospawn", "togglevoidspawn", 
        "sendtestannounce", "showcredits", "getlobbyitems", 
        "opennav", "navigator", "hideplayers", "showplayers",
        "bossbar"
    );

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String start = args[0].toLowerCase();
            completions = COMMANDS.stream()
                .filter(c -> c.toLowerCase().startsWith(start))
                .collect(Collectors.toList());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("tptospawn") || args[0].equalsIgnoreCase("showcredits")) {
                String start = args[1].toLowerCase();
                List<String> players = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    players.add(player.getName());
                }
                
                if (args[0].equalsIgnoreCase("showcredits")) {
                    players.add("ALL");
                }
                
                completions = players.stream()
                    .filter(p -> p.toLowerCase().startsWith(start))
                    .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("bossbar")) {
                // Opciones para el comando bossbar
                String start = args[1].toLowerCase();
                completions = Arrays.asList("reload", "toggle", "show", "hide")
                    .stream()
                    .filter(s -> s.toLowerCase().startsWith(start))
                    .collect(Collectors.toList());
            }
        }

        return completions;
    }
}
