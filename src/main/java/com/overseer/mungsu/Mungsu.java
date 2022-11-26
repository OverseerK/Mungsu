package com.overseer.mungsu;

import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static org.bukkit.Bukkit.getWorldContainer;

public final class Mungsu extends JavaPlugin implements Listener {

    World overworld;
    World netherworld;
    World enderworld;

    File beddatafile = new File(getDataFolder(), "bed.yml");
    FileConfiguration beddata = YamlConfiguration.loadConfiguration(beddatafile);
    File compassdatafile = new File(getDataFolder(), "compass.yml");
    FileConfiguration compassdata = YamlConfiguration.loadConfiguration(compassdatafile);
    File bukkitfile = new File(getWorldContainer(), "bukkit.yml");
    FileConfiguration bukkit = YamlConfiguration.loadConfiguration(bukkitfile);


    public int getDay() {
        return (int) ((overworld.getFullTime() - 18000) / 24000);
    }

    @Override
    public void onEnable() {
        Bukkit.broadcastMessage("§4【 SMP : THE NIGHTMARE 】 -  Reloaded.");
        Bukkit.getPluginManager().registerEvents(this, this);
        for (World w : Bukkit.getWorlds()) {
            if (w.getEnvironment() == World.Environment.NORMAL) {
                overworld = w;
            } else if (w.getEnvironment() == World.Environment.NETHER) {
                netherworld = w;
            } else if (w.getEnvironment() == World.Environment.THE_END) {
                enderworld = w;
            }
        }

        //기본셋팅
        overworld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        overworld.setGameRule(GameRule.REDUCED_DEBUG_INFO, true);
        netherworld.setGameRule(GameRule.REDUCED_DEBUG_INFO, true);
        enderworld.setGameRule(GameRule.REDUCED_DEBUG_INFO, true);
        overworld.setFullTime(18000);
        //하루 타이머
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            long fulltime = overworld.getFullTime();
            overworld.setFullTime(fulltime + 24000);
            Bukkit.broadcastMessage("§eDAY " + getDay());

//            if (getDay() % 5 == 0) {
//                Bukkit.broadcastMessage("§4끔찍한 밤이 될 것 같다...");
//                bukkit.set("spawn-limits.monsters", 160);
//            } else {
//                bukkit.set("spawn-limits.monsters", 80);
//            }
//            try {
//                bukkit.save(bukkitfile);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }

            //침대 삭제
            for (String name : beddata.getKeys(false)) {
                if (getDay() == beddata.getInt(name) + 15) {
                    String[] cord = name.split(",");
                    overworld.getBlockAt(Integer.parseInt(cord[0]), Integer.parseInt(cord[1]), Integer.parseInt(cord[2])).setType(Material.AIR);
                    beddata.set(name, null);
                    try {
                        beddata.save(beddatafile);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            //자석석 초기화
            for (String name : compassdata.getKeys(false)) {
                if (getDay() == compassdata.getInt(name) + 15) {
                    String[] cord = name.split(",");
                    Block b = overworld.getBlockAt(Integer.parseInt(cord[0]), Integer.parseInt(cord[1]), Integer.parseInt(cord[2]));
                    b.setType(Material.AIR);
                    Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> b.setType(Material.LODESTONE), 1);
                    compassdata.set(name, null);
                    try {
                        compassdata.save(compassdatafile);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }, 24000, 24000);

        //팬텀 랜덤 스폰
//        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
//            for (Player p : Bukkit.getOnlinePlayers()) {
//                Phantom ph = (Phantom) overworld.spawnEntity(p.getLocation().add(rand.nextInt(20) - 10, rand.nextInt(20), rand.nextInt(20) - 10), EntityType.PHANTOM);
//            }
//        }, 0, 1200);
    }

//    public Location getRandomSpawn() {
//        int boadersize = (int) overworld.getWorldBorder().getSize();
//        int x = rand.nextInt(boadersize) - boadersize / 2;
//        int z = rand.nextInt(boadersize) - boadersize / 2;
//        int y = overworld.getHighestBlockYAt(x, z);
//        return new Location(overworld, x, y, z);
//    }

//    @EventHandler
//    public void initalSpawn(PlayerJoinEvent e) {
//        Player p = e.getPlayer();
//        if (!p.hasPlayedBefore()) {
//            p.setBedSpawnLocation(getRandomSpawn(), true);
//        }
//    }
//
//    @EventHandler
//    public void randomSpawn(PlayerDeathEvent e) {
//        Player p = e.getPlayer();
//        p.setBedSpawnLocation(getRandomSpawn(), true);
//    }

    @EventHandler
    public void onBedSet(PlayerBedEnterEvent e) throws IOException {
        Location bed = e.getBed().getLocation();
        beddata.set(bed.getBlockX() + "," + bed.getBlockY() + "," + bed.getBlockZ(), getDay());
        beddata.save(beddatafile);
    }

    @EventHandler
    public void onLodestonePlace(PlayerInteractEvent e) throws IOException {
        Block b = e.getClickedBlock();
        if (b != null && b.getType() == Material.LODESTONE) {
            compassdata.set(b.getX() + "," + b.getY() + "," + b.getZ(), getDay());
            compassdata.save(compassdatafile);
        }
    }

    @EventHandler
    public void onLodestoneBreak(BlockBreakEvent e) throws IOException {
        Block b = e.getBlock();
        String loc = b.getX() + "," + b.getY() + "," + b.getZ();
        if (b.getType() == Material.LODESTONE && compassdata.contains(loc)) {
            compassdata.set(loc, null);
            compassdata.save(compassdatafile);
        }
    }

    HashMap<Player, Integer> feralTask = new HashMap<>();

    @EventHandler
    public void feralBite(EntityDamageByEntityEvent e) {
        Entity victim = e.getEntity();
        if (victim instanceof Player p && !feralTask.containsKey(p)) {
            AttributeInstance speed = p.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
            if (speed != null) {
                speed.setBaseValue(4.2);
                feralTask.put(p, Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                    speed.setBaseValue(4);
                    feralTask.remove(p);
                }, 60));
            }
        }
    }

    HashMap<Player, Integer> lightTask = new HashMap<>();

    @EventHandler
    public void enhancedLightning(EntityDamageEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.LIGHTNING) {
            if (e.getEntity() instanceof Player p) {
                if (!lightTask.containsKey(p)) {
                    e.setDamage(e.getDamage() * 3);
                    lightTask.put(p, Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                        lightTask.remove(p);
                    }, 20));
                }
            } else {
                e.setDamage(e.getDamage() * 3);
            }
        }
    }

    @EventHandler
    public void dimensionEffect(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        if (p.getWorld() == netherworld) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 0, false, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, Integer.MAX_VALUE, 0, false, false, false));
        } else if (p.getWorld() == enderworld) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, Integer.MAX_VALUE, 0, false, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 0, false, false, false));
        } else if (p.getWorld() == overworld) {
            p.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
            p.removePotionEffect(PotionEffectType.WEAKNESS);
            p.removePotionEffect(PotionEffectType.FAST_DIGGING);
            p.removePotionEffect(PotionEffectType.SLOW);
        }
    }

    @EventHandler
    public void persistentEffect(PlayerItemConsumeEvent e) {
        if (e.getItem().getType() == Material.MILK_BUCKET) {
            Player p = e.getPlayer();
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                if (p.getWorld() == netherworld) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 0, false, false, false));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, Integer.MAX_VALUE, 0, false, false, false));
                } else if (p.getWorld() == enderworld) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, Integer.MAX_VALUE, 0, false, false, false));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 0, false, false, false));
                }
            }, 1);
        }
    }

    @EventHandler
    public void onMonsterSpawn(CreatureSpawnEvent e) {
        LivingEntity en = e.getEntity();
        if (en instanceof Monster) {
            AttributeInstance attributehealth = en.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            AttributeInstance attributedamage = en.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            double modifer = 1.2;
            if (en.getCategory() == EntityCategory.UNDEAD) {
                modifer = 1.5;
            }
            if (en.getType() == EntityType.WITHER_SKELETON || en.getType() == EntityType.BLAZE) {
                modifer = 2;
            }
            if (attributehealth != null) {
                double health = attributehealth.getBaseValue() * modifer;
                attributehealth.setBaseValue(health);
                en.setHealth(health);
            }
            if (attributedamage != null) {
                attributedamage.setBaseValue(attributedamage.getBaseValue() * modifer);
            }
        }
    }

//    @EventHandler
//    public void angryPiglin(EntityDamageEvent e) {
//        if (e.getEntity() instanceof Player p && p.getWorld() == netherworld) {
//            for (Entity en : p.getNearbyEntities(10, 10, 10)) {
//                if (en instanceof PigZombie pig) {
//                    pig.setAngry(true);
//                }
//            }
//        }
//    }

    @EventHandler
    public void elytraSuperNerf(PlayerElytraBoostEvent e) {
        Player p = e.getPlayer();
        ItemStack cp = p.getInventory().getChestplate();
        if (cp != null) {
            Damageable dm = (Damageable) cp.getItemMeta();
            assert dm != null;
            int damage = dm.getDamage();
            if (damage >= 425) {
                dm.setDamage(431);
            } else {
                dm.setDamage(damage + 6);
            }
            cp.setItemMeta(dm);
        }
    }


}
