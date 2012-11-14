package me.com.hutattedonmyarm.Mita.MitaTowns; 
import java.sql.ResultSet;
import java.util.logging.Logger;
import lib.net.darqy.SQLib.SQLite;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class MitaTowns extends JavaPlugin implements Listener {
	
	private ConsoleCommandSender console;
	private Logger logger = Bukkit.getServer().getLogger();
	private String databaseName = "MitaTownsDB.db";
	private SQLite sqlite = new SQLite(this.logger, "[MitaTowns]", databaseName, "plugins/MitaTowns/");
	public static Economy economy = null;
 
	public void onEnable() {
		this.console = Bukkit.getServer().getConsoleSender();
		this.console.sendMessage("[MitaTowns] Enabling MitaTowns...");
		Plugin worldEdit = getServer().getPluginManager().getPlugin("WorldEdit");
		if (worldEdit == null) {
			this.console.sendMessage(ChatColor.RED + "Error! WorldEdit not found!");
		}
		getServer().getPluginManager().registerEvents(this, this);
		this.sqlite.open();
		setupDatabase();
		setupEconomy();
		saveConfig();
		saveDefaultConfig();
	}
	public void onDisable() {
		this.sqlite.close();
	}
	private boolean setupEconomy()
    {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }
	private void setupDatabase() {
		if (!this.sqlite.tableExists("towns")) {
			String query = "CREATE TABLE towns (townid INTEGER PRIMARY KEY, townname TEXT, founder INTEGER, mayor INTEGER, homePointX INTEGER, homePointY INTEGER, homePointZ INTEGER, edge1X INTEGER, edge1Z INTEGER, edge2X INTEGER, edge2Z INTEGER, world TEXT)";
			this.sqlite.modifyQuery(query);
		}
		if (!this.sqlite.tableExists("PlayerTowns")) {
			String query = "CREATE TABLE PlayerTowns (ptid INTEGER PRIMARY KEY, userid INTEGER, townid INTEGER)";
			this.sqlite.modifyQuery(query);
		}
		if (!this.sqlite.tableExists("users")) {
			String query = "CREATE TABLE users (userid INTEGER PRIMARY KEY, username TEXT)";
			this.sqlite.modifyQuery(query);
		}
	}
	private void playerOnly(CommandSender sender) { sender.sendMessage(ChatColor.RED + "Only players can use this command"); }
	private void noPermission(CommandSender sender, Command cmd, String[] args) {
		sender.sendMessage(ChatColor.RED + "You don't have permission to do that. This incident will be logged!");
		String argString = "";
		for (int i = 0; i < args.length; i++) {
			argString = argString + args[i] + " ";
		}
		this.console.sendMessage(sender.getName() + " was denied access to command /" + cmd.getLabel() + " " + argString);
		Bukkit.getServer().broadcast(sender.getName() + " was denied access to command /" + cmd.getLabel() + " " + argString, "MitaTowns.watchPerms");
	}
	private void noMoney(Player p, double needed, double has) {
		p.sendMessage(ChatColor.RED + "You don't have enough money. You need " + (needed-has) + " more");
	}
	private boolean isLocIn2DArea(Location loc, Location edge1, Location edge2) {
		return (
				(((loc.getX() < edge1.getX()) && (loc.getX() > edge2.getX())) || 
						((loc.getX() > edge1.getX()) && (loc.getX() < edge2.getX()))) &&
				(((loc.getZ() < edge1.getZ()) && (loc.getZ() > edge2.getZ())) || 
						((loc.getZ() > edge1.getZ()) && (loc.getZ() < edge2.getZ())))
			);
	}
	private boolean isLocIn3DArea(Location loc, Location edge1, Location edge2) {
		return (
				(((loc.getX() < edge1.getX()) && (loc.getX() > edge2.getX())) || 
						((loc.getX() > edge1.getX()) && (loc.getX() < edge2.getX()))) &&
				(((loc.getZ() < edge1.getZ()) && (loc.getZ() > edge2.getZ())) || 
						((loc.getZ() > edge1.getZ()) && (loc.getZ() < edge2.getZ()))) &&
				(((loc.getY() < edge1.getY()) && (loc.getY() > edge2.getY())) || 
						((loc.getY() > edge1.getY()) && (loc.getY() < edge2.getY())))
			);
	}
	private String colorize(String s){
    	if(s == null) return null;
    	return s.replaceAll("&([0-9a-f])", "\u00A7$1");
    }
	@EventHandler
	public void playerJoin(PlayerJoinEvent evt) {
		Player p = evt.getPlayer();
		ResultSet rs = sqlite.readQuery("SELECT userid FROM users WHERE username = '" + p.getName() + "'");
		try {
			if(rs != null && !rs.next()) { //User doesn't exist in DB, so they joined the first time, We'll add them
				sqlite.modifyQuery("INSERT INTO users (username) VALUES ('" + p.getName() + "')");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private boolean newTown(CommandSender sender, Command cmd, String label, String[] args) {
		Player p = null;
		if ((sender instanceof Player)) {
			p = (Player)sender;
		} else {
			playerOnly(sender);
			return true;
		}
		if (!p.hasPermission("MitaTowns.town.new")) {
			noPermission(sender, cmd, args);
			return true;
		}
		if(args.length < 2) return false;
		if(!economy.has(p.getName(), getConfig().getDouble("new_town.cost"))) {
			noMoney(p, getConfig().getDouble("new_town.cost"), economy.getBalance(p.getName()));
			return true;
		}
		int id = 1;
		ResultSet rs = sqlite.readQuery("SELECT userid FROM users WHERE username = '" + p.getName() + "'");
		try {
			id = rs.getInt("userid");
		} catch (Exception e) {
			id = -1;
		}
		 rs = this.sqlite.readQuery("SELECT mayor FROM towns WHERE townid = (SELECT townid FROM PlayerTowns WHERE userid = '" + id + "')");
		try {
			while (rs.next()) {
				if (rs.getInt("mayor") == id) {
					p.sendMessage(ChatColor.RED + "You're already mayor of a town and cannot found another one");
					return true;
				}
			}
		}
		catch (Exception localException1) { }
		Location l = p.getLocation();
		int size_X = getConfig().getInt("new_town.size.x-Dir");
		int size_Z = getConfig().getInt("new_town.size.z-Dir");
		Location edge1 = new Location(l.getWorld(), l.getX()-size_X/2, l.getY(), l.getZ()-size_Z/2);
		Location edge2 = new Location(l.getWorld(), l.getX()-size_X/2, l.getY(), l.getZ()+size_Z/2);
		Location edge3 = new Location(l.getWorld(), l.getX()+size_X/2, l.getY(), l.getZ()-size_Z/2);
		Location edge4 = new Location(l.getWorld(), l.getX()+size_X/2, l.getY(), l.getZ()+size_Z/2);
		rs = sqlite.readQuery("SELECT edge1X, edge1Z, edge2X, edge2Z FROM towns WHERE world = '" + l.getWorld().getName() + "'");
		try {
			int te1X = rs.getInt("edge1X");
			int te2X = rs.getInt("edge2X");
			int te1Z = rs.getInt("edge1Z");
			int te2Z = rs.getInt("edge2Z");
			Location e1 = new Location(edge1.getWorld(), te1X, 42, te1Z);
			Location e2 = new Location(edge2.getWorld(), te2X, 42, te2Z);
			if(isLocIn2DArea(edge1, e1, e2) || isLocIn2DArea(edge2, e1, e2) || isLocIn2DArea(edge3, e1, e2) || isLocIn2DArea(edge4, e1, e2)) {
				sender.sendMessage(ChatColor.RED + "There is already another town in your way");
				return true;
			}
		} catch (Exception e) {
		}
		this.sqlite.modifyQuery("INSERT INTO towns (townname, founder, mayor, homePointX, homePointZ, homePointY, edge1X, edge1Z, edge2X, edge2Z, world) VALUES ('" + args[1] + "', '" + id + "', '" + id + "', '" + l.getBlockX() + "', '" + l.getBlockX() + "','" + l.getBlockX() + "', '" + (l.getBlockX() - size_X / 2) + "', '" + (l.getBlockZ() - size_Z / 2) + "', '" + (l.getBlockX() + size_X / 2) + "', '" + (l.getBlockZ() + size_Z / 2) + "', '"+l.getWorld().getName()+"')");
		rs = this.sqlite.readQuery("SELECT townid FROM towns WHERE townname = '" + args[1] + "'");
		try {
			this.sqlite.modifyQuery("INSERT INTO PlayerTowns (townid, userid) VALUES ('" + rs.getInt("townid") + "', '" + id + "')");
		} catch (Exception e) { e.printStackTrace(); }
		return true;
	}
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("town")) {
			if(args.length == 0) {
				
			} else {
				if(args[0].equalsIgnoreCase("town")) {
					return newTown(sender, cmd, label, args);
				} else {
					
				}
			}
		}
		return true;
	}
}