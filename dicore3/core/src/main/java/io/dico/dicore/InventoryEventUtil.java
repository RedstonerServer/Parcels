package io.dico.dicore;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class InventoryEventUtil {
    
    private InventoryEventUtil() {
    
    }
    
    public static ItemStack getNewItem(InventoryClickEvent event) {
        Inventory clicked = event.getInventory();
        switch (event.getAction()) {
            case SWAP_WITH_CURSOR:
            case PLACE_ALL:
                return event.getCursor();
            case PICKUP_ALL:
            case HOTBAR_MOVE_AND_READD:
            case MOVE_TO_OTHER_INVENTORY:
            case DROP_ALL_SLOT:
            case COLLECT_TO_CURSOR:
                return null;
            case PICKUP_HALF:
            case PICKUP_SOME:
                ItemStack item = clicked.getItem(event.getSlot()).clone();
                item.setAmount(item.getAmount() / 2);
                return item;
            case PICKUP_ONE:
            case DROP_ONE_SLOT:
                item = clicked.getItem(event.getSlot()).clone();
                item.setAmount(Math.max(0, item.getAmount() - 1));
                return item;
            case PLACE_ONE:
                item = event.getView().getCursor().clone();
                item.setAmount(1);
                return item;
            case PLACE_SOME:
                item = event.getView().getCursor().clone();
                item.setAmount(item.getAmount() / 2);
                return item;
            case HOTBAR_SWAP:
                return event.getView().getBottomInventory().getItem(event.getHotbarButton());
            default:
                return clicked.getItem(event.getSlot());
        }
    }
    
    
    public static Map<Integer, ItemStack> deduceChangesIfItemAdded(Inventory inventory, ItemStack added, boolean computeNewItem) {
        int addedAmount = added.getAmount();
        Map<Integer, ItemStack> rv = Collections.emptyMap();
        
        for (int n = inventory.getSize(), i = 0; i < n; i++) {
            if (addedAmount <= 0) break;
            
            ItemStack current = inventory.getItem(i);
            if (current == null || current.getType() == Material.AIR || current.isSimilar(added)) {
                int count = current == null ? 0 : current.getAmount();
                int max = (current == null ? added : current).getType().getMaxStackSize();
                if (count < max) {
                    int diff = max - count;
                    if (diff > addedAmount) {
                        diff = addedAmount;
                    }
                    addedAmount -= diff;
                    
                    if (rv.isEmpty()) rv = new LinkedHashMap<>();
                    
                    if (computeNewItem) {
                        current = (current == null ? added : current).clone();
                        current.setAmount(count + diff);
                        rv.put(i, current);
                    } else {
                        rv.put(i, null);
                    }
                }
            }
        }
        
        return rv;
    }
    
    
    public static ItemStack getNewHeldItemIfPickedUp(PlayerInventory inventory, ItemStack added) {
        int heldItemSlot = inventory.getHeldItemSlot();
        ItemStack heldItem = inventory.getItem(heldItemSlot);
        
        if (SpigotUtil.isItemPresent(heldItem) && !added.isSimilar(heldItem)) {
            return null;
        }
        
        int amt = added.getAmount();
        for (int i = 0; i < heldItemSlot; i++) {
            ItemStack item = inventory.getItem(i);
            if (!SpigotUtil.isItemPresent(item)) {
                return null;
            }
            if (item.isSimilar(item)) {
                amt -= Math.max(0, item.getMaxStackSize() - item.getAmount());
                if (amt <= 0) {
                    return null;
                }
            }
        }
        
        added = added.clone();
        added.setAmount(amt + Math.max(0, heldItem == null ? 0 : heldItem.getAmount()));
        return added;
    }
    
    
}
