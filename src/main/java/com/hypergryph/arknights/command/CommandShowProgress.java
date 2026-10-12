package com.hypergryph.arknights.command;

import com.alibaba.fastjson.JSONObject;
import com.hypergryph.arknights.core.dao.userDao;
import com.hypergryph.arknights.core.pojo.Account;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public class CommandShowProgress extends CommandBase {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getCommandName() {
        return "showProgress";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "[UID]";
    }

    @Override
    public String getCommandDescription() {
        return "查看账号主线进度";
    }

    @Override
    public String getCommandExample() {
        return "/showProgress 1000000000";
    }

    @Override
    public String getCommandExampleUsage() {
        return "显示当前主线关卡、已通关数和已解锁关卡数";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        int startIndex = args.length > 0 && args[0].equalsIgnoreCase(getCommandName()) ? 1 : 0;
        if (args.length != startIndex + 1) {
            LOGGER.error("用法: /showProgress <UID>");
            return;
        }

        try {
            long uid = Long.parseLong(args[startIndex]);
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
            JSONObject status = userData.getJSONObject("status");
            JSONObject stages = userData.getJSONObject("dungeon").getJSONObject("stages");
            int clearedCount = 0;
            int unlockedCount = stages.size();
            for (Map.Entry<String, Object> entry : stages.entrySet()) {
                JSONObject stageProgress = (JSONObject)entry.getValue();
                if (stageProgress.getIntValue("state") >= 3
                        || stageProgress.getIntValue("completeTimes") > 0) {
                    clearedCount++;
                }
            }

            LOGGER.info("账号进度: UID={}, mainStageProgress={}, clearedStages={}, unlockedStages={}",
                    uid, status.getString("mainStageProgress"), clearedCount, unlockedCount);
        } catch (NumberFormatException e) {
            LOGGER.error("UID 格式错误，必须为数字");
        } catch (Exception e) {
            LOGGER.error("执行 showProgress 命令失败", e);
        }
    }
}