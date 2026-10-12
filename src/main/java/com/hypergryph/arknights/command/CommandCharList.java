package com.hypergryph.arknights.command;

import com.alibaba.fastjson.JSONObject;
import com.hypergryph.arknights.ArknightsApplication;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandCharList extends CommandBase {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    @Override
    public String getCommandName() {
        return "char";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "[page] [pageSize=20]";
    }

    @Override
    public String getCommandDescription() {
        return "分页查看干员 ID 和名称";
    }

    @Override
    public String getCommandExample() {
        return "/char 2 20";
    }

    @Override
    public String getCommandExampleUsage() {
        return "默认查看第 1 页，每页 20 名；页容量最大 100";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        int startIndex = args.length > 0 && args[0].equalsIgnoreCase(getCommandName()) ? 1 : 0;
        int argumentCount = args.length - startIndex;
        if (argumentCount < 0 || argumentCount > 2) {
            LOGGER.error("用法: /char [page] [pageSize=20]");
            return;
        }

        try {
            int page = argumentCount >= 1 ? Integer.parseInt(args[startIndex]) : 1;
            int pageSize = argumentCount >= 2 ? Integer.parseInt(args[startIndex + 1]) : DEFAULT_PAGE_SIZE;
            if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
                LOGGER.error("页码必须大于 0，每页数量范围为 1 到 {}", MAX_PAGE_SIZE);
                return;
            }

            List<String> charIds = new ArrayList<>();
            for (String charId : ArknightsApplication.characterJson.keySet()) {
                if (charId.startsWith("char_")) {
                    charIds.add(charId);
                }
            }
            Collections.sort(charIds);

            int pageCount = (charIds.size() + pageSize - 1) / pageSize;
            if (page > pageCount) {
                LOGGER.error("页码超出范围: 当前共 {} 页", pageCount);
                return;
            }

            int start = (page - 1) * pageSize;
            int end = Math.min(start + pageSize, charIds.size());
            LOGGER.info("干员列表: 第 {}/{} 页，本页 {} 名，总计 {} 名", page, pageCount, end - start, charIds.size());
            for (int i = start; i < end; i++) {
                String charId = charIds.get(i);
                JSONObject character = ArknightsApplication.characterJson.getJSONObject(charId);
                LOGGER.info("{} | {} | {}", charId, character.getString("name"), character.getString("rarity"));
            }
        } catch (NumberFormatException e) {
            LOGGER.error("页码和每页数量必须是数字");
        } catch (Exception e) {
            LOGGER.error("执行 char 命令失败", e);
        }
    }
}