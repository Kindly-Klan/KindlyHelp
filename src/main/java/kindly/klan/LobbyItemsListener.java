package kindly.klan;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class LobbyItemsListener implements Listener {
    
    private final KindlyHelpPlugin plugin;
    
    public LobbyItemsListener(KindlyHelpPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (isLobbyItem(item)) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        // Bloquear cualquier tipo de interacción con ítems del lobby
        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        
        boolean isLobbyItemInvolved = (clicked != null && isLobbyItem(clicked)) || 
                                    (cursor != null && isLobbyItem(cursor));
        
        if (isLobbyItemInvolved) {
            event.setCancelled(true);
            
            // Si es un click doble o middle click en modo creativo, también cancelar
            if (event.getClick() == ClickType.DOUBLE_CLICK || 
                event.getClick() == ClickType.CREATIVE) {
                event.setCancelled(true);
                return;
            }
            
            // Si es el inventario del jugador y un ítem del lobby, permitir solo click derecho
            if (event.getClickedInventory() instanceof PlayerInventory && 
                event.getClick() == ClickType.RIGHT && 
                clicked != null && 
                isLobbyItem(clicked)) {
                // Dejar que el evento PlayerInteractEvent maneje la interacción
                return;
            }
            
            // Cancelar cualquier otro tipo de interacción
            event.setCancelled(true);
        }
        
        // Verificar movimientos de hotbar
        if (event.getHotbarButton() != -1) {
            ItemStack hotbarItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
            if (hotbarItem != null && isLobbyItem(hotbarItem)) {
                event.setCancelled(true);
            }
        }
        
        // Prevenir shift-clicks
        if (event.isShiftClick() && clicked != null && isLobbyItem(clicked)) {
            event.setCancelled(true);
        }
        
        // Prevenir drag-and-drop
        if (event.getClick() == ClickType.LEFT && cursor != null && isLobbyItem(cursor)) {
            event.setCancelled(true);
        }
        
        // Prevenir intercambio de número
        if (event.getClick() == ClickType.NUMBER_KEY) {
            ItemStack hotbarItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
            if ((clicked != null && isLobbyItem(clicked)) || 
                (hotbarItem != null && isLobbyItem(hotbarItem))) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        ItemStack dragged = event.getOldCursor();
        if (dragged != null && isLobbyItem(dragged)) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (isLobbyItem(event.getItem())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        ItemStack mainHand = event.getMainHandItem();
        ItemStack offHand = event.getOffHandItem();
        
        if ((mainHand != null && isLobbyItem(mainHand)) || 
            (offHand != null && isLobbyItem(offHand))) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreativeInventoryClick(InventoryCreativeEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        
        if ((clicked != null && isLobbyItem(clicked)) || 
            (cursor != null && isLobbyItem(cursor))) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemHold(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        ItemStack previousItem = player.getInventory().getItem(event.getPreviousSlot());
        
        // Verificar si alguno de los ítems involucrados es un ítem del lobby
        if ((newItem != null && isLobbyItem(newItem)) || 
            (previousItem != null && isLobbyItem(previousItem))) {
            // No cancelar el evento, pero asegurarse de que los ítems permanezcan en sus slots originales
            if (newItem != null && isLobbyItem(newItem)) {
                player.getInventory().setItem(event.getNewSlot(), newItem);
            }
            if (previousItem != null && isLobbyItem(previousItem)) {
                player.getInventory().setItem(event.getPreviousSlot(), previousItem);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null) return;
        
        // Solo procesar ítems del lobby
        if (!isLobbyItem(item)) return;
        
        // Permitir solo el click derecho para los ítems del lobby
        if (event.getAction() != Action.RIGHT_CLICK_AIR && 
            event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            return;
        }
        
        // Verificar si es el ítem del navegador
        if (plugin.getPluginConfig().getBoolean("navigator.enabled", true)) {
            try {
                Material navigatorMaterial = Material.valueOf(plugin.getPluginConfig().getString("navigator.item", "COMPASS"));
                String navigatorName = ChatColor.translateAlternateColorCodes('&', 
                    plugin.getPluginConfig().getString("navigator.itemName", "&bServidores"));
                
                if (item.getType() == navigatorMaterial && 
                    item.hasItemMeta() && 
                    item.getItemMeta().hasDisplayName() &&
                    item.getItemMeta().getDisplayName().equals(navigatorName)) {
                    // Permitir que ServerNavigatorListener maneje esto
                    return;
                }
            } catch (IllegalArgumentException ignored) {}
        }
        
        // Para otros ítems del lobby, prevenir la interacción por defecto
        event.setCancelled(true);
    }
    
    private boolean isLobbyItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        
        String displayName = item.getItemMeta().getDisplayName();
        
        // Verificar si es el ítem de visibilidad de jugadores
        if (plugin.getPluginConfig().getBoolean("playerVisibility.enabled", true)) {
            try {
                Material toggleMaterial = Material.valueOf(plugin.getPluginConfig().getString("playerVisibility.toggleItem", "CLOCK"));
                if (item.getType() == toggleMaterial) {
                    String showName = ChatColor.translateAlternateColorCodes('&', 
                        plugin.getPluginConfig().getString("playerVisibility.showPlayersName", "&aMostrar jugadores"));
                    String hideName = ChatColor.translateAlternateColorCodes('&', 
                        plugin.getPluginConfig().getString("playerVisibility.hidePlayersName", "&cOcultar jugadores"));
                    
                    if (displayName.equals(showName) || displayName.equals(hideName)) {
                        return true;
                    }
                }
            } catch (IllegalArgumentException ignored) {}
        }
        
        // Verificar si es el ítem del navegador
        if (plugin.getPluginConfig().getBoolean("navigator.enabled", true)) {
            try {
                Material navigatorMaterial = Material.valueOf(plugin.getPluginConfig().getString("navigator.item", "COMPASS"));
                if (item.getType() == navigatorMaterial) {
                    String navigatorName = ChatColor.translateAlternateColorCodes('&', 
                        plugin.getPluginConfig().getString("navigator.itemName", "&bServidores"));
                    
                    if (displayName.equals(navigatorName)) {
                        return true;
                    }
                }
            } catch (IllegalArgumentException ignored) {}
        }
        
        return false;
    }
}