package kindly.klan;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.ChatColor;

public class ServerNavigatorListener implements Listener {

    private KindlyHelpPlugin plugin;
    private ServerNavigator navigator;

    public ServerNavigatorListener(KindlyHelpPlugin plugin, ServerNavigator navigator) {
        this.plugin = plugin;
        this.navigator = navigator;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Corrección de la condición para verificar si es el inventario del navegador
        if (navigator.isNavigatorInventory(event.getView().getTitle())) {
            event.setCancelled(true); // Prevenir que se tomen los ítems
            
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                String serverName = navigator.getServerNameFromItem(event.getCurrentItem());
                if (serverName != null && !serverName.isEmpty()) {
                    Player player = (Player) event.getWhoClicked();
                    navigator.connectToServer(player, serverName);
                    player.closeInventory();
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // Verificar si es el ítem del navegador de servidores
        if (item != null && 
            (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            try {
                Material navigatorMaterial = Material.valueOf(plugin.getPluginConfig().getString("navigator.item", "COMPASS"));
                if (item.getType() == navigatorMaterial && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    String navigatorName = ChatColor.translateAlternateColorCodes('&', 
                        plugin.getPluginConfig().getString("navigator.itemName", "&bServidores"));
                    if (item.getItemMeta().getDisplayName().equals(navigatorName)) {
                        event.setCancelled(true);
                        navigator.openNavigator(player);
                    }
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Material de ítem del navegador no válido en config.yml");
            }
        }
    }
}