package dev.swiss.clanbot.clan.listeners;

import dev.swiss.clanbot.clan.ClanRequest;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ClanRequestButtonListener extends ListenerAdapter {

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        if(Objects.requireNonNull(event.getUser()).isBot()) return;

        if(!ClanRequest.isClanRequestMessage(event.getMessageIdLong())) return;

        ClanRequest request = ClanRequest.getClanRequestByMessageID(event.getMessageIdLong());
        if(request == null) return;

        if(event.getComponentId().equals("accept")) {
            request.accept(event.getMessageIdLong());
        } else if(event.getComponentId().equals("deny")) {
            request.deny(event.getMessageIdLong());
        }
    }

}
