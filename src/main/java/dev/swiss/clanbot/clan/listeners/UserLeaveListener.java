package dev.swiss.clanbot.clan.listeners;

import dev.swiss.clanbot.clan.*;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.member.*;
import net.dv8tion.jda.api.hooks.*;
import org.jetbrains.annotations.*;

/**
 * @author Swiss (swiss@swissdev.com)
 */
public class UserLeaveListener extends ListenerAdapter {

    @Override
    public void onGuildMemberLeave(@NotNull GuildMemberLeaveEvent event) {
        Member member = event.getMember();
        Clan clan = Clan.getByMember(member);
        if(clan == null)
            return;
        if(clan.getLeader() == member.getIdLong()) {
            clan.disband();
            return;
        }
        clan.getMembers().remove(member.getIdLong());
        clan.getColeaders().remove(member.getIdLong());
        clan.save();
    }

}
