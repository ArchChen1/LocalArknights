package com.hypergryph.arknights.command;

import com.alibaba.fastjson.JSONObject;
import com.hypergryph.arknights.core.dao.userDao;
import com.hypergryph.arknights.core.pojo.Account;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommandDelChar extends CommandBase {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getCommandName() {
        return "delChar";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "[UID] [charInstId|charId]";
    }

    @Override
    public String getCommandDescription() {
        return "从账号中删除一个干员实例";
    }

    @Override
    public String getCommandExample() {
        return "/delChar 1000000000 12";
    }

    @Override
    public String getCommandExampleUsage() {
        return "可传实例 ID；角色 ID 仅在账号中唯一时可用";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        int startIndex = args.length > 0 && args[0].equalsIgnoreCase(getCommandName()) ? 1 : 0;
        if (args.length != startIndex + 2) {
            LOGGER.error("用法: /delChar <UID> <charInstId|charId>");
            return;
        }

        try {
            long uid = Long.parseLong(args[startIndex]);
            String target = args[startIndex + 1];
            if (uid <= 0) {
                LOGGER.error("UID 必须大于 0");
                return;
            }

            List<Account> accounts = userDao.queryAccountByUid(uid);
            if (accounts.size() != 1) {
                LOGGER.error("未找到账号: {}", uid);
                return;
            }

            JSONObject userData = JSONObject.parseObject(accounts.get(0).getUser());
            JSONObject troop = userData.getJSONObject("troop");
            JSONObject chars = troop.getJSONObject("chars");
            String charInstId = null;
            JSONObject charData = chars.getJSONObject(target);

            if (charData != null) {
                charInstId = target;
            } else {
                List<String> matchingInstances = new ArrayList<>();
                for (Map.Entry<String, Object> entry : chars.entrySet()) {
                    JSONObject ownedChar = chars.getJSONObject(entry.getKey());
                    if (ownedChar != null && target.equals(ownedChar.getString("charId"))) {
                        matchingInstances.add(entry.getKey());
                    }
                }
                if (matchingInstances.isEmpty()) {
                    LOGGER.error("账号中未找到干员实例或角色 ID: {}", target);
                    return;
                }
                if (matchingInstances.size() > 1) {
                    LOGGER.error("该角色有多个实例，请改用 charInstId: {}", matchingInstances);
                    return;
                }
                charInstId = matchingInstances.get(0);
                charData = chars.getJSONObject(charInstId);
            }

            String charId = charData.getString("charId");
            chars.remove(charInstId);

            boolean hasRemainingCopy = false;
            for (Map.Entry<String, Object> entry : chars.entrySet()) {
                JSONObject ownedChar = chars.getJSONObject(entry.getKey());
                if (ownedChar != null && charId.equals(ownedChar.getString("charId"))) {
                    hasRemainingCopy = true;
                    break;
                }
            }

            if (!hasRemainingCopy) {
                troop.getJSONObject("charGroup").remove(charId);
                JSONObject buildingChars = userData.getJSONObject("building").getJSONObject("chars");
                List<String> buildingEntriesToRemove = new ArrayList<>();
                for (Map.Entry<String, Object> entry : buildingChars.entrySet()) {
                    JSONObject buildingChar = buildingChars.getJSONObject(entry.getKey());
                    if (buildingChar != null && charId.equals(buildingChar.getString("charId"))) {
                        buildingEntriesToRemove.add(entry.getKey());
                    }
                }
                for (String buildingEntryId : buildingEntriesToRemove) {
                    buildingChars.remove(buildingEntryId);
                }
            }

            userDao.setUserData(uid, userData);
            LOGGER.info("干员实例已删除: UID={}, charInstId={}, charId={}", uid, charInstId, charId);
            LOGGER.info("潜能材料和已获得资源不会自动删除；请让账号重新登录刷新");
        } catch (NumberFormatException e) {
            LOGGER.error("UID 格式错误，必须为数字");
        } catch (Exception e) {
            LOGGER.error("执行 delChar 命令失败", e);
        }
    }
}