package me.com.hutattedonmyarm.Mita.MitaTowns; 
import java.sql.ResultSet;
import java.util.logging.Logger;
import lib.net.darqy.SQLib.SQLite;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
public class MitaTowns extends JavaPlugin {
	
	private ConsoleCommandSender console;
	private Logger logger = Bukkit.getServer().getLogger();
	private String databaseName = "MitaBaseDB.db";
	private SQLite sqlite = new SQLite(this.logger, "[MitaBase]", this.databaseName, "plugins/MitaTowns/");
	private SQLite mbsqlite = new SQLite(this.logger, "[MitaBase]", this.databaseName, "plugins/MitaBase/");
 
	public void onEnable() {
		this.console = Bukkit.getServer().getConsoleSender();
		this.console.sendMessage("[MitaTowns] Enabling MitaTowns...");
		Plugin worldEdit = getServer().getPluginManager().getPlugin("WorldEdit");
		if (worldEdit == null) {
			this.console.sendMessage(ChatColor.RED + "Error! WorldEdit not found!");
		}
		this.sqlite.open();
		setupDatabase();
		saveDefaultConfig();
	}
	public void onDisable() {
		this.sqlite.close();
	}
	private void setupDatabase() {
		if (!this.sqlite.tableExists("towns")) {
			String query = "CREATE TABLE towns (townid INTEGER PRIMARY KEY, townname TEXT, founder INTEGER, mayor INTEGER, homePointX INTEGER, homePointY INTEGER, homePointZ INTEGER, edge1X INTEGER, edge1Z INTEGER, edge2X INTEGER, edge2Z INTEGER)";
			this.sqlite.modifyQuery(query);
		}
		if (!this.sqlite.tableExists("PlayerTowns")) {
			String query = "CREATE TABLE PlayerTowns (ptid INTEGER PRIMARY KEY, userid INTEGER, townid INTEGER)";
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
		}
		int id = -1;
		ResultSet rs = mbsqlite.readQuery("SELECT userid FROM users WHERE username = '" + p.getName() + "'");
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
		int size_X = getConfig().getInt("new_town_size.x-Dir");
		int size_Z = getConfig().getInt("new_town_size.z-Dir");
		this.sqlite.modifyQuery("INSERT INTO towns (townname, founder, mayor, homePointX, homePointZ, homePointY, edge1X, edge1Z, edge2X, edge2Z) VALUES ('" + args[1] + "', '" + id + "', '" + id + "', '" + l.getBlockX() + "', '" + l.getBlockX() + "','" + l.getBlockX() + "', '" + (l.getBlockX() - size_X / 2) + "', '" + (l.getBlockZ() - size_Z / 2) + "', '" + (l.getBlockX() + size_X / 2) + "', '" + (l.getBlockZ() + size_Z / 2) + "')");
		rs = this.sqlite.readQuery("SELECT townid FROM towns WHERE townname = '" + args[1] + "'");
		try {
			this.sqlite.modifyQuery("INSERT INTO PlayerTowns (townid, userid) VALUES ('" + rs.getInt("townid") + "', '" + id + "')");
		} catch (Exception e) { e.printStackTrace(); }
		return true;
	}
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("town")) {
			switch (args.length) {
			case 0:
				break;
			case 1:
				break;
			case 2:
				if (args[0].equalsIgnoreCase("new")) return newTown(sender, cmd, label, args);
				break;
			}
		}
		return true;
	}
}