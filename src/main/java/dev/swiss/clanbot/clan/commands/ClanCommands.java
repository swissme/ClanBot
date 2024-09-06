package dev.swiss.clanbot.clan.commands;

import com.mongodb.client.model.Filters;
import dev.swiss.clanbot.*;
import dev.swiss.clanbot.clan.*;
import dev.swiss.clanbot.clan.Invite;
import dev.swiss.clanbot.mongodb.MongoHandler;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.*;
import net.dv8tion.jda.api.exceptions.*;
import net.dv8tion.jda.api.hooks.*;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.*;
import org.bson.Document;
import org.jetbrains.annotations.*;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;

import static net.dv8tion.jda.api.requests.ErrorResponse.CANNOT_SEND_TO_USER;

/**
 * @author Swiss (swiss@swissdev.com)
 */
public class ClanCommands extends ListenerAdapter {

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        Member member = event.getMember();
        if(member == null)
            return;
        String[] args = event.getCommandPath().split("/");
        if(args[0].equalsIgnoreCase("clan")) {
            if(Objects.requireNonNull(event.getSubcommandName()).equalsIgnoreCase("create")) {
                String name = Objects.requireNonNull(event.getOption("name")).getAsString();
                if(Clan.exists(name)) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("That clan already exists")
                            .build()
                    ).queue();
                    return;
                }
                if(Clan.inClan(member)) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You already in a clan")
                            .build()
                    ).queue();
                    return;
                }
                if(ClanRequest.getClanRequests().containsKey(member.getIdLong())) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You already have a clan request")
                            .build()
                    ).queue();
                    return;
                }
                for (Long leaderLoop : ClanRequest.getClanRequests().keySet()) {
                    ClanRequest clanRequest = ClanRequest.getClanRequests().get(leaderLoop);
                    if(clanRequest == null) continue;
                    if(clanRequest.getName().equalsIgnoreCase(name)) {
                        event.replyEmbeds(
                            new EmbedBuilder()
                                .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                                .setDescription("A clan request with that name already exists")
                                .build()
                        ).queue();
                        return;
                    }
                }

                ClanRequest clanRequest = new ClanRequest(member.getIdLong(), name);

                TextChannel adminLogChannel = ClanBot.getInstance().getAdminLogChannel();

                if (adminLogChannel != null) {
                    List<Button> buttons = new ArrayList<>();
                    buttons.add(Button.success("accept", "Accept"));
                    buttons.add(Button.danger("deny", "Deny"));
                    adminLogChannel.sendMessageEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setTitle("New Clan Creation Request")
                            .setDescription("**Clan Name:**\n> " + clanRequest.getName() + "\n**Clan Leader:**\n> " + member.getAsMention() + " / (" + member.getUser().getAsTag() + ")")
                            .build()
                    ).setActionRow(buttons).queue(message -> {
                        MongoHandler.getClanRequests().insertOne(new Document()
                            .append("messageID", message.getIdLong())
                            .append("name", clanRequest.getName())
                            .append("nameLowerCase", clanRequest.getName().toLowerCase())
                            .append("leader", clanRequest.getLeader()));
                        clanRequest.setMessageID(message.getIdLong());
                    });
                }

                event.replyEmbeds(
                    new EmbedBuilder()
                        .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                        .setDescription("Successfully requested to create the clan named `" + name + "`")
                        .build()
                ).queue();
            } else if(Objects.requireNonNull(event.getSubcommandName()).equalsIgnoreCase("disband")) {
                if(!Clan.inClan(member)) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You are not in a clan")
                            .build()
                    ).queue();
                    return;
                }
                Clan clan = Clan.getByMember(member);
                assert clan != null;
                if(clan.getLeader() != member.getIdLong()) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You are not the leader of `" + clan.getName() + "`")
                            .build()
                    ).queue();
                    return;
                }
                for (long clanMember : clan.getMembers()) {
                    PrivateChannel pc = ClanBot.getInstance().getGuild().retrieveMemberById(clanMember).complete().getUser().openPrivateChannel().complete();

                    try {
                        pc.sendMessageEmbeds(
                            new EmbedBuilder()
                                .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                                .setDescription("Your clan `" + clan.getName() + "` has been disbanded")
                                .build()
                        ).queue();
                    } catch(ErrorResponseException ignored) {

                    }
                }
                event.replyEmbeds(
                    new EmbedBuilder()
                        .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                        .setDescription("Successfully disbanded your clan named `" + clan.getName() + "`")
                        .build()
                ).queue();
                clan.disband();
            } else if(Objects.requireNonNull(event.getSubcommandName()).equalsIgnoreCase("info")) {
                Clan clan = null;
                if(event.getOption("name") == null) {
                    clan = Clan.getByMember(member);
                    if(clan == null) {
                        event.replyEmbeds(
                            new EmbedBuilder()
                                .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                                .setDescription("You are not in a clan")
                                .build()
                        ).queue();
                        return;
                    }
                }
                if(clan == null) {
                    String name = Objects.requireNonNull(event.getOption("name")).getAsString();
                    clan = Clan.getByName(name);
                    if(clan == null) {
                        event.replyEmbeds(
                            new EmbedBuilder()
                                .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                                .setDescription("That clan doesn't exist")
                                .build()
                        ).queue();
                        return;
                    }
                }

                List<String> memberStrings = new ArrayList<>();
                for (long clanMember : clan.getMembers()) {
                    memberStrings.add(ClanBot.getInstance().getJdaInstance().retrieveUserById(clanMember).complete().getAsMention());
                }

                List<String> coLeaderStrings = new ArrayList<>();
                if(clan.getColeaders().size() != 0) {
                    for (long clanCoLeader : clan.getColeaders()) {
                        coLeaderStrings.add(ClanBot.getInstance().getGuild().retrieveMemberById(clanCoLeader).complete().getAsMention());
                    }
                }

                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setTitle("Clan Info For: `" + clan.getName() + "` **(** *Members:* **" + clan.getMembers().size() + "** **)**")
                        .setColor(Color.decode(clan.getColor()))
                        .setThumbnail(clan.getLogo())
                        .addField("**Leader:**", ClanBot.getInstance().getGuild().retrieveMemberById(clan.getLeader()).complete().getAsMention(), true);
                if(clan.getColeaders().size() != 0)
                    embedBuilder.addField("**Co-Leaders:**", String.join("\n ", coLeaderStrings), true);
                else
                    embedBuilder.addField("**Co-Leaders:**", "None", true);

                if(clan.getMembers().size() != 0)
                    embedBuilder.addField("**Members:**", String.join("\n ", memberStrings), false);
                else
                    embedBuilder.addField("**Members:**", "None", false);

                embedBuilder.addField("**Clan Socials:**", "[Clan Discord](" + clan.getDiscord() + ") | [Clan Youtube](" + clan.getYoutube() + ")", false);

                event.replyEmbeds(
                        embedBuilder
                                .build()
                ).queue();
            } else if(Objects.requireNonNull(event.getSubcommandName()).equalsIgnoreCase("changeleader")) {
                Clan clan = Clan.getByMember(member);
                if(clan == null) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You are not in a clan")
                            .build()
                    ).queue();
                    return;
                }
                if(clan.getLeader() != member.getIdLong()) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You are not the leader of `" + clan.getName() + "`")
                            .build()
                    ).queue();
                    return;
                }
                IMentionable mentioned = Objects.requireNonNull(event.getOption("newleader")).getAsMentionable();
                Member newLeader = ClanBot.getInstance().getGuild().retrieveMemberById(mentioned.getIdLong()).complete();
                if(newLeader.getIdLong() == member.getIdLong()) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You cannot set the leader to yourself")
                            .build()
                    ).queue();
                    return;
                }
                if(!clan.getMembers().contains(newLeader.getIdLong())) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription(newLeader.getAsMention() + " is not in your clan")
                            .build()
                    ).queue();
                    return;
                }
                ClanBot.getInstance().getGuild().addRoleToMember(newLeader, ClanBot.getInstance().getLeaderRole()).queue();
                ClanBot.getInstance().getGuild().removeRoleFromMember(member, ClanBot.getInstance().getLeaderRole()).queue();
                clan.getColeaders().remove(newLeader.getIdLong());
                clan.setLeader(newLeader.getIdLong());
                clan.save();
                event.replyEmbeds(
                    new EmbedBuilder()
                        .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                        .setDescription("You have set the leader position to " + newLeader.getAsMention())
                        .build()
                ).queue();
            } else if(Objects.requireNonNull(event.getSubcommandName()).equalsIgnoreCase("promote")) {
                Clan clan = Clan.getByMember(member);
                if(clan == null) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You are not in a clan")
                            .build()
                    ).queue();
                    return;
                }
                if(clan.getLeader() != member.getIdLong()) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You are not the leader of `" + clan.getName() + "`")
                            .build()
                    ).queue();
                    return;
                }
                IMentionable mentioned = Objects.requireNonNull(event.getOption("member")).getAsMentionable();
                Member target = ClanBot.getInstance().getGuild().retrieveMemberById(mentioned.getIdLong()).complete();
                if(target.getIdLong() == member.getIdLong()) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You cannot promote yourself")
                            .build()
                    ).queue();
                    return;
                }
                if(!clan.getMembers().contains(target.getIdLong())) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription(target.getAsMention() + " is not in your clan")
                            .build()
                    ).queue();
                    return;
                }
                if(clan.getColeaders().contains(target.getIdLong())) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription(target.getAsMention() + " is already a coleader")
                            .build()
                    ).queue();
                    return;
                }
                ClanBot.getInstance().getGuild().addRoleToMember(target, ClanBot.getInstance().getCoLeaderRole()).queue();
                clan.getColeaders().add(target.getIdLong());
                clan.save();
                event.replyEmbeds(
                    new EmbedBuilder()
                        .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                        .setDescription(target.getAsMention() + " is now promoted to coleader")
                        .build()
                ).queue();
            } else if(Objects.requireNonNull(event.getSubcommandName()).equalsIgnoreCase("demote")) {
                Clan clan = Clan.getByMember(member);
                if(clan == null) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You are not in a clan")
                            .build()
                    ).queue();
                    return;
                }
                if(clan.getLeader() != member.getIdLong()) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You are not the leader of `" + clan.getName() + "`")
                            .build()
                    ).queue();
                    return;
                }
                IMentionable mentioned = Objects.requireNonNull(event.getOption("member")).getAsMentionable();
                Member target = ClanBot.getInstance().getGuild().retrieveMemberById(mentioned.getIdLong()).complete();
                if(target.getIdLong() == member.getIdLong()) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You cannot demote yourself")
                            .build()
                    ).queue();
                    return;
                }
                if(!clan.getMembers().contains(target.getIdLong())) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription(target.getAsMention() + " is not in your clan")
                            .build()
                    ).queue();
                    return;
                }
                if(!clan.getColeaders().contains(target.getIdLong())) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription(target.getAsMention() + " isn't a coleader")
                            .build()
                    ).queue();
                    return;
                }
                ClanBot.getInstance().getGuild().removeRoleFromMember(target, ClanBot.getInstance().getCoLeaderRole()).queue();
                clan.getColeaders().remove(target.getIdLong());
                clan.save();
                event.replyEmbeds(
                    new EmbedBuilder()
                        .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                        .setDescription(target.getAsMention() + " is demoted from coleader")
                        .build()
                ).queue();
            } else if(Objects.requireNonNull(event.getSubcommandName()).equalsIgnoreCase("invite")) {
                Clan clan = Clan.getByMember(member);
                if(clan == null) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You are not in a clan")
                            .build()
                    ).queue();
                    return;
                }
                IMentionable mentioned = Objects.requireNonNull(event.getOption("member")).getAsMentionable();
                Member target = ClanBot.getInstance().getGuild().retrieveMemberById(mentioned.getIdLong()).complete();
                if(target.getIdLong() == member.getIdLong()) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You cannot invite yourself")
                            .build()
                    ).queue();
                    return;
                }
                if(clan.getMembers().contains(target.getIdLong())) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription(target.getAsMention() + " is already in your clan")
                            .build()
                    ).queue();
                    return;
                }
                if(Clan.getByMember(target) != null) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("That user is already in a clan")
                            .build()
                    ).queue();
                    return;
                }
                if(clan.getInvites().stream().anyMatch(invite -> invite.getMember().getIdLong() == target.getIdLong())) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription(target.getAsMention() + " is already invited to `" + clan.getName() + "`")
                            .build()
                    ).queue();
                    return;
                }
                if(clan.getLeader() == member.getIdLong()) {
                    event.reply(".").setEphemeral(true).queue();
                    ClanBot.fastInviteClan(event, target, clan);
                    return;
                }
                if(clan.getColeaders().contains(member.getIdLong())) {
                    event.reply(".").setEphemeral(true).queue();
                    ClanBot.fastInviteClan(event, target, clan);
                }
            } else if(Objects.requireNonNull(event.getSubcommandName()).equalsIgnoreCase("kick")) {
                Clan clan = Clan.getByMember(member);
                if(clan == null) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You are not in a clan")
                            .build()
                    ).queue();
                    return;
                }
                IMentionable mentioned = Objects.requireNonNull(event.getOption("member")).getAsMentionable();
                Member target = ClanBot.getInstance().getGuild().retrieveMemberById(mentioned.getIdLong()).complete();
                if(target.getIdLong() == member.getIdLong()) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You cannot kick yourself")
                            .build()
                    ).queue();
                    return;
                }
                if(!clan.getMembers().contains(target.getIdLong())) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription(target.getAsMention() + " is not in your clan")
                            .build()
                    ).queue();
                    return;
                }
                if(clan.getColeaders().contains(member.getIdLong())) {
                    if(clan.getColeaders().contains(target.getIdLong())) {
                        event.replyEmbeds(
                            new EmbedBuilder()
                                .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                                .setDescription("You cannot kick another coleader")
                                .build()
                        ).queue();
                        return;
                    }
                    if(clan.getLeader() == target.getIdLong()) {
                        event.replyEmbeds(
                            new EmbedBuilder()
                                .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                                .setDescription("You cannot kick a leader")
                                .build()
                        ).queue();
                        return;
                    }
                }
                if(clan.getColeaders().contains(target.getIdLong())) {
                    ClanBot.getInstance().getGuild().removeRoleFromMember(target, ClanBot.getInstance().getCoLeaderRole()).queue();
                }
                if(clan.getLeader() == member.getIdLong()) {
                    clan.getColeaders().remove(target.getIdLong());
                    clan.getMembers().remove(target.getIdLong());
                    clan.save();
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You have kicked " + target.getAsMention() + " from `" + clan.getName() + "`")
                            .build()
                    ).queue();
                    return;
                }
                if(clan.getColeaders().contains(member.getIdLong())) {
                    clan.getColeaders().remove(target.getIdLong());
                    clan.getMembers().remove(target.getIdLong());
                    clan.save();
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You have kicked " + target.getAsMention() + " from `" + clan.getName() + "`")
                            .build()
                    ).queue();
                }
            } else if(Objects.requireNonNull(event.getSubcommandName()).equalsIgnoreCase("setlogo")) {
                Clan clan = Clan.getByMember(member);
                if(clan == null) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You are not in a clan")
                            .build()
                    ).queue();
                    return;
                }
                if(clan.getLeader() != member.getIdLong()) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You are not the leader of `" + clan.getName() + "`")
                            .build()
                    ).queue();
                    return;
                }
                String logo = Objects.requireNonNull(event.getOption("logo")).getAsString();
                if(!ClanBot.isValid(logo)) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("Please use a valid image url")
                            .build()
                    ).queue();
                    return;
                }
                clan.setLogo(logo);
                clan.save();
                event.replyEmbeds(
                    new EmbedBuilder()
                        .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                        .setDescription("Successfully set the logo")
                        .setThumbnail(logo)
                        .build()
                ).queue();
            } else if(Objects.requireNonNull(event.getSubcommandName()).equalsIgnoreCase("setcolor")) {
                Clan clan = Clan.getByMember(member);
                if(clan == null) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You are not in a clan")
                            .build()
                    ).queue();
                    return;
                }
                if(clan.getLeader() != member.getIdLong()) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You are not the leader of `" + clan.getName() + "`")
                            .build()
                    ).queue();
                    return;
                }
                String color = Objects.requireNonNull(event.getOption("color")).getAsString();
                if(!ClanBot.isValidHexCode(color)) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("Please use a valid hex color code")
                            .build()
                    ).queue();
                    return;
                }
                clan.setColor(color);
                clan.save();
                event.replyEmbeds(
                    new EmbedBuilder()
                        .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                        .setDescription("Successfully set the color")
                        .build()
                ).queue();
            } else if(Objects.requireNonNull(event.getSubcommandName()).equalsIgnoreCase("setdiscord")) {
                Clan clan = Clan.getByMember(member);
                if(clan == null) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You are not in a clan")
                            .build()
                    ).queue();
                    return;
                }
                if(clan.getLeader() != member.getIdLong()) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You are not the leader of `" + clan.getName() + "`")
                            .build()
                    ).queue();
                    return;
                }
                String discord = Objects.requireNonNull(event.getOption("discord")).getAsString();
                if(!ClanBot.isValidDiscordInvite(discord)) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("Please use a valid discord")
                            .build()
                    ).queue();
                    return;
                }
                clan.setDiscord(discord);
                clan.save();
                event.replyEmbeds(
                    new EmbedBuilder()
                        .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                        .setDescription("Successfully set the discord")
                        .build()
                ).queue();
            } else if(Objects.requireNonNull(event.getSubcommandName()).equalsIgnoreCase("leave")) {
                Clan clan = Clan.getByMember(member);
                if(clan == null) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You are not in a clan")
                            .build()
                    ).queue();
                    return;
                }
                if(clan.getLeader() == member.getIdLong()) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("Use **/clan disband** instead")
                            .build()
                    ).queue();
                    return;
                }
                if(clan.getColeaders().contains(member.getIdLong())) {
                    ClanBot.getInstance().getGuild().removeRoleFromMember(member, ClanBot.getInstance().getCoLeaderRole()).queue();
                }
                clan.getColeaders().remove(member.getIdLong());
                clan.removeMember(member);
                clan.save();
                event.replyEmbeds(
                    new EmbedBuilder()
                        .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                        .setDescription("Successfully left `" + clan.getName() + "`")
                        .build()
                ).queue();
            } else if(Objects.requireNonNull(event.getSubcommandName()).equalsIgnoreCase("setyoutube")) {
                Clan clan = Clan.getByMember(member);
                if(clan == null) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You are not in a clan")
                            .build()
                    ).queue();
                    return;
                }
                if(clan.getLeader() != member.getIdLong()) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("You are not the leader of `" + clan.getName() + "`")
                            .build()
                    ).queue();
                    return;
                }
                String youtube = Objects.requireNonNull(event.getOption("youtube")).getAsString();
                if(!ClanBot.isValidYoutube(youtube)) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("Please use a valid youtube")
                            .build()
                    ).queue();
                    return;
                }
                clan.setYoutube(youtube);
                clan.save();
                event.replyEmbeds(
                    new EmbedBuilder()
                        .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                        .setDescription("Successfully set the youtube")
                        .build()
                ).queue();
            }
        }
    }

}
