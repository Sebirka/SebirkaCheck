package ban.kollycheck;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class SebirkaCheck extends JavaPlugin implements CommandExecutor, Listener {
    private final Map<Player, Player> checkRequests = new HashMap<>();
    private static final String NOTIFY_PERMISSION = "sebirka.notify";

    @Override
    public void onEnable() {
        getCommand("check").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Команды доступны только игрокам!");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("sebirka.check")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав для использования этой команды.");
            return true;
        }
        if (command.getName().equalsIgnoreCase("check")) {
            if (args.length < 1) {
                player.sendMessage(ChatColor.GOLD + "[SebirkaCheck]" + ChatColor.GREEN + " Команды проверки на читы:");
                player.sendMessage(ChatColor.GOLD + "[SebirkaCheck]" + ChatColor.YELLOW + " /check ник - вызвать игрока на проверку на читы.");
                player.sendMessage(ChatColor.GOLD + "[SebirkaCheck]" + ChatColor.YELLOW + " /check ник allow - разрешить игроку после успешной проверки.");
                player.sendMessage(ChatColor.GOLD + "[SebirkaCheck]" + ChatColor.YELLOW + " /check ник dis - забанить игрока за использование читов.");
                player.sendMessage(ChatColor.GOLD + "[SebirkaCheck]" + ChatColor.YELLOW + " Создатели: Sebirka,m1xss");
                return true;
            }
            Player targetPlayer = Bukkit.getServer().getPlayer(args[0]);
            if (targetPlayer == null) {
                player.sendMessage(ChatColor.RED + "Игрок с ником " + args[0] + " не найден.");
                return true;
            }
            targetPlayer.setGameMode(GameMode.SURVIVAL);
            targetPlayer.setInvulnerable(true);
            if (args.length == 1) {
                Player moderator = player;
                checkRequests.put(targetPlayer, moderator);
                Location moderatorLocation = moderator.getLocation();
                targetPlayer.teleport(moderatorLocation);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission(NOTIFY_PERMISSION)) {
                        p.sendMessage(ChatColor.GRAY + "Модератор " + ChatColor.YELLOW + moderator.getName()
                                + ChatColor.GRAY + " вызвал игрока " + ChatColor.YELLOW + targetPlayer.getName()
                                + ChatColor.GRAY + " на проверку на читы.");
                    }
                }
                targetPlayer.sendMessage(ChatColor.GREEN + " ");
                targetPlayer.sendMessage(ChatColor.GREEN + "Вы вызваны на проверку на читы.");
                targetPlayer.sendMessage(ChatColor.GREEN + "Напишите свой дискорд, чтобы модератор связался с вами и провел проверку.");
                targetPlayer.sendMessage(ChatColor.GREEN + " ");
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (checkRequests.containsKey(targetPlayer)) {
                            targetPlayer.sendMessage(ChatColor.GREEN + " ");
                            targetPlayer.sendMessage(ChatColor.GREEN + "Вы вызваны на проверку на читы.");
                            targetPlayer.sendMessage(ChatColor.GREEN + "Напишите свой дискорд, чтобы модератор связался с вами и провел проверку.");
                            targetPlayer.sendMessage(ChatColor.GREEN + " ");
                        } else {
                            cancel();
                        }
                    }
                }.runTaskTimer(this, 0L, 100L);
            } else if (args.length == 2 && args[1].equalsIgnoreCase("allow")) {
                if (checkRequests.containsKey(targetPlayer)) {
                    Player moderator = checkRequests.get(targetPlayer);
                    checkRequests.remove(targetPlayer);
                    moderator.sendMessage(ChatColor.GREEN + "Игрок " + targetPlayer.getName() + " успешно прошел проверку на читы.");
                    targetPlayer.sendMessage(ChatColor.GREEN + "Вы успешно прошли проверку на читы.");
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.hasPermission(NOTIFY_PERMISSION)) {
                            p.sendMessage(ChatColor.GRAY + "Модератор " + ChatColor.YELLOW + moderator.getName()
                                    + ChatColor.GRAY + " закончил проверку игрока " + ChatColor.YELLOW + targetPlayer.getName()
                                    + ChatColor.GRAY + " и не обнаружил читов.");
                        }
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Игрок не находится на проверке.");
                }
                if (command.getName().equalsIgnoreCase("checkso")) {
                    String banCommand = "litebans:ban " + player.getName() + " 47d признание на проверке";
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), banCommand);
                    player.sendMessage(ChatColor.RED + "Вы признались в использовании читов на проверке. Вы были забанены.");
                }
            } else if (args.length == 2 && args[1].equalsIgnoreCase("dis")) {
                if (checkRequests.containsKey(targetPlayer)) {
                    Player moderator = checkRequests.get(targetPlayer);
                    checkRequests.entrySet().removeIf(entry -> entry.getValue().equals(player));
                    String banCommand = "litebans:ban " + targetPlayer.getName() + " 60d читы";
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), banCommand);
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.hasPermission(NOTIFY_PERMISSION)) {
                            p.sendMessage(ChatColor.GRAY + "Модератор " + ChatColor.YELLOW + moderator.getName()
                                    + ChatColor.GRAY + " закончил проверку игрока " + ChatColor.YELLOW + targetPlayer.getName()
                                    + ChatColor.GRAY + " и обнаружил читы. Игрок забанен.");
                        }
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Вы не можете использовать эту команду. Игрок не находится на проверке или вы не вызывали его.");
                }
            } else {
                player.sendMessage(ChatColor.RED + "Использование: /check <ник> [allow|dis]");
            }
        }
        return false;
    }



    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (checkRequests.containsKey(player)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
                player.teleport(from);
            }
        }
    }
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (checkRequests.containsKey(player)) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (checkRequests.containsKey(player)) {
            Player moderator = getModeratorForPlayer(player);
            if (moderator != null) {
                String banCommand = "litebans:ban " + player.getName() + " 60d лив с проверки";
                moderator.performCommand(banCommand);
                notifyModeratorActions(moderator, player, "Игрок вышел с сервера и был забанен.");
                checkRequests.remove(player);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();
            if (checkRequests.containsKey(damager)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (checkRequests.containsKey(player)) {
            event.setCancelled(true);
            player.setHealth(player.getMaxHealth());
        }
    }

    private Player getModeratorForPlayer(Player player) {
        for (Map.Entry<Player, Player> entry : checkRequests.entrySet()) {
            if (entry.getKey().equals(player)) {
                return entry.getValue();
            }
        }
        return null;
    }
    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (checkRequests.containsKey(player)) {
            player.sendMessage(ChatColor.RED + "Вы не можете использовать команды во время проверки");
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (checkRequests.containsKey(player)) {
            player.sendMessage(ChatColor.RED + "Вы не можете ломать блоки во время проверки.");
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (checkRequests.containsKey(player)) {
            player.sendMessage(ChatColor.RED + "Вы не можете ставить блоки во время проверки.");
            event.setCancelled(true);
        }
    }


    private void notifyModeratorActions(Player moderator, Player targetPlayer, String message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(NOTIFY_PERMISSION)) {
                p.sendMessage(ChatColor.GRAY + "Модератор " + ChatColor.YELLOW + moderator.getName()
                        + ChatColor.GRAY + " провел проверку игрока " + ChatColor.YELLOW + targetPlayer.getName()
                        + ChatColor.GRAY + " и обнаружил читы. " + message);
            }
        }
    }
}