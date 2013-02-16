package me.com.hutattedonmyarm.Mita.MitaTowns; 
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import lib.net.darqy.SQLib.SQLite;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class MitaTowns extends JavaPlugin implements Listener {
	
	private ConsoleCommandSender console;
	private Logger logger = Bukkit.getServer().getLogger();
	private String databaseName = "MitaTownsDB.db";
	private SQLite sql = new SQLite(this.logger, "[MitaTowns]", databaseName, "plugins/MitaTowns/");
	public static Economy economy = null;
 
	public void onEnable() {
		console = Bukkit.getServer().getConsoleSender();
		console.sendMessage("[MitaTowns] Enabling MitaTowns...");
		getServer().getPluginManager().registerEvents(this, this);
		sql.open();
		setupDatabase();
		setupEconomy();
		saveConfig();
		saveDefaultConfig();
	}
	public void onDisable() {
		this.sql.close();
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
		if (!sql.tableExists("towns")) {
			sql.modifyQuery("CREATE TABLE towns (townid INTEGER PRIMARY KEY, townname TEXT,  mayor INTEGER, homePointX INTEGER, homePointY INTEGER, homePointZ INTEGER, corner1X INTEGER, corner1ZZ INTEGER, corner3X INTEGER, corner3Z INTEGER, world TEXT)");
		}
		if (!sql.tableExists("PlayerTowns")) {
			sql.modifyQuery("CREATE TABLE PlayerTowns (ptid INTEGER PRIMARY KEY, playerid INTEGER, townid INTEGER)");
		}
		if (!sql.tableExists("players")) {
			sql.modifyQuery("CREATE TABLE players (playerid INTEGER PRIMARY KEY, username TEXT, assistantOfTown INTEGER, friendBuild INTEGER, friendDestroy INTEGER, friendUse INTEGER)");
		}
		if (!sql.tableExists("plots")) {
			//if it's a townplot, the ownerid = townid
			sql.modifyQuery("CREATE TABLE plots (plotid INTEGER PRIMARY KEY, ownerid INTEGER, corner1X INTEGER, corner1Z INTEGER, corner1X INTEGER, corner1Z INTEGER, type INTEGER)");
		}
		if(!sql.tableExists("friendships")) {
			sql.modifyQuery("CREATE TABLE friendships (friendshipid INTEGER PRIMARY KEY)");
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
	private void dispTownHelp(CommandSender sender) {
		//TODO write help
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
	private String colorize(String s){
    	if(s == null) return null;
    	return s.replaceAll("&([0-9a-f])", "\u00A7$1");
    }
	private int isMayorOrAdmin(Player p, String permission, boolean useAssistants) {
		//TODO implement! 0: No Mayor/Admin/Assistant, -1: Admin; Everything else: townid; If Admin AND Mayor,the townid is returned
		return 0;
	}
	
	@EventHandler	
	public void playerJoin(PlayerJoinEvent evt) {
		Player p = evt.getPlayer();
		ResultSet rs = sql.readQuery("SELECT userid FROM players WHERE playername = '" + p.getName() + "'");
		try {
			if(rs != null && !rs.next()) { //User doesn't exist in DB, so they joined the first time, We'll add them
				sql.modifyQuery("INSERT INTO players (playername, assistantOfTown, friendBuild, friendDestroy, friendUse) VALUES ('" + p.getName() + "', 0, 0, 0, 0)");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player p = null;
		if(sender instanceof Player) {
			p = (Player) sender;
		}
		if (cmd.getName().equalsIgnoreCase("plot")) {
			
		} else if (cmd.getName().equalsIgnoreCase("res")) {
	
		} else if (cmd.getName().equalsIgnoreCase("town")) {
			if(args.length == 0 || args[0].equalsIgnoreCase("?") || args[0].equalsIgnoreCase("help")) { //Console
				if(p == null || p.hasPermission("MitaTowns.town.help")) {
					noPermission(sender, cmd, args);
				} else {
					dispTownHelp(sender);
				}
			} else if (args[0].equalsIgnoreCase("assistant")) { //Console
				manageAssistants(sender, cmd, args);
			} else if (args[0].equalsIgnoreCase("info")) { //Console
				
			} else if (args[0].equalsIgnoreCase("leave")) { //No console
				if(p == null) {
					playerOnly(sender);
				} else {
					
				}
			} else if (args[0].equalsIgnoreCase("new")) { //No console
				if(p == null) {
					playerOnly(sender);
				} else {
					
				}
			} else {
				return false;
			}
		}
		return true;
	}

	public void manageAssistants(CommandSender sender, Command cmd, String[] args) {
		Player p = null;
		if(sender instanceof Player) {
			p = (Player) sender;
		}
		if(args.length < 2) { //Display help. Console
			if(p == null || p.hasPermission("MitaTowns.assistant.help")) {
				dispAssistantHelp(sender);
			} else {
				noPermission(sender, cmd, args);
			}
		} else if(args[1].equalsIgnoreCase("add")) {
			if(p == null || (isMayorOrAdmin(p, "MitaTowns.manageAssistants", false)!= 0 && p.hasPermission("MitaTowns.assistant.add"))) {
				addAssistant(sender, args);
			} else {
				noPermission(sender, cmd, args);
			}
		} else if(args[1].equalsIgnoreCase("remove")) {
			if(p == null || (isMayorOrAdmin(p, "MitaTowns.manageAssistants", false) != 0 && p.hasPermission("MitaTowns.assistant.remove"))) {
				removeAssistant();
			} else {
				noPermission(sender, cmd, args);
			}
		} else if(args[1].equalsIgnoreCase("promote")) {
			if(p == null || (isMayorOrAdmin(p, "MitaTowns.manageAssistants", false) != 0 && p.hasPermission("MitaTowns.assistant.promote"))) {
				promoteAssistant();
			} else {
				noPermission(sender, cmd, args);
			}
		}
	}
	public void addAssistant(CommandSender sender, String[] args) {
		Player p = null;
		if(sender instanceof Player) {
			p = (Player) sender;
		}
		if(p == null || (p.hasPermission("MitaTowns.manageAssistans") && args.length >= 4)) { //Console and Admin. Syntax: /t assistant add <name> <town>
			if(args.length < 4) { 
				dispAssistantHelp(sender);
				return;
			}
			OfflinePlayer op = Bukkit.getOfflinePlayer(args[2]);
			if(op == null) { 
				sender.sendMessage(ChatColor.RED + "Player " + args[2] + " not found");
				return;
			}
			ResultSet rs = sql.readQuery("SELECT townid FROM towns WHERE townname = '" + args[3] + "'");
			int townid = -1;
			try {
				while (rs.next()) {
					townid = rs.getInt("townid");
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			if(townid == -1) {
				sender.sendMessage(ChatColor.RED + "Town " + args[3] + " not found");
			}
			promotePlayerToAssistant(op, townid);
		} else { //Player or Admin as Mayor. Syntax: /t assistant add <name>
			if(args.length < 3) { 
				dispAssistantHelp(sender);
				return;
			}
			OfflinePlayer op = Bukkit.getOfflinePlayer(args[2]);
			if(op == null) { 
				sender.sendMessage(ChatColor.RED + "Player " + args[2] + " not found");
				return;
			}
			int t = isMayorOrAdmin(p, "MitaTowns.manageAssistans", false);
			if(t == -1) {
				p.sendMessage("Please specify a town");
				return;
			} else if (t == 0) {
				p.sendMessage(ChatColor.RED + "You're not a major");
			}
			promotePlayerToAssistant(op, t);
		}
	}
	public void promotePlayerToAssistant(OfflinePlayer p, int townID) {
		sql.modifyQuery("UPDATE players SET assistantOfTown= '" + townID + "' WHERE username = '" + p.getName() + "'");
		return;
	}
	public void removeAssistant() {
		
	}
	public void promoteAssistant() {
	
	}
	public void dispAssistantHelp(CommandSender sender) {
		//TODO write help
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/*private boolean newTown(CommandSender sender, Command cmd, String label, String[] args) {
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
		ResultSet rs = sql.readQuery("SELECT townid FROM towns WHERE townname = '" + args[1] + "'");
		try {
			rs.getInt("townid");
			p.sendMessage(ChatColor.RED + "A town with this name already exists");
			return true;
		} catch (Exception e) {	}
		int id = -1;
		rs = sql.readQuery("SELECT userid FROM users WHERE username = '" + p.getName() + "'");
		try {
			id = rs.getInt("userid");
		} catch (Exception e) {
			id = -1;
		}
		 rs = this.sql.readQuery("SELECT mayor FROM towns WHERE townid = (SELECT townid FROM PlayerTowns WHERE userid = '" + id + "')");
		try {
			while (rs.next()) {
				if (rs.getInt("mayor") == id) {
					p.sendMessage(ChatColor.RED + "You're already mayor of a town and cannot found another one");
					return true;
				}
			}
		}
		catch (Exception e) { }
		Location l = p.getLocation();
		int size_X = getConfig().getInt("new_town.size.x-Dir");
		int size_Z = getConfig().getInt("new_town.size.z-Dir");
		Location edge1 = new Location(l.getWorld(), l.getX()-size_X/2, l.getY(), l.getZ()-size_Z/2);
		Location edge2 = new Location(l.getWorld(), l.getX()-size_X/2, l.getY(), l.getZ()+size_Z/2);
		Location edge3 = new Location(l.getWorld(), l.getX()+size_X/2, l.getY(), l.getZ()-size_Z/2);
		Location edge4 = new Location(l.getWorld(), l.getX()+size_X/2, l.getY(), l.getZ()+size_Z/2);
		rs = sql.readQuery("SELECT edge1X, edge1Z, edge2X, edge2Z FROM towns WHERE world = '" + l.getWorld().getName() + "'");
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
		this.sql.modifyQuery("INSERT INTO towns (townname, founder, mayor, homePointX, homePointZ, homePointY, edge1X, edge1Z, edge2X, edge2Z, world) VALUES ('" + args[1] + "', '" + id + "', '" + id + "', '" + l.getBlockX() + "', '" + l.getBlockX() + "','" + l.getBlockX() + "', '" + (l.getBlockX() - size_X / 2) + "', '" + (l.getBlockZ() - size_Z / 2) + "', '" + (l.getBlockX() + size_X / 2) + "', '" + (l.getBlockZ() + size_Z / 2) + "', '"+l.getWorld().getName()+"')");
		rs = this.sql.readQuery("SELECT townid FROM towns WHERE townname = '" + args[1] + "'");
		try {
			this.sql.modifyQuery("INSERT INTO PlayerTowns (townid, userid) VALUES ('" + rs.getInt("townid") + "', '" + id + "')");
			sql.modifyQuery("INSERT INTO flatplots (ownerid, edge1X, edge1Z, edge2X, edge2Z, type, isTown) VALUES ('" + rs.getInt("townid") + "', " + edge1.getBlockX() + ", " + edge1.getBlockZ() + ", " + edge4.getBlockX() + ", " + edge4.getBlockZ() + ", 'Town', '1')");
		} catch (Exception e) { e.printStackTrace(); }
		sender.sendMessage(ChatColor.GREEN + "You're now founder and mayor of " + args[1]);
		return true;
	}
	private void dispTown(CommandSender sender, String townname) {
		String mayor, founder, inhabitans = null;
		ResultSet rs = sql.readQuery("SELECT username FROM users, towns WHERE userid = mayor AND townname = '" + townname +"'");
		try {
			mayor = rs.getString("username");
			rs = sql.readQuery("SELECT username FROM users, towns WHERE userid = founder AND townname = '" + townname +"'");
			founder = rs.getString("username");
			rs = sql.readQuery("SELECT username FROM users, towns, PlayerTowns WHERE users.userid = PlayerTowns.userid AND PlayerTowns.townid = towns.townid AND townname = '" + townname +"'");
			while (rs.next()) {
				inhabitans += rs.getString("username") + ", ";
			}
			inhabitans = inhabitans.substring(0, inhabitans.length()-2);
			sender.sendMessage(ChatColor.UNDERLINE + "" + ChatColor.BLUE + "Town: " + townname);
			sender.sendMessage(ChatColor.DARK_RED + "Founder: " + founder);
			sender.sendMessage(ChatColor.YELLOW + "Mayor: " + mayor);
			sender.sendMessage(ChatColor.BLUE + "Inhabitans: " + inhabitans);
		} catch (Exception e) {
			
		}
	}
	private boolean manageAssistants(CommandSender sender, Command cmd, String label, String[] args) {
		if(args.length == 1) {
			return false;
		}
		Player p = null;
		if ((sender instanceof Player)) {
			p = (Player)sender;
		}
		if (args.length == 2) {
			if(args[1].equalsIgnoreCase("show")) { //t assistant show; Admin/Mayor/Assi only!
				if(p == null) { playerOnly(sender); }
				if (!isMayorOrAdmin(p, "MitaTowns.manageAssistans", true))p.sendMessage(ChatColor.RED + "You're not the mayor, an assistant or an admin");
				ResultSet rs = sql.readQuery("SELECT username FROM users, towns, PlayerTowns WHERE users.userid = PlayerTowns.userid AND towns.townid = PlayerTowns.townid AND users.assistant = towns.townid AND towns.townname = (SELECT townname FROM towns, PlayerTowns WHERE towns.townid = PlayerTowns.townid AND Playertowns.userid = (SELECT userid FROM users WHERE username = '" + sender.getName() + "'))");
				try {
					String a = "";
					while (rs.next()) {
						a += rs.getString("username") + ", ";
					}
					a = a.substring(0, a.length()-2);
					p.sendMessage(ChatColor.BLUE + "Assistants: " + a);
				} catch (Exception e) {
					p.sendMessage("This town doesn't have any assistans");
				}
			} else {
				return false;
			}
		} else if(args.length == 3){
			if(args[1].equalsIgnoreCase("add")) { //t assistant add <name>; Admin/Mayor only!
				if(p == null) { playerOnly(sender); }
				if (!isMayorOrAdmin(p, "MitaTowns.manageAssistans", false))p.sendMessage(ChatColor.RED + "You're not the mayor or an admin");
				OfflinePlayer pl = getServer().getOfflinePlayer(args[2]);
				if(pl == null) {
					p.sendMessage(ChatColor.RED + "Player " + args[2] + " not found");
					return true;
				}
				ResultSet rs = sql.readQuery("SELECT userid FROM users, PlayerTowns WHERE username = '" + pl.getName() + "' AND users.userid = PlayerTowns.userid AND PlayerTowns.townid = (SELECT townid FROM PlayerTowns WHERE userid = (SELECT userid FROM users WHERE username = '" + p.getName() + "'))");
				try {
					rs.getInt("userid");
				} catch (Exception e) {
					p.sendMessage(ChatColor.RED + "Player " + pl.getName() + " is not in your town");
					return true;
				}
				sql.modifyQuery("UPDATE users SET assistant = (SELECT townid FROM PlayerTowns WHERE userid = (SELECT userid FROM users WHERE username = '" + p.getName() + "')) WHERE username = '" + pl.getName() + "'");
				p.sendMessage(ChatColor.BLUE + pl.getName() + " is now assistant of your town!");
			} else if(args[1].equals("remove")) { //t assistant remove <name>; Admin/Mayor/Console only!
				if (!(p == null || isMayorOrAdmin(p, "MitaTowns.manageAssistans", false)))p.sendMessage(ChatColor.RED + "You're not the mayor or an admin");
				sql.modifyQuery("UPDATE users SET assistant = '0' WHERE username = '" + args[2] + "'");
				sender.sendMessage(ChatColor.BLUE + args[0] + " is no longer an assistant");
			} else if(args[1].equals("show")) { //t assistant show <town>; Console/Admin only!
				if(!(p == null || p.hasPermission("MitaTowns.manageAssistans"))) {
					noPermission(sender, cmd, args);
					return true;
				}
				ResultSet rs = sql.readQuery("SELECT username FROM users, towns, PlayerTowns WHERE users.userid = PlayerTowns.userid AND towns.townid = PlayerTowns.townid AND users.assistant = towns.townid AND towns.townname = '" + args[2] + "'");
				try {
					String a = "";
					while (rs.next()) {
						a += rs.getString("username") + ", ";
					}
					a = a.substring(0, a.length()-2);
					p.sendMessage(ChatColor.BLUE + "Assistants: " + a);
				} catch (Exception e) {
					sender.sendMessage("This town doesn't have any assistans");
				}
			} else {
				return false;
			}
		} else {
			if(args[1].equalsIgnoreCase("add")) { //t assistant add <name> <town>; Console/Admin only!
				if(!(p == null || p.hasPermission("MitaTowns.manageAssistans"))) {
					noPermission(sender, cmd, args);
					return true;
				}
				OfflinePlayer pl = getServer().getOfflinePlayer(args[2]);
				if(pl == null) {
					p.sendMessage(ChatColor.RED + "Player " + args[2] + " not found");
					return true;
				}
				String tname = args[3];
				ResultSet rs = sql.readQuery("SELECT userid FROM PlayerTowns WHERE townid = (SELECT townid FROM towns WHERE townname = '" + tname + "') AND userid = (SELECT userid FROM users WHERE username = '" + pl.getName() + "')");
				try {
					rs.getInt("userid");
				} catch (Exception e) {
					sender.sendMessage(ChatColor.RED + "Player " + pl.getName() + " is not in " + tname);
					return true;
				}
				sql.modifyQuery("UPDATE users SET assistant = (SELECT townid FROM towns WHERE townname = '" + tname + "') WHERE username = '" + pl.getName() + "'");
				sender.sendMessage(ChatColor.BLUE + pl.getName() + " is now assistant of " + tname);
			} else {
				return false;
			}
		}
		return true;
	}
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("town")) {
			if(args.length == 0) {
				Player p = null;
				ResultSet rs = null;
				if ((sender instanceof Player)) {
					p = (Player)sender;
					rs = sql.readQuery("SELECT townname FROM towns AS t, users AS u, PlayerTowns as pt WHERE t.townid = pt.townid AND u.userid = pt.userid AND u.username = ' " + p.getName() + "'");
				} else {
					playerOnly(sender);
					return false;
				}
				try {
					dispTown(sender, rs.getString("townname"));
				} catch (Exception e) {
					
				}
			} else {
				if(args[0].equalsIgnoreCase("new")) {
					return newTown(sender, cmd, label, args);
				} else if(args[0].equalsIgnoreCase("assistant")) {
					return manageAssistants(sender, cmd, label, args);
				}  else if(args[0].equalsIgnoreCase("?") || args[0].equalsIgnoreCase("help")) {
					return manageAssistants(sender, cmd, label, args);
				} else {
					return true;
				}
			}
		}
		return true;
	}*/
}