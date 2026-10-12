package com.hypergryph.arknights.command;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hypergryph.arknights.Admin;
import com.hypergryph.arknights.ArknightsApplication;
import com.hypergryph.arknights.core.dao.userDao;
import com.hypergryph.arknights.core.pojo.Account;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class CommandAddChar extends CommandBase {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getCommandName() {
        return "addChar";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "[UID] [charId]";
    }

    @Override
    public String getCommandDescription() {
        return "向账号添加干员";
    }

    @Override
    public String getCommandExample() {
        return "/addChar 1000000000 char_285_medic2";
    }

    @Override
    public String getCommandExampleUsage() {
        return "重复干员会按游戏逻辑转换为对应信物";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        int startIndex = args.length > 0 && args[0].equalsIgnoreCase(getCommandName()) ? 1 : 0;
        if (args.length != startIndex + 2) {
            LOGGER.error("用法: /addChar <UID> <charId>");
            return;
        }

        long uid;
        try {
            uid = Long.parseLong(args[startIndex]);
        } catch (NumberFormatException e) {
            LOGGER.error("UID 格式错误，必须为数字");
            return;
        }

        try {
            String charId = args[startIndex + 1];
            if (uid <= 0) {
                LOGGER.error("UID 必须大于 0");
                return;
            }
            if (!ArknightsApplication.characterJson.containsKey(charId)) {
                LOGGER.error("无效的干员 ID: {}", charId);
                return;
            }

            List<Account> accounts = userDao.queryAccountByUid(uid);
            if (accounts.size() != 1) {
                LOGGER.error("未找到账号: {}", uid);
                return;
            }

            JSONObject userData = JSONObject.parseObject(accounts.get(0).getUser());
            JSONArray items = new JSONArray();
            Admin.GM_GiveItem(userData, charId, "CHAR", 1, items);
            userDao.setUserData(uid, userData);
            LOGGER.info("干员已添加: UID={}, charId={}", uid, charId);
        } catch (NumberFormatException e) {
            LOGGER.error("添加干员时遇到数值格式错误", e);
        } catch (Exception e) {
            LOGGER.error("执行 addChar 命令失败", e);
        }
    }
}