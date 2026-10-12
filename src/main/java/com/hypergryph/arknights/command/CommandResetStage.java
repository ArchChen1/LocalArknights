package com.hypergryph.arknights.command;

import com.alibaba.fastjson.JSONObject;
import com.hypergryph.arknights.ArknightsApplication;
import com.hypergryph.arknights.core.dao.userDao;
import com.hypergryph.arknights.core.pojo.Account;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class CommandResetStage extends CommandBase {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getCommandName() {
        return "resetStage";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "[UID] [stageId]";
    }

    @Override
    public String getCommandDescription() {
        return "重置账号的单个关卡进度";
    }

    @Override
    public String getCommandExample() {
        return "/resetStage 1000000000 main_05-10";
    }

    @Override
    public String getCommandExampleUsage() {
        return "清除该关卡的通关记录，不回滚已获得的奖励";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        int startIndex = args.length > 0 && args[0].equalsIgnoreCase(getCommandName()) ? 1 : 0;
        if (args.length != startIndex + 2) {
            LOGGER.error("用法: /resetStage <UID> <stageId>");
            return;
        }

        try {
            long uid = Long.parseLong(args[startIndex]);
            String stageId = args[startIndex + 1];
            if (uid <= 0) {
                LOGGER.error("UID 必须大于 0");
                return;
            }
            if (!ArknightsApplication.mainStage.containsKey(stageId)
                    || !ArknightsApplication.stageTable.containsKey(stageId)) {
                LOGGER.error("无效或不可用的关卡 ID: {}", stageId);
                return;
            }

            List<Account> accounts = userDao.queryAccountByUid(uid);
            if (accounts.size() != 1) {
                LOGGER.error("未找到账号: {}", uid);
                return;
            }

            JSONObject userData = JSONObject.parseObject(accounts.get(0).getUser());
            JSONObject stages = userData.getJSONObject("dungeon").getJSONObject("stages");
            JSONObject stageProgress = new JSONObject(true);
            stageProgress.put("stageId", stageId);
            stageProgress.put("hasBattleReplay", 0);
            stageProgress.put("noCostCnt", 1);
            stageProgress.put("practiceTimes", 0);
            stageProgress.put("completeTimes", 0);
            stageProgress.put("state", 0);
            stageProgress.put("startTimes", 0);
            stages.put(stageId, stageProgress);
            userData.getJSONObject("status").put("mainStageProgress", stageId);
            userDao.setUserData(uid, userData);

            LOGGER.info("关卡进度已重置: UID={}, stageId={}", uid, stageId);
            LOGGER.info("请让该账号重新登录以刷新客户端进度");
        } catch (NumberFormatException e) {
            LOGGER.error("UID 格式错误，必须为数字");
        } catch (Exception e) {
            LOGGER.error("执行 resetStage 命令失败", e);
        }
    }
}