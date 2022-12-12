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
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

public final class Mungsu extends JavaPlugin implements Listener {

    Random rand = new Random();

    World overworld;
    World netherworld;
    World enderworld;

    File beddatafile = new File(getDataFolder(), "bed.yml");
    FileConfiguration beddata = YamlConfiguration.loadConfiguration(beddatafile);
    File compassdatafile = new File(getDataFolder(), "compass.yml");
    FileConfiguration compassdata = YamlConfiguration.loadConfiguration(compassdatafile);
    File configfile = new File(getDataFolder(), "config.yml");
    FileConfiguration config = YamlConfiguration.loadConfiguration(configfile);

    int elytraDamage;
    int bedBreakPeriod;
    int compassBreakPeriod;
    int wolfEnhancePeriod;
    double wolfEnhanceAmount;
    int phantomSpawnPeriod;
    int phantomSpawnDelay;
    int phantomSpawnCount;
    double hostileHealth;
    double hostileStrength;
    double undeadHealth;
    double undeadStrength;
    double netherHealth;
    double netherStrength;
    int dayLength;


    public int getDay() {
        return (int) ((overworld.getFullTime() - 18000) / 24000);
    }

    @Override
    public void onEnable() {
        Bukkit.broadcastMessage("��4�� SMP : THE NIGHTMARE �� -  Reloaded.");
        Bukkit.getPluginManager().registerEvents(this, this);
        if (!new File(getDataFolder(), "config.yml").exists()) {
            saveDefaultConfig();
        }
        elytraDamage = config.getInt("���� ���� ������ ���ҷ�");
        bedBreakPeriod = config.getInt("ħ�� �ı� �ֱ� (��)");
        compassBreakPeriod = config.getInt("�ڼ��� ���� �ֱ� (��)");
        wolfEnhancePeriod = config.getInt("���� ��ȭ �ֱ� (��)");
        wolfEnhanceAmount = config.getInt("���� ��ȭ ���");
        phantomSpawnPeriod = config.getInt("���� ���� �ֱ� (��)");
        phantomSpawnDelay = config.getInt("���� ���� ���� (��)");
        phantomSpawnCount = config.getInt("���� ���� ��");
        hostileHealth = config.getInt("���� ü�� ���");
        hostileStrength = config.getInt("���� ���ݷ� ���");
        undeadHealth = config.getInt("�𵥵� ü�� ���");
        undeadStrength = config.getInt("�𵥵� ���ݷ� ���");
        netherHealth = config.getInt("��������/������ ü�� ���");
        netherStrength = config.getInt("��������/������ ���ݷ� ���");
        dayLength = config.getInt("�Ϸ� ���� (ƽ)");

        for (World w : Bukkit.getWorlds()) {
            if (w.getEnvironment() == World.Environment.NORMAL) {
                overworld = w;
            } else if (w.getEnvironment() == World.Environment.NETHER) {
                netherworld = w;
            } else if (w.getEnvironment() == World.Environment.THE_END) {
                enderworld = w;
            }
        }

        //�⺻����
        overworld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        overworld.setGameRule(GameRule.REDUCED_DEBUG_INFO, true);
        netherworld.setGameRule(GameRule.REDUCED_DEBUG_INFO, true);
        enderworld.setGameRule(GameRule.REDUCED_DEBUG_INFO, true);
        overworld.setFullTime(18000);

        //�Ϸ� Ÿ�̸�
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            int day = config.getInt("��¥");
            config.set("��¥", day + 1);
            try {
                config.save(configfile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            overworld.setFullTime(day * 24000L + 18000);
            Bukkit.broadcastMessage("��eDAY " + getDay());

            //ħ�� ����
            for (String name : beddata.getKeys(false)) {
                if (getDay() == beddata.getInt(name) + bedBreakPeriod) {
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

            //�ڼ��� �ʱ�ȭ
            for (String name : compassdata.getKeys(false)) {
                if (getDay() == compassdata.getInt(name) + compassBreakPeriod) {
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

        }, 0, dayLength);

        //���� ���� ����
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                Location l = p.getLocation();
                if (l.getBlockY() >= l.getWorld().getHighestBlockYAt(l)) {
                    for (int i = 0; i < phantomSpawnCount; i++) {
                        Phantom ph = (Phantom) overworld.spawnEntity(p.getLocation().add(rand.nextInt(30) - 15, 30, rand.nextInt(30) - 15), EntityType.PHANTOM);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (p.isDead()) {
                                    ph.remove();
                                    cancel();
                                } else if (ph.isDead()) {
                                    cancel();
                                } else {
                                    ph.setTarget(p);
                                }
                            }
                        }.runTaskTimer(this, 0, 20);
                    }
                }
            }
        }, phantomSpawnDelay * 24000L, phantomSpawnPeriod * 20L);

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
            double healthmodifer = hostileHealth;
            double strengthmodifer = hostileStrength;
            if (en.getCategory() == EntityCategory.UNDEAD) {
                healthmodifer = undeadHealth;
                strengthmodifer = undeadStrength;
            }
            if (en.getType() == EntityType.WITHER_SKELETON || en.getType() == EntityType.BLAZE) {
                healthmodifer = netherHealth;
                strengthmodifer = netherStrength;
            }
            if (attributehealth != null) {
                double health = attributehealth.getBaseValue() * healthmodifer;
                attributehealth.setBaseValue(health);
                en.setHealth(health);
            }
            if (attributedamage != null) {
                attributedamage.setBaseValue(attributedamage.getBaseValue() * strengthmodifer);
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
            if (damage >= 431 - elytraDamage) {
                dm.setDamage(431);
            } else {
                dm.setDamage(damage + elytraDamage);
            }
            cp.setItemMeta(dm);
        }
    }

    @EventHandler
    public void wolfEnhance(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Wolf) {
            e.setDamage(e.getDamage() * getDay() / wolfEnhancePeriod * wolfEnhanceAmount);
        }
    }

}
