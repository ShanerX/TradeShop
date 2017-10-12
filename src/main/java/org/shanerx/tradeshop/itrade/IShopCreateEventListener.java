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

package org.shanerx.tradeshop.itrade;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.shanerx.tradeshop.ShopType;
import org.shanerx.tradeshop.TradeShop;
import org.shanerx.tradeshop.Utils;

public class IShopCreateEventListener extends Utils implements Listener {

    private TradeShop plugin;

    public IShopCreateEventListener(TradeShop instance) {
        plugin = instance;
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        Sign s = (Sign) event.getBlock().getState();
        if (!(event.getLine(0).equalsIgnoreCase("[iTrade]"))) {
            return;
        }

        Block chest = findShopChest(s.getBlock());

        if (!player.hasPermission(getCreateIPerm())) {
            failedSign(event, ShopType.ITRADE, "no-ts-create-permission");
            return;
        }
        
        if (findShopChest(s.getBlock()) != null && getShopUsers(findShopChest(s.getBlock())).size() > 0) {
            getShopOwners(s).stream().forEach(op -> {
                if (!op.getName().equalsIgnoreCase(plugin.getSettings().getString("itrade-shop-name"))) {
                    failedSign(event, ShopType.ITRADE, "not-owner");
                    return;
                }
            });
        }

        String line1 = event.getLine(1);
        String line2 = event.getLine(2);

        if (!line1.contains(" ") || !line2.contains(" ")) {
            failedSign(event, ShopType.ITRADE, "missing-item");
            return;
        }

        String[] info1 = line1.split(" ");
        String[] info2 = line2.split(" ");

        if (info1.length != 2 || info2.length != 2) {
            failedSign(event, ShopType.ITRADE, "missing-info");
            return;
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

        int amount1 = 0, amount2 = 0;
        ItemStack item1 = null, item2 = null;

        try {
            amount1 = Integer.parseInt(info1[0]);
            amount2 = Integer.parseInt(info2[0]);

        } catch (Exception e) {
            failedSign(event, ShopType.ITRADE, "amount-not-num");
            return;
        }

        try {
            item1 = isValidType(info1[1], durability1, amount1);
            item2 = isValidType(info2[1], durability2, amount2);
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        if (item1 == null || item2 == null) {
            failedSign(event, ShopType.ITRADE, "missing-item");
            return;
        } else if (isBlacklistItem(item1) || isBlacklistItem(item2)) {
            failedSign(event, ShopType.ITRADE, "illegal-item");
            return;
        }

        if (chest != null) {
            setName((InventoryHolder) chest.getState(), "o:" + plugin.getSettings().getString("itrade-shop-name"));
        }
        event.setLine(0, ChatColor.DARK_GREEN + "[iTrade]");
        event.setLine(3, plugin.getSettings().getString("itrade-shop-name"));
        event.getPlayer().sendMessage(colorize(getPrefix() + plugin.getMessages().getString("successful-setup")));
    }
}
