/*
 *     Copyright (c) 2016-2017 SparklingComet @ http://shanerx.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *              http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * NOTICE: All modifications made by others to the source code belong
 * to the respective contributor. No contributor should be held liable for
 * any damages of any kind, whether be material or moral, which were
 * caused by their contribution(s) to the project. See the full License for more information
 */

package org.shanerx.tradeshop.bitrade;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.shanerx.tradeshop.TradeShop;
import org.shanerx.tradeshop.Utils;

public class BiTradeEventListener extends Utils implements Listener {
	
	private TradeShop plugin;
	
	public BiTradeEventListener(TradeShop instance) {
		plugin = instance;
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onBlockInteract(PlayerInteractEvent e) {
		
		Player buyer = e.getPlayer();
		
		if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
		    
			if (!isBiTradeShopSign(e.getClickedBlock())) {
				return;
			}
			Sign s = (Sign) e.getClickedBlock().getState();
			BlockState chestState;
			
			try {
				chestState = e.getClickedBlock().getRelative(0, -1, 0).getState();
			} catch (NullPointerException npe) {
				buyer.sendMessage(colorize(getPrefix() + plugin.getMessages().getString("missing-shop")));
				return;
			}
			
			Inventory chestInventory = ((InventoryHolder) chestState).getInventory();
			Inventory playerInventory = buyer.getInventory();
			
			String line1 = s.getLine(1);
			String line2 = s.getLine(2);
			String[] info1 = line1.split(" ");
			String[] info2 = line2.split(" ");
			
			
			int amount1 = Integer.parseInt(info1[0]);
			int amount2 = Integer.parseInt(info2[0]);
			e.setCancelled(true);
			
			if(buyer.isSneaking()){
			    if(!buyer.isOnGround() && plugin.getSettings().getBoolean("allow-quad-trade")){
                    amount1 = amount1 * 4;
                    amount2 = amount2 * 4;
			    } else if(plugin.getSettings().getBoolean("allow-double-trade")){
	                amount1 += amount1;
	                amount2 += amount2;
	            }
            }
			
			int durability1 = 0;
			int durability2 = 0;
			if (line1.split(":").length > 1) {
				durability1 = Integer.parseInt(info1[1].split(":")[1]);
				info1[1] = info1[1].split(":")[0];
			}
			if (line2.split(":").length > 1) {
				durability2 = Integer.parseInt(info2[1].split(":")[1]);
				info2[1] = info2[1].split(":")[0];
			}
			
			String item_name1, item_name2;
			
			if (isInt(info1[1]))
				item_name1 = Material.getMaterial(Integer.parseInt(info1[1])).name();
			else
				item_name1 = info1[1].toUpperCase();
			
			if (isInt(info2[1]))
				item_name2 = Material.getMaterial(Integer.parseInt(info2[1])).name();
			else
				item_name2 = info2[1].toUpperCase();
			
			ItemStack item1 = new ItemStack(Material.getMaterial(item_name1), amount1); // What the player gets
			ItemStack item2 = new ItemStack(Material.getMaterial(item_name2), amount2); // What the player pays
			
			if (!containsAtLeast(playerInventory, item2.getType(), (short) durability2, amount2)) {
				buyer.sendMessage(colorize(getPrefix() + plugin.getMessages().getString("insufficient-items")
						.replace("{ITEM}", item_name2.toLowerCase()).replace("{AMOUNT}", String.valueOf(amount2))));
				return;
			}
			
			if (!containsAtLeast(chestInventory, item1.getType(), (short) durability1, amount1)) {
				buyer.sendMessage(colorize(getPrefix() + plugin.getMessages().getString("shop-empty")
						.replace("{ITEM}", item_name1.toLowerCase()).replace("{AMOUNT}", String.valueOf(amount1))));
				return;
			}
			
			if (!canExchange(chestInventory, item1, amount1, item2, amount2)) {
				buyer.sendMessage(colorize(getPrefix() + plugin.getMessages().getString("shop-full")
						.replace("{ITEM}", item_name1.toLowerCase()).replace("{AMOUNT}", String.valueOf(amount1))));
				return;
			}
			
			if (!canExchange(playerInventory, item2, amount2, item1, amount1)) {
				buyer.sendMessage(colorize(getPrefix() + plugin.getMessages().getString("player-full")
						.replace("{ITEM}", item_name2.toLowerCase()).replace("{AMOUNT}", String.valueOf(amount2))));
				return;
			}
			
			int count = amount1, removed = 0;
			while (count > 0) {
				ItemStack temp = chestInventory.getItem(chestInventory.first(item1.getType()));
				if (count > item1.getMaxStackSize())
					removed = item1.getMaxStackSize();
				else
					removed = count;
				
				if (removed > temp.getAmount())
					removed = temp.getAmount();
				
				item1.setAmount(removed);
				item1.setData(temp.getData());
				item1.setItemMeta(temp.getItemMeta());
				item1.setDurability((short) durability1);
				chestInventory.removeItem(item1);
				playerInventory.addItem(item1);
				
				count -= removed;
			}
			
			count = amount2;
			removed = 0;
			while (count > 0) {
				ItemStack temp = playerInventory.getItem(playerInventory.first(item2.getType()));
				if (count > item2.getMaxStackSize())
					removed = item2.getMaxStackSize();
				else
					removed = count;
				
				if (removed > temp.getAmount())
					removed = temp.getAmount();
				
				item2.setAmount(removed);
				item2.setData(temp.getData());
				item2.setItemMeta(temp.getItemMeta());
				item2.setDurability((short) durability2);
				playerInventory.removeItem(item2);
				chestInventory.addItem(item2);
				
				count -= removed;
			}
			
			String message = plugin.getMessages().getString("on-trade").replace("{AMOUNT1}", String.valueOf(amount1)).replace("{AMOUNT2}", String.valueOf(amount2)).replace("{ITEM1}", item_name1.toLowerCase()).replace("{ITEM2}", item_name2.toLowerCase()).replace("{SELLER}", s.getLine(3));
			buyer.sendMessage(colorize(getPrefix() + message));
			
		} else if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            
		    if (!isBiTradeShopSign(e.getClickedBlock())) {
                return;
            }
            Sign s = (Sign) e.getClickedBlock().getState();
            BlockState chestState;
            
            try {
                chestState = e.getClickedBlock().getRelative(0, -1, 0).getState();
            } catch (NullPointerException npe) {
                buyer.sendMessage(colorize(getPrefix() + plugin.getMessages().getString("missing-shop")));
                return;
            }
            
            Inventory chestInventory = ((InventoryHolder) chestState).getInventory();
            Inventory playerInventory = buyer.getInventory();
            
            String line2 = s.getLine(2);
            String line1 = s.getLine(1);
            String[] info2 = line2.split(" ");
            String[] info1 = line1.split(" ");
            
            
            int amount2 = Integer.parseInt(info2[0]);
            int amount1 = Integer.parseInt(info1[0]);
            e.setCancelled(true);
            
            if(buyer.isSneaking()){
                if(!buyer.isOnGround() && plugin.getSettings().getBoolean("allow-quad-trade")){
                    amount2 = amount2 * 4;
                    amount1 = amount1 * 4;
                } else if(plugin.getSettings().getBoolean("allow-double-trade")){
                    amount2 += amount2;
                    amount1 += amount1;
                }
            }
            
            int durability2 = 0;
            int durability1 = 0;
            if (line2.split(":").length > 2) {
                durability2 = Integer.parseInt(info2[2].split(":")[2]);
                info2[2] = info2[2].split(":")[0];
            }
            if (line1.split(":").length > 2) {
                durability1 = Integer.parseInt(info1[2].split(":")[2]);
                info1[2] = info1[2].split(":")[0];
            }
            
            String item_name2, item_name1;
            
            if (isInt(info2[1]))
                item_name2 = Material.getMaterial(Integer.parseInt(info2[1])).name();
            else
                item_name2 = info2[1].toUpperCase();
            
            if (isInt(info1[1]))
                item_name1 = Material.getMaterial(Integer.parseInt(info1[1])).name();
            else
                item_name1 = info1[1].toUpperCase();
            
            ItemStack item2 = new ItemStack(Material.getMaterial(item_name2), amount2); // What the player gets
            ItemStack item1 = new ItemStack(Material.getMaterial(item_name1), amount1); // What the player pays
            
            if (!containsAtLeast(playerInventory, item1.getType(), (short) durability1, amount1)) {
                buyer.sendMessage(colorize(getPrefix() + plugin.getMessages().getString("insufficient-items")
                        .replace("{ITEM}", item_name1.toLowerCase()).replace("{AMOUNT}", String.valueOf(amount1))));
                return;
            }
            
            if (!containsAtLeast(chestInventory, item2.getType(), (short) durability2, amount2)) {
                buyer.sendMessage(colorize(getPrefix() + plugin.getMessages().getString("shop-empty")
                        .replace("{ITEM}", item_name2.toLowerCase()).replace("{AMOUNT}", String.valueOf(amount2))));
                return;
            }
            
            if (!canExchange(chestInventory, item2, amount2, item1, amount1)) {
                buyer.sendMessage(colorize(getPrefix() + plugin.getMessages().getString("shop-full")
                        .replace("{ITEM}", item_name2.toLowerCase()).replace("{AMOUNT}", String.valueOf(amount2))));
                return;
            }
            
            if (!canExchange(playerInventory, item1, amount1, item2, amount2)) {
                buyer.sendMessage(colorize(getPrefix() + plugin.getMessages().getString("player-full")
                        .replace("{ITEM}", item_name1.toLowerCase()).replace("{AMOUNT}", String.valueOf(amount1))));
                return;
            }
            
            int count = amount2, removed = 0;
            while (count > 0) {
                ItemStack temp = chestInventory.getItem(chestInventory.first(item2.getType()));
                if (count > item2.getMaxStackSize())
                    removed = item2.getMaxStackSize();
                else
                    removed = count;
                
                if (removed > temp.getAmount())
                    removed = temp.getAmount();
                
                item2.setAmount(removed);
                item2.setData(temp.getData());
                item2.setItemMeta(temp.getItemMeta());
                item2.setDurability((short) durability2);
                chestInventory.removeItem(item2);
                playerInventory.addItem(item2);
                
                count -= removed;
            }
            
            count = amount1;
            removed = 0;
            while (count > 0) {
                ItemStack temp = playerInventory.getItem(playerInventory.first(item1.getType()));
                if (count > item1.getMaxStackSize())
                    removed = item1.getMaxStackSize();
                else
                    removed = count;
                
                if (removed > temp.getAmount())
                    removed = temp.getAmount();
                
                item1.setAmount(removed);
                item1.setData(temp.getData());
                item1.setItemMeta(temp.getItemMeta());
                item1.setDurability((short) durability1);
                playerInventory.removeItem(item1);
                chestInventory.addItem(item1);
                
                count -= removed;
            }
            
            String message = plugin.getMessages().getString("on-trade").replace("{AMOUNT2}", String.valueOf(amount1)).replace("{AMOUNT1}", String.valueOf(amount2)).replace("{ITEM2}", item_name1.toLowerCase()).replace("{ITEM1}", item_name2.toLowerCase()).replace("{SELLER}", s.getLine(3));
            buyer.sendMessage(colorize(getPrefix() + message));
		}
	}
}