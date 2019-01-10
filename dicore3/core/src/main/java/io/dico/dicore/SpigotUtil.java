package io.dico.dicore;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Attachable;
import org.bukkit.material.MaterialData;
import org.bukkit.projectiles.ProjectileSource;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class SpigotUtil {
    
    private SpigotUtil() {
        throw new UnsupportedOperationException();
    }
    
    public static World matchWorld(String input) {
        try {
            UUID uid = UUID.fromString(input);
            World world = Bukkit.getWorld(uid);
            if (world != null) {
                return world;
            }
        } catch (IllegalArgumentException ignored) {
        }
        
        World result = Bukkit.getWorld(input);
        if (result == null) {
            input = input.toLowerCase().replace("_", "").replaceAll("[-_]", "");
            for (World world : Bukkit.getWorlds()) {
                if (world.getName().toLowerCase().equals(input)) {
                    result = world;
                    break;
                }
            }
        }
        
        return result;
    }
    
    public static Block getSupportingBlock(Block block) {
        MaterialData data = block.getState().getData();
        if (data instanceof Attachable) {
            BlockFace attachedOn = ((Attachable) data).getAttachedFace();
            return block.getRelative(attachedOn);
        }
        return null;
    }
    
    public static boolean isItemPresent(ItemStack stack) {
        return stack != null && stack.getType() != Material.AIR && stack.getAmount() > 0;
    }
    
    public static boolean removeItems(Inventory from, ItemStack item, int amount) {
        for (Map.Entry<Integer, ? extends ItemStack> entry : from.all(item.getType()).entrySet()) {
            ItemStack stack = entry.getValue();
            if (item.isSimilar(stack)) {
                amount -= stack.getAmount();
                int stackAmount = -Math.min(0, amount);
                if (stackAmount == 0) {
                    from.setItem(entry.getKey(), null);
                } else {
                    stack.setAmount(stackAmount);
                }
            }
        }
        return amount <= 0;
    }
    
    public static BlockFace yawToFace(float yaw) {
        if ((yaw %= 360) < 0)
            yaw += 360;
        if (45 <= yaw && yaw < 135)
            return BlockFace.WEST;
        if (135 <= yaw && yaw < 225)
            return BlockFace.NORTH;
        if (225 <= yaw && yaw < 315)
            return BlockFace.EAST;
        return BlockFace.SOUTH;
    }
    
    public static void addItems(InventoryHolder entity, ItemStack... items) {
        Location dropLocation;
        if (entity instanceof Entity) {
            dropLocation = ((Entity) entity).getLocation();
        } else if (entity instanceof BlockState) {
            dropLocation = ((BlockState) entity).getLocation().add(0.5, 1, 0.5);
        } else {
            throw new IllegalArgumentException("Can't find location of this InventoryHolder: " + entity);
        }
        World world = dropLocation.getWorld();
        for (ItemStack toDrop : entity.getInventory().addItem(items).values()) {
            world.dropItemNaturally(dropLocation, toDrop);
        }
    }
    
    public static String asJsonString(Object object) {
        return asJsonString(null, object, 0);
    }
    
    public static String asJsonString(String key, Object object, int indentation) {
        String indent = new String(new char[indentation * 2]).replace('\0', ' ');
        StringBuilder builder = new StringBuilder(indent);
        if (key != null) {
            builder.append(key).append(": ");
        }
        if (object instanceof ConfigurationSerializable) {
            object = ((ConfigurationSerializable) object).serialize();
        }
        if (object instanceof Map) {
            builder.append("{\n");
            Map<?, ?> map = (Map) object;
            for (Map.Entry entry : map.entrySet()) {
                builder.append(asJsonString(String.valueOf(entry.getKey()), entry.getValue(), indentation + 1));
            }
            builder.append(indent).append("}");
        } else if (object instanceof List) {
            builder.append("[\n");
            List list = (List) object;
            for (Object entry : list) {
                builder.append(asJsonString(null, entry, indentation + 1));
            }
            builder.append(indent).append("]");
        } else {
            builder.append(String.valueOf(object));
        }
        return builder.append(",\n").toString();
    }
    
    public static String asJsonString(String key, Object object, int indentation, BiConsumer<List<?>, StringBuilder> listHeader) {
        String indent = new String(new char[indentation * 2]).replace('\0', ' ');
        StringBuilder builder = new StringBuilder(indent);
        if (key != null) {
            builder.append(key).append(": ");
        }
        if (object instanceof ConfigurationSerializable) {
            object = ((ConfigurationSerializable) object).serialize();
        }
        if (object instanceof Map) {
            builder.append("{\n");
            Map<?, ?> map = (Map) object;
            for (Map.Entry entry : map.entrySet()) {
                builder.append(asJsonString(String.valueOf(entry.getKey()), entry.getValue(), indentation + 1, listHeader));
            }
            builder.append(indent).append("}");
        } else if (object instanceof List) {
            builder.append("[");
            List list = (List) object;
            listHeader.accept(list, builder);
            builder.append("\n");
            for (Object entry : list) {
                builder.append(asJsonString(null, entry, indentation + 1, listHeader));
            }
            builder.append(indent).append("]");
        } else {
            builder.append(String.valueOf(object));
        }
        return builder.append(",\n").toString();
    }
    
    public static BlockFace estimateDirectionTo(Location from, Location to) {
        double dx = from.getX() - to.getX();
        double dz = from.getZ() - to.getZ();
    
        boolean xGreater = Math.abs(dx) - Math.abs(dz) > 0;
        double f = xGreater ? 2 / Math.abs(dx) : 2 / Math.abs(dz);
        dx *= f;
        dz *= f;
    
        double other = Math.abs(xGreater ? dz : dx);
    
        if (other <= .5) {
            return xGreater ? (dx < 0 ? BlockFace.WEST : BlockFace.EAST) : (dz < 0 ? BlockFace.NORTH : BlockFace.SOUTH);
        }
    
        if (other < 1.5) {
            if (xGreater) {
                return dx < 0 ? (dz < 0 ? BlockFace.WEST_NORTH_WEST : BlockFace.WEST_SOUTH_WEST) : (dz < 0 ? BlockFace.EAST_NORTH_EAST : BlockFace.EAST_SOUTH_EAST);
            }
            return dx < 0 ? (dz < 0 ? BlockFace.NORTH_NORTH_WEST : BlockFace.SOUTH_SOUTH_WEST) : (dz < 0 ? BlockFace.NORTH_NORTH_EAST : BlockFace.SOUTH_SOUTH_EAST);
        }
    
        return dx < 0 ? (dz < 0 ? BlockFace.NORTH_WEST : BlockFace.SOUTH_WEST) : (dz < 0 ? BlockFace.NORTH_EAST : BlockFace.SOUTH_EAST);
    }
    
    public static Entity findEntityFromDamager(Entity damager, EntityType searched) {
        if (damager.getType() == searched) {
            return damager;
        }
        
        if (damager instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Entity && ((Entity) shooter).getType() == searched) {
                return (Entity) shooter;
            }
            return null;
        }
        
        if (damager.getType() == EntityType.PRIMED_TNT) {
            Entity source = ((TNTPrimed) damager).getSource();
            if (source.getType() == searched) {
                return source;
            }
        }
    
        return null;
    }
    
    public static int xpForNextLevel(int currentLevel) {
        if (currentLevel >= 30) {
            return 112 + (currentLevel - 30) * 9;
        }
        
        if (currentLevel >= 15) {
            return 37 + (currentLevel - 15) * 5;
        }
        
        return 7 + currentLevel * 2;
    }
    
    public static int removeExp(Player entity, int xp) {
        int total = entity.getTotalExperience();
        if (xp > total) {
            xp = total;
        }
        
        int level = entity.getLevel();
        if (level < 0) {
            return 0;
        }
        
        int removed = 0;
        int xpForNextLevel = xpForNextLevel(level);
        int current = (int) entity.getExp() * xpForNextLevel;
        
        if (xp > current) {
            xp -= current;
            total -= current;
            removed += current;
            
            if (level == 0) {
                entity.setExp(0F);
                entity.setTotalExperience(total);
                return removed;
            }
        } else {
            current -= xp;
            total -= xp;
            removed += xp;
            
            entity.setExp((float) current / xpForNextLevel);
            entity.setTotalExperience(total);
            return removed;
        }
        
        do {
            xpForNextLevel = xpForNextLevel(--level);
            if (xpForNextLevel >= xp) {
                total -= xp;
                removed += xp;
                
                entity.setExp(1F / xpForNextLevel * (xpForNextLevel - xp));
                entity.setTotalExperience(total);
                entity.setLevel(level);
                return removed;
            }
            
            xp -= xpForNextLevel;
            total -= xpForNextLevel;
            removed += xpForNextLevel;
        } while (level > 0);
        
        entity.setExp(0F);
        entity.setTotalExperience(0);
        entity.setLevel(0);
        return removed;
    }
    
    public static int getTotalExp(Player entity) {
        int rv = 0;
        int level = Math.min(entity.getLevel(), 20000);
        for (int i = 0; i < level; i++) {
            rv += xpForNextLevel(i);
        }
        rv += Math.min(1F, Math.max(0F, entity.getExp())) * xpForNextLevel(level);
        return rv;
    }
    
    public static double getTotalExpLevels(Player entity) {
        int xp = entity.getTotalExperience();
        
        int level = 0;
        while (xp > 0 && level < 20000) {
            int needed = xpForNextLevel(level);
            if (needed > xp) {
                return level + ((double) xp / needed);
            }
            xp -= needed;
            level++;
        }
        
        return level;
    }
    
    public static int getNearbyPlayerCount(Player origin, double range, Predicate<Player> predicate) {
        List<Entity> entities = origin.getNearbyEntities(range, range, range);
        int result = 0;
        for (Entity entity : entities) {
            if (entity.getType() == EntityType.PLAYER && predicate.test((Player) entity)) {
                result++;
            }
        }
        return result;
    }
    
    public static void getNearbyPlayers(Player origin, double range, Collection<Player> collection, Predicate<Player> predicate) {
        List<Entity> entities = origin.getNearbyEntities(range, range, range);
        for (Entity entity : entities) {
            if (entity.getType() == EntityType.PLAYER && predicate.test((Player) entity)) {
                collection.add((Player) entity);
            }
        }
    }
    
    public static void forEachNearbyPlayer(Player origin, double range, Consumer<Player> action) {
        List<Entity> entities = origin.getNearbyEntities(range, range, range);
        for (Entity entity : entities) {
            if (entity.getType() == EntityType.PLAYER) {
                action.accept((Player) entity);
            }
        }
    }
    
    public static double distanceSquared(Location first, Location second) {
        double dx = first.getX() - second.getX();
        double dy = first.getY() - second.getY();
        double dz = first.getZ() - second.getZ();
        
        return dx * dx + dy * dy + dz * dz;
    }
    
    
    public static <T extends Entity> Iterator<T> findNearbyEntities(Entity origin, boolean includeSelf, Predicate<Entity> predicate, double horizontalRange, double verticalRange) {
        Objects.requireNonNull(origin);
        return new Iterator<T>() {
            Entity next;
            List<Entity> nearby;
            int index = 0;
            int size;
            
            {
                if (includeSelf) {
                    next = origin;
                } else {
                    next = findNext();
                }
            }
            
            Entity findNext() {
                if (nearby == null) {
                    nearby = origin.getNearbyEntities(horizontalRange, verticalRange, horizontalRange);
                    size = nearby.size();
                }
                
                while (index < size) {
                    Entity e = nearby.get(index++);
                    if (predicate.test(e)) {
                        return e;
                    }
                }
                
                return null;
            }
            
            @Override
            public boolean hasNext() {
                return next != null;
            }
            
            @Override
            public T next() {
                if (next == null) {
                    throw new NoSuchElementException();
                }
                Entity result = next;
                next = findNext();
                //noinspection unchecked
                return (T) result;
            }
            
        };
    }
    
    
}
