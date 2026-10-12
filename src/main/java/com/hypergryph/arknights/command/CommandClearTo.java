package com.hypergryph.arknights.command;

import com.alibaba.fastjson.JSONObject;
import com.hypergryph.arknights.ArknightsApplication;
import com.hypergryph.arknights.core.dao.userDao;
import com.hypergryph.arknights.core.pojo.Account;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommandClearTo extends CommandBase {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getCommandName() {
        return "clearTo";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "[UID] [stageId]";
    }

    @Override
    public String getCommandDescription() {
        return "将账号主线进度推进到指定关卡";
    }

    @Override
    public String getCommandExample() {
        return "/clearTo 1000000000 main_05-10";
    }

    @Override
    public String getCommandExampleUsage() {
        return "前序主线记为三星通关，目标关卡解锁但不结算奖励";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        int startIndex = args.length > 0 && args[0].equalsIgnoreCase(getCommandName()) ? 1 : 0;
        if (args.length != startIndex + 2) {
            LOGGER.error("用法: /clearTo <UID> <stageId>");
            return;
        }

        try {
            long uid = Long.parseLong(args[startIndex]);
            String targetStageId = args[startIndex + 1];
            if (uid <= 0) {
                LOGGER.error("UID 必须大于 0");
                return;
            }
            if (!ArknightsApplication.mainStage.containsKey(targetStageId)
                    || !ArknightsApplication.stageTable.containsKey(targetStageId)) {
                LOGGER.error("无效或不可用的关卡 ID: {}", targetStageId);
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
            String currentStageId = status.getString("mainStageProgress");
            List<String> stagesToClear = getStagesToClear(currentStageId, targetStageId);
            if (stagesToClear == null) {
                LOGGER.error("目标关卡 {} 不在账号当前进度 {} 的后续主线中", targetStageId, currentStageId);
                return;
            }

            for (String stageId : stagesToClear) {
                JSONObject stageProgress = getOrCreateStageProgress(stages, stageId);
                stageProgress.put("state", 3);
                stageProgress.put("completeTimes", Math.max(1, stageProgress.getIntValue("completeTimes")));
                stageProgress.put("practiceTimes", 0);
                unlockBranches(stages, stageId);
            }

            getOrCreateStageProgress(stages, targetStageId);
            status.put("mainStageProgress", targetStageId);
            userDao.setUserData(uid, userData);

            LOGGER.info("主线进度已更新: UID={}, cleared={}, unlocked={}",
                    uid, stagesToClear.size(), targetStageId);
            LOGGER.info("请让该账号重新登录以刷新客户端进度");
        } catch (NumberFormatException e) {
            LOGGER.error("UID 格式错误，必须为数字");
        } catch (Exception e) {
            LOGGER.error("执行 clearTo 命令失败", e);
        }
    }

    private List<String> getStagesToClear(String currentStageId, String targetStageId) {
        if (currentStageId == null) {
            return null;
        }

        List<String> stagesToClear = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        String stageId = currentStageId;
        while (stageId != null && visited.add(stageId)) {
            if (stageId.equals(targetStageId)) {
                return stagesToClear;
            }

            stagesToClear.add(stageId);
            JSONObject stageLinks = ArknightsApplication.mainStage.getJSONObject(stageId);
            if (stageLinks == null) {
                return null;
            }
            stageId = stageLinks.getString("next");
        }
        return null;
    }

    private JSONObject getOrCreateStageProgress(JSONObject stages, String stageId) {
        JSONObject stageProgress = stages.getJSONObject(stageId);
        if (stageProgress != null) {
            return stageProgress;
        }

        stageProgress = new JSONObject(true);
        stageProgress.put("stageId", stageId);
        stageProgress.put("hasBattleReplay", 0);
        stageProgress.put("noCostCnt", 1);
        stageProgress.put("practiceTimes", 0);
        stageProgress.put("completeTimes", 0);
        stageProgress.put("state", 0);
        stageProgress.put("startTimes", 0);
        stages.put(stageId, stageProgress);
        return stageProgress;
    }

    private void unlockBranches(JSONObject stages, String stageId) {
        JSONObject stageLinks = ArknightsApplication.mainStage.getJSONObject(stageId);
        String[] branchKeys = {"sub", "star", "hard"};
        for (String branchKey : branchKeys) {
            String branchStageId = stageLinks.getString(branchKey);
            if (branchStageId != null && ArknightsApplication.stageTable.containsKey(branchStageId)) {
                getOrCreateStageProgress(stages, branchStageId);
            }
        }
    }
}
