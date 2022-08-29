package dev.swiss.clanbot.clan.listeners;

import dev.swiss.clanbot.*;
import dev.swiss.clanbot.clan.*;
import dev.swiss.clanbot.clan.Invite;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.*;
import net.dv8tion.jda.api.hooks.*;
import org.jetbrains.annotations.*;

import java.awt.*;
import java.util.*;

/**
 * @author Swiss (swiss@swissdev.com)
 */
public class ButtonClickListener extends ListenerAdapter {

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        PrivateChannel pc = event.getPrivateChannel();

        Clan clan = Clan.getClanByInviteMessage(event.getMessageIdLong());
        Invite invite = Clan.getInviteByInviteMessage(event.getMessageIdLong());
        if(clan == null || invite == null)
            return;

        if(invite.getExpiredTime() <= System.currentTimeMillis()) {
            pc.sendMessageEmbeds(
                    new EmbedBuilder()
                            .setDescription("That invite expired, ask the leader for a new one")
                            .build()
            ).queue();
            clan.getInvites().remove(invite);
            return;
        }

        Member leader = ClanBot.getInstance().getGuild().retrieveMemberById(clan.getLeader()).complete();

        if(event.getComponentId().equals("join")) {
            event.replyEmbeds(
                    new EmbedBuilder()
                            .setDescription("You have just joined `" + clan.getName() + "`")
                            .setColor(Color.decode(clan.getColor()))
                            .build()
            ).queue();
            leader.getUser().openPrivateChannel().queue((channel -> channel.sendMessageEmbeds(
                    new EmbedBuilder()
                            .setDescription(invite.getMember().getAsMention() + " has joined `" + clan.getName() + "`")
                            .build()
            ).queue()));
            clan.getMembers().add(invite.getMember().getIdLong());
            clan.getInvites().remove(invite);
            clan.save();
            ClanBot.getInstance().getGuild().addRoleToMember(invite.getMember(), Objects.requireNonNull(ClanBot.getInstance().getGuild().getRoleById(clan.getRole()))).queue();
        } else if(event.getComponentId().equals("deny")) {
            event.replyEmbeds(
                    new EmbedBuilder()
                            .setDescription("You have just declined the invite to `" + clan.getName() + "`")
                            .setColor(Color.decode(clan.getColor()))
                            .build()
            ).queue();
            leader.getUser().openPrivateChannel().queue((channel -> channel.sendMessageEmbeds(
                    new EmbedBuilder()
                            .setDescription(invite.getMember().getAsMention() + " has declined your invite to `" + clan.getName() + "`")
                            .build()
            ).queue()));
            clan.getMembers().remove(invite.getMember().getIdLong());
            clan.getInvites().remove(invite);
            clan.save();
        }
    }

}
