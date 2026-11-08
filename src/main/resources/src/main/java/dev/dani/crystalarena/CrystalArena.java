package dev.dani.crystalarena;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class CrystalArena extends JavaPlugin implements Listener, CommandExecutor {

    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();
    private String world;
    private int minX, maxX, minY, maxY, minZ, maxZ;
    private boolean armed = false;

    private static final Set<Material> ALLOWED = Set.of(
        Material.OBSIDIAN, Material.RESPAWN_ANCHOR, Material.GLOWSTONE
    );

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadRegion();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("arena").setExecutor(this);
    }

    private void loadRegion() {
        if (!getConfig().getBoolean("set", false)) return;
        world = getConfig().getString("world");
        minX = getConfig().getInt("minX"); maxX = getConfig().getInt("maxX");
        minY = getConfig().getInt("minY"); maxY = getConfig().getInt("maxY");
        minZ = getConfig().getInt("minZ"); maxZ = getConfig().getInt("maxZ");
        armed = true;
    }

    private boolean inArena(Location l) {
        return armed && l.getWorld() != null && l.getWorld().getName().equals(world)
            && l.getBlockX() >= minX && l.getBlockX() <= maxX
            && l.getBlockY() >= minY && l.getBlockY() <= maxY
            && l.getBlockZ() >= minZ && l.getBlockZ() <= maxZ;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (!(s instanceof Player p) || !p.hasPermission("crystalarena.admin")) {
            s.sendMessage("No permission.");
            return true;
        }
        if (a.length == 0) { p.sendMessage("/arena pos1 | pos2 | set | clear"); return true; }
        switch (a[0].toLowerCase()) {
            case "pos1" -> { pos1.put(p.getUniqueId(), p.getLocation()); p.sendMessage("Corner 1 saved."); }
            case "pos2" -> { pos2.put(p.getUniqueId(), p.getLocation()); p.sendMessage("Corner 2 saved."); }
            case "set" -> {
                Location l1 = pos1.get(p.getUniqueId()), l2 = pos2.get(p.getUniqueId());
                if (l1 == null || l2 == null || !l1.getWorld().equals(l2.getWorld())) {
                    p.sendMessage("Set both corners in the same world first."); return true;
                }
                getConfig().set("set", true);
                getConfig().set("world", l1.getWorld().getName());
                getConfig().set("minX", Math.min(l1.getBlockX(), l2.getBlockX()));
                getConfig().set("maxX", Math.max(l1.getBlockX(), l2.getBlockX()));
                getConfig().set("minY", Math.min(l1.getBlockY(), l2.getBlockY()));
                getConfig().set("maxY", Math.max(l1.getBlockY(), l2.getBlockY()));
                getConfig().set("minZ", Math.min(l1.getBlockZ(), l2.getBlockZ()));
                getConfig().set("maxZ", Math.max(l1.getBlockZ(), l2.getBlockZ()));
                saveConfig();
                loadRegion();
                p.sendMessage("Arena region saved and protected.");
            }
            case "clear" -> {
                getConfig().set("set", false); saveConfig(); armed = false;
                p.sendMessage("Arena protection removed.");
            }
            default -> p.sendMessage("/arena pos1 | pos2 | set | clear");
        }
        return true;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (!inArena(e.getBlock().getLocation())) return;
        if (e.getPlayer().hasPermission("crystalarena.admin")) return;
        if (!ALLOWED.contains(e.getBlockPlaced().getType())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("You can't place that in the arena.");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) return;
        if (!inArena(e.getClickedBlock().getLocation())) return;
        if (e.getPlayer().hasPermission("crystalarena.admin")) return;
        Material held = e.getMaterial();
        if (held.name().endsWith("_SPAWN_EGG") || held == Material.ARMOR_STAND
            || held == Material.ITEM_FRAME || held == Material.GLOW_ITEM_FRAME
            || held == Material.PAINTING) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onHanging(HangingPlaceEvent e) {
        if (e.getPlayer() == null || !inArena(e.getEntity().getLocation())) return;
        if (!e.getPlayer().hasPermission("crystalarena.admin")) e.setCancelled(true);
    }
}
