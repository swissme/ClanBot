package dev.swiss.clanbot.clan;

import com.mongodb.client.model.*;
import dev.swiss.clanbot.*;
import dev.swiss.clanbot.mongodb.*;
import lombok.*;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.components.*;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.managers.*;
import org.bson.*;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author Swiss (swiss@swissdev.com)
 */

@Data
public class Clan {

    @Getter
    private static List<Clan> clans = new ArrayList<>();

    private String name;
    private long leader;
    private List<Long> coleaders;
    private List<Long> members;
    private String logo;
    private String discord;
    private String youtube;
    private String color;
    private long channel;
    private long role;
    private long createdAt;
    private List<Invite> invites;

    public Clan(String name, long leader) {
        this.name = name;
        this.leader = leader;

        this.load();
    }

    public static Clan getByName(String name) {
        return clans.stream().filter(clan -> clan.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public void load() {
        Document document = MongoHandler.getClans().find(Filters.eq("nameLowerCase", this.name.toLowerCase())).first();
        if(document != null) {

            this.coleaders = new ArrayList<>();
            if(document.getList("coleaders", Long.class) != null) {
                this.coleaders.addAll(document.getList("coleaders", Long.class));
            }
            this.members = new ArrayList<>();
            this.members.addAll(document.getList("members", Long.class));

            this.logo = document.getString("logo");
            this.discord = document.getString("discord");
            this.color = document.getString("color");
            this.createdAt = document.getLong("createdAt");
            this.role = document.getLong("role");
            this.channel = document.getLong("channel");
            this.youtube = document.getString("youtube");

            this.invites = new ArrayList<>();
            for (Document doc : document.getList("invites", Document.class)) {
                if(!ClanBot.getInstance().getGuild().isMember(User.fromId(doc.getLong("member"))))
                    continue;
                this.invites.add(
                        new Invite(
                                ClanBot.getInstance().getGuild().retrieveMemberById(doc.getLong("member")).complete(),
                                doc.getLong("expireTime"),
                                doc.getLong("messageID")
                        )
                );
            }

            clans.removeIf(clan -> clan.getName().equalsIgnoreCase(this.name));
            clans.add(this);

            return;

        }
        this.coleaders = new ArrayList<>();
        this.members = new ArrayList<>();
        this.logo = "https://cdn.discordapp.com/attachments/993901818864222369/993940560245162106/dwadawd.png";
        this.discord = "Set with **/clan setdiscord** *<discord invite>*";
        this.color = "#000000";
        ClanBot.getInstance().getGuild().addRoleToMember(this.leader, ClanBot.getInstance().getLeaderRole()).queue();
        this.role = ClanBot.getInstance().getGuild().createRole()
                .setName(this.name)
                .setHoisted(true)
                .setColor(Color.BLACK)
                .setMentionable(false)
                .complete()
                .getIdLong();
        Role roleObject = ClanBot.getInstance().getGuild().getRoleById(this.role);
        ClanBot.getInstance().getGuild().addRoleToMember(this.leader, Objects.requireNonNull(roleObject)).queue();
        this.channel = ClanBot.getInstance().getGuild().createTextChannel(this.name, ClanBot.getInstance().getClanCategory())
                .addPermissionOverride(roleObject, Collections.singleton(Permission.MESSAGE_WRITE), null)
                .addPermissionOverride(ClanBot.getInstance().getGuild().getPublicRole(), null, Collections.singleton(Permission.MESSAGE_WRITE))
                .complete()
                .getIdLong();
        TextChannel channelObject = ClanBot.getInstance().getGuild().getTextChannelById(this.channel);
        assert channelObject != null;
        channelObject.sendMessage(roleObject.getAsMention()).queue();
        channelObject.sendMessageEmbeds(
                new EmbedBuilder()
                        .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                        .setDescription("**Welcome to the " + ClanBot.getInstance().getConfig().getServerName() + " Clan System**\n" +
                                "You can use the following commands:\n\n" +
                                "> **/clan invite** <member> - Invites a user to your clan.\n" +
                                "> **/clan kick** <member> - Kick someone from your clan.\n" +
                                "> **/clan setlogo** <image link> - Set a clanlogo/avatar.\n" +
                                "> **/clan setcolor** <hexcode> - Change your clans role & embed's color.\n" +
                                "> **/clan setdiscord** <discord invite> - Add your clans discord.\n" +
                                "> **/clan setyoutube** <youtube link> - Add your youtubes discord.\n\n" +
                                "*Please contact support if you encounter any issues during this process.*")
                        .build()
        ).queue();
        this.createdAt = System.currentTimeMillis();
        this.youtube = "Set with **/clan setyoutube** *<youtube link>*";
        this.invites = new ArrayList<>();
        this.members.add(this.leader);

        clans.add(this);
        MongoHandler.getClans().insertOne(new Document()
                .append("name", this.name)
                .append("nameLowerCase", this.name.toLowerCase())
                .append("leader", this.leader)
                .append("members", this.members)
                .append("logo", this.logo)
                .append("discord", this.discord)
                .append("color", this.color)
                .append("role", this.role)
                .append("channel", this.channel)
                .append("createdAt", this.createdAt)
                .append("invites", this.invites)
                .append("coleaders", this.coleaders));
    }

    public void save() {
        clans.removeIf(clan -> clan.getName().equalsIgnoreCase(this.name));
        clans.add(this);

        List<Document> newInvites = new ArrayList<>();
        for (Invite invite : this.invites) {
            newInvites.add(
                    new Document()
                            .append("member", invite.getMember().getIdLong())
                            .append("expireTime", invite.getExpiredTime())
                            .append("messageID", invite.getMessageID())
            );
        }

        Document document = new Document()
                .append("name", this.name)
                .append("nameLowerCase", this.name.toLowerCase())
                .append("members", members)
                .append("logo", this.logo)
                .append("discord", this.discord)
                .append("color", this.color)
                .append("role", this.role)
                .append("channel", this.channel)
                .append("createdAt", this.createdAt)
                .append("leader", this.leader)
                .append("invites", newInvites);
        if(this.coleaders.size() != 0) {
            document.append("coleaders", this.coleaders);
        }
        MongoHandler.getClans().findOneAndReplace(Filters.eq("nameLowerCase", this.name.toLowerCase()), document);
    }

    public void addMember(Member member) {
        this.members.add(member.getIdLong());
        ClanBot.getInstance().getGuild().addRoleToMember(member, Objects.requireNonNull(ClanBot.getInstance().getGuild().getRoleById(this.role))).queue();
    }

    public void removeMember(Member member) {
        this.members.remove(member.getIdLong());
        ClanBot.getInstance().getGuild().removeRoleFromMember(member, Objects.requireNonNull(ClanBot.getInstance().getGuild().getRoleById(this.role))).queue();
    }

    public void disband() {
        ClanBot.getInstance().getGuild().removeRoleFromMember(this.leader, ClanBot.getInstance().getLeaderRole()).queue();
        for (long coleader : this.coleaders) {
            ClanBot.getInstance().getGuild().removeRoleFromMember(coleader, ClanBot.getInstance().getCoLeaderRole()).queue();
        }
        Objects.requireNonNull(ClanBot.getInstance().getGuild().getRoleById(this.role)).delete().complete();
        Objects.requireNonNull(ClanBot.getInstance().getGuild().getTextChannelById(this.channel)).delete().complete();
        MongoHandler.getClans().deleteOne(Filters.eq("nameLowerCase", this.name.toLowerCase()));
        clans.removeIf(clan -> clan.getName().equalsIgnoreCase(this.name));
    }

    public void invite(Member member) {
        PrivateChannel pc = member.getUser().openPrivateChannel().complete();
        List<Button> buttons = new ArrayList<>();
        buttons.add(Button.success("join", "Join").withEmoji(Emoji.fromMarkdown("<a:yes:981664413054537769>")));
        buttons.add(Button.danger("deny", "Deny").withEmoji(Emoji.fromMarkdown("<a:no:981664577202839562>")));
        pc.sendMessageEmbeds(
                new EmbedBuilder()
                        .setTitle("Clan Invitation: `" + this.name + "`")
                        .setDescription("To join this clan please react with the check mark. If you do not choose to join this clan, hit the cross")
                        .setColor(Color.decode(this.color))
                        .build()
        ).setActionRow(buttons).queue(message -> {
            Invite invite = new Invite(
                    member,
                    System.currentTimeMillis() + 86400000,
                    message.getIdLong()
            );
            this.invites.add(
                    invite
            );
            save();
        });
    }

    public static boolean exists(String name) {
        return clans.stream().anyMatch(clan -> clan.getName().equalsIgnoreCase(name));
    }

    public static boolean inClan(Member member) {
        return clans.stream().anyMatch(clan -> clan.getMembers().contains(member.getIdLong()));
    }

    public static Clan getByMember(Member member) {
        return clans.stream().filter(clan -> clan.getMembers().contains(member.getIdLong())).findFirst().orElse(null);
    }

    public static Clan getClanByInviteMessage(long id) {
        for (Clan clan : clans)
            for (Invite invite : clan.getInvites())
                if(invite.getMessageID() == id)
                    return clan;

        return null;
    }

    public static Invite getInviteByInviteMessage(long id) {
        for (Clan clan : clans)
            for (Invite invite : clan.getInvites())
                if (invite.getMessageID() == id)
                    return invite;

        return null;
    }

    public void setColor(String color) {
        this.color = color;
        Objects.requireNonNull(ClanBot.getInstance().getGuild().getRoleById(this.role)).getManager().setColor(Color.decode(color)).queue();
    }

    public static void loadClans() {
        for (Document document : MongoHandler.getClans().find()) {
            Clan clan = new Clan(document.getString("name"), document.getLong("leader"));
        }
    }

}
