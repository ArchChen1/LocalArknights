package com.hypergryph.arknights.command;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hypergryph.arknights.Admin;
import com.hypergryph.arknights.core.dao.userDao;
import com.hypergryph.arknights.core.pojo.Account;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommandAddItem extends CommandBase {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Set<String> SUPPORTED_TYPES = new HashSet<>(Arrays.asList(
            "HGG_SHD", "LGG_SHD", "MATERIAL", "CARD_EXP", "SOCIAL_PT", "AP_GAMEPLAY",
            "AP_ITEM", "TKT_TRY", "DIAMOND", "DIAMOND_SHD", "GOLD", "TKT_RECRUIT",
            "TKT_INST_FIN", "TKT_GACHA_PRSV", "RENAMING_CARD", "RETRO_COIN", "AP_SUPPLY",
            "TKT_GACHA_10", "TKT_GACHA", "CHAR_SKIN"
    ));

    @Override
    public String getCommandName() {
        return "addItem";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "[UID] [itemType] [itemId] [count]";
    }

    @Override
    public String getCommandDescription() {
        return "向账号添加道具或资源";
    }

    @Override
    public String getCommandExample() {
        return "/addItem 1000000000 MATERIAL 30011 10";
    }

    @Override
    public String getCommandExampleUsage() {
        return "道具类型和 ID 使用游戏数据中的值";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        int startIndex = args.length > 0 && args[0].equalsIgnoreCase(getCommandName()) ? 1 : 0;
        if (args.length != startIndex + 4) {
            LOGGER.error("用法: /addItem <UID> <itemType> <itemId> <count>");
            return;
        }

        try {
            long uid = Long.parseLong(args[startIndex]);
            String itemType = args[startIndex + 1];
            String itemId = args[startIndex + 2];
            int count = Integer.parseInt(args[startIndex + 3]);
            if (uid <= 0 || count <= 0) {
                LOGGER.error("UID 和数量必须大于 0");
                return;
            }
            if (!SUPPORTED_TYPES.contains(itemType) && !itemType.contains("VOUCHER")) {
                LOGGER.error("不支持的道具类型: {}", itemType);
                return;
            }
            if (itemType.equals("CHAR_SKIN") && count != 1) {
                LOGGER.error("皮肤是非堆叠道具，数量必须为 1");
                return;
            }

            List<Account> accounts = userDao.queryAccountByUid(uid);
            if (accounts.size() != 1) {
                LOGGER.error("未找到账号: {}", uid);
                return;
            }

            JSONObject userData = JSONObject.parseObject(accounts.get(0).getUser());
            JSONArray items = new JSONArray();
            Admin.GM_GiveItem(userData, itemId, itemType, count, items);
            userDao.setUserData(uid, userData);
            LOGGER.info("道具已添加: UID={}, type={}, id={}, count={}", uid, itemType, itemId, count);
        } catch (NumberFormatException e) {
            LOGGER.error("UID 或数量格式错误");
        } catch (Exception e) {
            LOGGER.error("执行 addItem 命令失败", e);
        }
    }
}