package com.hypergryph.arknights.command;

public class CommandManager extends CommandHandler {
    public CommandManager() {
        this.registerCommand(new CommandHelp());
        this.registerCommand(new CommandAddD());
        this.registerCommand(new CommandCreateAC());
        this.registerCommand(new CommandClearTo());
        this.registerCommand(new CommandResetStage());
        this.registerCommand(new CommandAddItem());
        this.registerCommand(new CommandAddChar());
        this.registerCommand(new CommandShowProgress());
        this.registerCommand(new CommandCharList());
        this.registerCommand(new CommandDelChar());
    }
}