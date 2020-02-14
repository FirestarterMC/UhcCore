package com.gmail.val59000mc.customitems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.gmail.val59000mc.configuration.MainConfiguration;
import com.gmail.val59000mc.exceptions.UhcPlayerDoesntExistException;
import com.gmail.val59000mc.game.GameManager;
import com.gmail.val59000mc.languages.Lang;
import com.gmail.val59000mc.players.UhcPlayer;
import com.gmail.val59000mc.players.UhcTeam;
import com.gmail.val59000mc.utils.CompareUtils;
import com.gmail.val59000mc.utils.UniversalMaterial;
import com.gmail.val59000mc.utils.VersionUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class UhcItems {
	
	public static void giveLobbyItemTo(Player player){
		MainConfiguration cfg = GameManager.getGameManager().getConfiguration();
		if (cfg.getMaxPlayersPerTeam() > 1 || !cfg.getTeamAlwaysReady()) {
			ItemStack item = new ItemStack(Material.BOOK);
			ItemMeta meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&d&lChoose Teams"));
			meta.setLore(Arrays.asList(ChatColor.GRAY + "Right-click to view", ChatColor.GRAY + "and join teams."));
			item.setItemMeta(meta);
			player.getInventory().addItem(item);
		}
	}

	public static void giveBungeeItemTo(Player player){
		GameManager gm = GameManager.getGameManager();
		if (gm.getConfiguration().getEnableBungeeSupport() && gm.getConfiguration().getEnableBungeeLobbyItem()){
			ItemStack barrier = new ItemStack(Material.BARRIER);
			ItemMeta barrierItemMeta = barrier.getItemMeta();
			barrierItemMeta.setDisplayName(Lang.ITEMS_BUNGEE);
			barrier.setItemMeta(barrierItemMeta);
			player.getInventory().setItem(8, barrier);
		}
	}

	public static void giveScenariosItemTo(Player player){
		ItemStack paper = new ItemStack(Material.PAPER);
		ItemMeta meta = paper.getItemMeta();
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6&lView Scenarios"));
		meta.setLore(Arrays.asList(ChatColor.GRAY + "View currently active", ChatColor.GRAY + "game scenarios."));
		paper.setItemMeta(meta);
		player.getInventory().addItem(paper);
	}

	public static boolean isLobbyItem(ItemStack item){
		return (
				item != null 
				&& item.getType().equals(Material.BOOK)
				&& item.getItemMeta().getDisplayName().contains("Choose Teams")
        );
	}

	public static boolean isScenariosHotbarItem(ItemStack item){
		return (
				item != null &&
						item.getType().equals(Material.PAPER) &&
						item.hasItemMeta() &&
						item.getItemMeta().hasDisplayName() &&
						item.getItemMeta().getDisplayName().contains("View Scenarios")
		);
	}

	public static boolean isLobbyBarrierItem(ItemStack item){
		return (
				item != null &&
						item.getType().equals(Material.BARRIER) &&
						item.hasItemMeta() &&
						item.getItemMeta().hasDisplayName() &&
						item.getItemMeta().getDisplayName().equals(Lang.ITEMS_BUNGEE)
		);
	}
	
	public static void openTeamInventory(Player player){
		int maxSlots = 5*9;
		Inventory inv = Bukkit.createInventory(null, maxSlots, Lang.TEAM_INVENTORY);
		int slot = 9;
		GameManager gm = GameManager.getGameManager();
		List<UhcTeam> teams = gm.getPlayersManager().listUhcTeams();
		for(UhcTeam team : teams){
			if(slot < maxSlots){
				ItemStack item = createTeamSkullItem(team);
				inv.setItem(slot, item);
				slot++;
			}
		}
		
		int[] glassLocations = {0, 1, 2, 3, 4, 5, 6, 7, 8, 36, 37, 38, 39, 41, 42, 43, 44};
		ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);

		for (int i = 0; i < glassLocations.length; i++) {
			inv.setItem(glassLocations[i], glass);
		}

		// Leave team item
		if(!gm.getConfiguration().getPreventPlayerFromLeavingTeam()){
			ItemStack leaveTeamItem = new ItemStack(Material.BARRIER);
			ItemMeta imLeave = leaveTeamItem.getItemMeta();
			imLeave.setDisplayName(ChatColor.RED+Lang.ITEMS_BARRIER);
			leaveTeamItem.setItemMeta(imLeave);
			inv.setItem(maxSlots-1, leaveTeamItem);
		}
		
		UhcPlayer uhcPlayer;
		try {
			uhcPlayer = gm.getPlayersManager().getUhcPlayer(player);
			

			// Team ready/not ready item
			if(uhcPlayer.isTeamLeader() && !gm.getConfiguration().getTeamAlwaysReady()){


				// Red Wool
				ItemStack readyTeamItem = new ItemStack(Material.ENDER_PEARL, 1);

				String readyState = ChatColor.RED + Lang.TEAM_NOT_READY;
				
				if(uhcPlayer.getTeam().isReadyToStart()){
					// Lime Wool
					readyTeamItem = new ItemStack(Material.EYE_OF_ENDER, 1);
					readyState = ChatColor.GREEN + Lang.TEAM_READY;
				}

				ItemMeta imReady = readyTeamItem.getItemMeta();
				imReady.setDisplayName(readyState);
				List<String> readyLore = new ArrayList<String>();
				readyLore.add(ChatColor.GRAY+Lang.TEAM_READY_TOGGLE);
				imReady.setLore(readyLore);
				readyTeamItem.setItemMeta(imReady);
				inv.setItem(40, readyTeamItem);
			}
			
			player.openInventory(inv);
		} catch (UhcPlayerDoesntExistException e) {
			e.printStackTrace();
		}
		
	}
	
	
	public static ItemStack createTeamSkullItem(UhcTeam team){
		UhcPlayer leader = team.getLeader();
		String leaderName = leader.getName();
		ItemStack item = VersionUtils.getVersionUtils().createPlayerSkull(leaderName, leader.getUuid());
		List<String> membersNames = team.getMembersNames();
		item.setAmount(membersNames.size());
		ItemMeta im = item.getItemMeta();
		
		// Setting up lore with team members
		List<String> teamLore = new ArrayList<String>();
		teamLore.add(ChatColor.YELLOW + "Members:");
		for(String teamMember : membersNames){
			teamLore.add(ChatColor.GRAY + teamMember);
		}
		
		// Ready State
		if(team.isReadyToStart())
			teamLore.add(ChatColor.DARK_GRAY+"- "+ ChatColor.GREEN + Lang.TEAM_READY);
		else
			teamLore.add(ChatColor.DARK_GRAY+"- "+ ChatColor.RED + Lang.TEAM_NOT_READY);
		
		im.setLore(teamLore);
		im.setDisplayName(ChatColor.GRAY + leaderName + "'s Team");
		item.setItemMeta(im);
		return item;
	}
	
	public static boolean isLobbyTeamItem(ItemStack item){
		if(item != null && item.getType() == UniversalMaterial.PLAYER_HEAD.getType()){
			List<String> lore = item.getItemMeta().getLore();
			return CompareUtils.stringListContains(lore, ChatColor.YELLOW+"Members:") || CompareUtils.stringListContains(lore, Lang.TEAM_REQUEST_HEAD);
		}
		return false;
	}
	
	public static boolean isLobbyLeaveTeamItem(ItemStack item){
			return (
					item != null 
					&& item.getType() == Material.BARRIER
					&& item.hasItemMeta()
					&& item.getItemMeta().getDisplayName().equals(ChatColor.RED+Lang.ITEMS_BARRIER)
					);
	}
	


	public static boolean isLobbyReadyTeamItem(ItemStack item) {
		return (
				item != null 
				&& (item.getType().equals(Material.ENDER_PEARL) || item.getType().equals(Material.EYE_OF_ENDER))
				&& item.hasItemMeta()
				&& (item.getItemMeta().getDisplayName().equals(ChatColor.RED+Lang.TEAM_NOT_READY)
						|| item.getItemMeta().getDisplayName().equals(ChatColor.GREEN+Lang.TEAM_READY))
				);
	}
	

	public static boolean isRegenHeadItem(ItemStack item) {
		return (
				item != null 
				&& item.getType() == UniversalMaterial.PLAYER_HEAD.getType()
				&& item.hasItemMeta()
				&& item.getItemMeta().getLore().contains(ChatColor.GREEN+Lang.ITEMS_REGEN_HEAD)
		);
	}
	
	public static boolean doesInventoryContainsLobbyTeamItem(Inventory inv, String name){
		for(ItemStack item : inv.getContents()){
			if(item!=null && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals(name) && isLobbyTeamItem(item))
				return true;
		}
		return false;
	}

	public static ItemStack createRegenHead(Player player) {
		String name = player.getName();
		ItemStack item = VersionUtils.getVersionUtils().createPlayerSkull(name, player.getUniqueId());
		ItemMeta im = item.getItemMeta();

		// Setting up lore with team members
		List<String> lore = new ArrayList<String>();
		lore.add(ChatColor.GREEN+Lang.ITEMS_REGEN_HEAD);
		im.setLore(lore);
		im.setDisplayName(name);
		item.setItemMeta(im);

		return item;
	}

	public static void giveCompassPlayingTo(Player player) {
		ItemStack compass = new ItemStack(Material.COMPASS, 1);
		ItemMeta im = compass.getItemMeta();
		im.setDisplayName(ChatColor.GREEN+Lang.ITEMS_COMPASS_PLAYING);
		compass.setItemMeta(im);
		player.getInventory().addItem(compass);
	}


	
	public static boolean isCompassPlayingItem(ItemStack item) {
		return (
				item != null 
				&& item.getType() == Material.COMPASS 
				&& item.getItemMeta().getDisplayName().equals(ChatColor.GREEN+Lang.ITEMS_COMPASS_PLAYING)
				);
	}
	
	public static boolean isKitSelectionItem(ItemStack item){
		return (
				item != null 
				&& item.getType() == Material.IRON_PICKAXE 
				&& item.getItemMeta().getDisplayName().equals(ChatColor.GREEN+Lang.ITEMS_KIT_SELECTION)
				);
	}
	
	public static void giveKitSelectionTo(Player player) {
		if(KitsManager.isAtLeastOneKit()){
			ItemStack pickaxe = new ItemStack(Material.IRON_PICKAXE, 1);
			ItemMeta im = pickaxe.getItemMeta();
			im.setDisplayName(ChatColor.GREEN+Lang.ITEMS_KIT_SELECTION);
			pickaxe.setItemMeta(im);
			player.getInventory().addItem(pickaxe);
		}
	}
	
	public static void giveCraftBookTo(Player player) {
		/*if(CraftsManager.isAtLeastOneCraft()){
			ItemStack book = new ItemStack(Material.ENCHANTED_BOOK, 1);
			ItemMeta im = book.getItemMeta();
			im.setDisplayName(ChatColor.LIGHT_PURPLE+Lang.ITEMS_CRAFT_BOOK);
			book.setItemMeta(im);
			player.getInventory().addItem(book);
		}*/ // Craft book is dumb
	}
	
	public static boolean isCraftBookItem(ItemStack item){
		return (
				item != null 
				&& item.getType().equals(Material.ENCHANTED_BOOK)
				&& item.hasItemMeta()
				&& item.getItemMeta().hasDisplayName()
				&& item.getItemMeta().getDisplayName().equals(ChatColor.LIGHT_PURPLE+Lang.ITEMS_CRAFT_BOOK)
				);
	}


	public static void spawnExtraXp(Location location, int quantity) {
		ExperienceOrb orb = (ExperienceOrb) location.getWorld().spawnEntity(location, EntityType.EXPERIENCE_ORB);
		orb.setExperience(quantity);	
	}

	public static ItemStack createGoldenHeadPlayerSkull(String name, UUID uuid){

		ItemStack itemStack = VersionUtils.getVersionUtils().createPlayerSkull(name, uuid);
		ItemMeta itemMeta = itemStack.getItemMeta();
		itemMeta.setDisplayName(Lang.ITEMS_GOLDEN_HEAD_SKULL_NAME.replace("%player%", name));

		List<String> lore = new ArrayList<>();
		lore.add(Lang.ITEMS_GOLDEN_HEAD_SKULL_HELP);
		itemMeta.setLore(lore);

		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	public static ItemStack createGoldenHead(){

		ItemStack itemStack = new ItemStack(Material.GOLDEN_APPLE);
		ItemMeta itemMeta = itemStack.getItemMeta();

		itemMeta.setDisplayName(Lang.ITEMS_GOLDEN_HEAD_APPLE_NAME);
		itemMeta.setLore(Collections.singletonList(Lang.ITEMS_GOLDEN_HEAD_APPLE_HELP));

		itemStack.setItemMeta(itemMeta);

		return itemStack;
	}

}