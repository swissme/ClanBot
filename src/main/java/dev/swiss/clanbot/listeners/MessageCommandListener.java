package dev.swiss.clanbot.listeners;

import dev.swiss.clanbot.*;
import net.dv8tion.jda.api.events.message.*;
import net.dv8tion.jda.api.hooks.*;
import org.jetbrains.annotations.*;

/**
 * @author Swiss (swiss@swissdev.com)
 */
public class MessageCommandListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(!event.isFromGuild())
            return;
        String[] args = event.getMessage().getContentRaw().split(" ");
        if(args[0].equalsIgnoreCase(ClanBot.getInstance().getConfig().getPrefix() + "clanhelp")) {
            event.getChannel().sendMessageEmbeds(ClanBot.getInstance().getHelpMessage().build()).queue();
        }
    }

}
