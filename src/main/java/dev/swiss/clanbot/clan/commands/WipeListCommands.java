package dev.swiss.clanbot.clan.commands;

import dev.swiss.clanbot.ClanBot;
import dev.swiss.clanbot.clan.Wipelist;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.utils.Timestamp;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class WipeListCommands extends ListenerAdapter {

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        Member member = event.getMember();
        if (member == null)
            return;

        String[] args = event.getCommandPath().split("/");
        if (args[0].equalsIgnoreCase("wipelist")) {
            if (Objects.requireNonNull(event.getSubcommandName()).equalsIgnoreCase("create")) {

                if(!member.hasPermission(Permission.ADMINISTRATOR))
                    return;
                String timestamp = Objects.requireNonNull(event.getOption("timestamp")).getAsString();

                List<Button> buttons = new ArrayList<>();
                buttons.add(net.dv8tion.jda.api.interactions.components.Button.success("join", "Join"));
                buttons.add(Button.danger("leave", "Leave"));
                event.getTextChannel().sendMessageEmbeds(
                    new EmbedBuilder()
                        .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                        .setTitle(ClanBot.getInstance().getConfig().getServerName() + " Wipelist")
                        .setDescription("Loading...")
                        .build()
                ).setActionRow(buttons).queue(message -> {
                    Wipelist wipelist = new Wipelist(UUID.randomUUID(), message, timestamp);
                    wipelist.update();
                });
                event.replyEmbeds(
                    new EmbedBuilder()
                        .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                        .setDescription("Wipelist created!")
                        .build()
                ).setEphemeral(true).queue();
            } else if (Objects.requireNonNull(event.getSubcommandName()).equalsIgnoreCase("delete")) {

                if (!member.hasPermission(Permission.ADMINISTRATOR))
                    return;
                UUID uuid = UUID.fromString(Objects.requireNonNull(event.getOption("uuid")).getAsString());

                Wipelist wipelist = Wipelist.getWipelistByUUID(uuid);
                if (wipelist == null) {
                    event.replyEmbeds(
                        new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("Wipelist not found!")
                            .build()
                    ).setEphemeral(true).queue();
                    return;
                }
                wipelist.delete();
                event.replyEmbeds(
                    new EmbedBuilder()
                        .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                        .setDescription("Wipelist `" + uuid + "` deleted!")
                        .build()
                ).setEphemeral(true).queue();
            }
        }
    }
}
