package dev.swiss.clanbot.clan.commands;

import dev.swiss.clanbot.clan.*;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.*;
import net.dv8tion.jda.api.hooks.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * @author Swiss (swiss@swissdev.com)
 */
public class StaffCommands extends ListenerAdapter {

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        Member member = event.getMember();
        if (member == null)
            return;

        String[] args = event.getCommandPath().split("/");
        if (args[0].equalsIgnoreCase("staff")) {
            if (Objects.requireNonNull(event.getSubcommandName()).equalsIgnoreCase("deleteclan")) {

                if(!member.hasPermission(Permission.ADMINISTRATOR))
                    return;
                String name = Objects.requireNonNull(event.getOption("name")).getAsString();

                Clan clan = Clan.getByName(name);
                if(clan == null) {
                    event.replyEmbeds(
                            new EmbedBuilder()
                                    .setDescription("`" + name + "` doesn't exist")
                                    .build()
                    ).queue();
                    return;
                }

                event.replyEmbeds(
                        new EmbedBuilder()
                                .setDescription("`" + clan.getName() + "` is now deleted")
                                .build()
                ).queue();
                clan.disband();
            }
        }
    }

}
