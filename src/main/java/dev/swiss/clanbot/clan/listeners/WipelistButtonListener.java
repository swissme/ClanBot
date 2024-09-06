package dev.swiss.clanbot.clan.listeners;

import dev.swiss.clanbot.ClanBot;
import dev.swiss.clanbot.clan.Clan;
import dev.swiss.clanbot.clan.ClanRequest;
import dev.swiss.clanbot.clan.Wipelist;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class WipelistButtonListener extends ListenerAdapter {

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        if(Objects.requireNonNull(event.getUser()).isBot()) return;

        if(!Wipelist.isWipelistMessage(event.getMessage())) return;

        Wipelist wipelist = Wipelist.getWipelistByMessage(event.getMessage());
        if(wipelist == null) return;

        Member member = event.getMember();

        if(member == null) return;

        Clan clan = Clan.getByMember(member);

        if(clan == null) {
            event.replyEmbeds(
                new EmbedBuilder()
                        .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                        .setDescription("You are not in a clan!")
                        .build()
            ).setEphemeral(true).queue();
            return;
        }

        if(clan.getLeader() != member.getIdLong() || clan.getColeaders().contains(member.getIdLong())) {
            event.replyEmbeds(
                new EmbedBuilder()
                        .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                        .setDescription("You are not the leader or coleader of your clan!")
                        .build()
            ).setEphemeral(true).queue();
            return;
        }

        if(event.getComponentId().equals("join")) {
            EmbedBuilder join = wipelist.join(clan);
            if(join == null) return;
            event.replyEmbeds(join.build()).setEphemeral(true).queue();
        } else if(event.getComponentId().equals("leave")) {
            EmbedBuilder leave = wipelist.leave(clan);
            if(leave == null) return;
            event.replyEmbeds(leave.build()).setEphemeral(true).queue();
        }
    }

}
