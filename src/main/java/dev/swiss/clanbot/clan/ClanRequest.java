package dev.swiss.clanbot.clan;

import com.mongodb.client.model.Filters;
import dev.swiss.clanbot.ClanBot;
import dev.swiss.clanbot.mongodb.MongoHandler;
import lombok.Data;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;

import java.awt.*;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Data
public class ClanRequest {

    private long messageID;
    private long leader;
    private String name;

    @Getter
    private static HashMap<Long, ClanRequest> clanRequests = new HashMap<>();

    public ClanRequest(Long leader, String name) {
        this.leader = leader;
        this.name = name;

        clanRequests.put(this.leader, this);
    }

    public void save() {
        clanRequests.keySet().removeIf(key -> clanRequests.get(key).equals(this));
        clanRequests.put(this.leader, this);

        Document document = new Document()
                .append("messageID", this.messageID)
                .append("name", this.name)
                .append("nameLowerCase", this.name.toLowerCase())
                .append("leader", this.leader);

        MongoHandler.getClanRequests().findOneAndReplace(Filters.eq("nameLowerCase", this.name.toLowerCase()), document);
    }

    public void delete() {
        MongoHandler.getClanRequests().findOneAndDelete(Filters.eq("nameLowerCase", this.name.toLowerCase()));
        clanRequests.keySet().remove(this.leader);
    }

    public static ClanRequest getClanRequest(String name) {
        return clanRequests.values().stream().filter(clanRequest -> clanRequest.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public static ClanRequest getClanRequest(long leader) {
        return clanRequests.get(leader);
    }

    public static ClanRequest getClanRequestByMessageID(long messageID) {
        return clanRequests.values().stream().filter(clanRequest -> clanRequest.getMessageID() == messageID).findFirst().orElse(null);
    }

    public static boolean isClanRequestMessage(long messageID) {
        return clanRequests.values().stream().anyMatch(clanRequest -> clanRequest.getMessageID() == messageID);
    }

    public static void loadClanRequests() {
        for (Document document : MongoHandler.getClanRequests().find()) {
            ClanRequest clanRequest = new ClanRequest(document.getLong("leader"), document.getString("name"));
            clanRequest.setMessageID(document.getLong("messageID"));
        }
    }

    public void accept(long messageID) {
        TextChannel adminLogChannel = ClanBot.getInstance().getAdminLogChannel();
        if(adminLogChannel == null) return;

        Message clanRequestMessage = adminLogChannel.retrieveMessageById(this.messageID).complete();

        if(clanRequestMessage == null) {
            adminLogChannel.sendMessageEmbeds(
                    new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("The clan request message could not be found. Please contact a developer.")
                            .setFooter("*Please note that the request has been automatically deleted.*")
                            .build()
            ).queue((message) -> message.delete().queueAfter(10, TimeUnit.SECONDS));
            return;
        }

        User userLeader = ClanBot.getInstance().getJdaInstance().retrieveUserById(this.leader).complete();
        Member memberLeader = null;
        if(ClanBot.getInstance().getGuild().isMember(userLeader)) {
            memberLeader = ClanBot.getInstance().getGuild().retrieveMember(userLeader).complete();
        }

        if(memberLeader == null) {
            ClanRequest request = ClanRequest.getClanRequestByMessageID(messageID);
            if(request != null) request.delete();
            clanRequestMessage.delete().queue();
            adminLogChannel.sendMessageEmbeds(
                    new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("The leader of the clan request isn't in the discord anymore.")
                            .setFooter("*Please note that the request will be automatically deleted in 10 seconds.*")
                            .build()
            ).queue((message) -> message.delete().queueAfter(10, TimeUnit.SECONDS));
            return;
        }

        clanRequestMessage.delete().queue();
        clanRequestMessage.replyEmbeds(
                new EmbedBuilder()
                        .setDescription("You have just accepted the clan named `" + this.name + "`, requested by " + memberLeader.getAsMention())
                        .setColor(Color.GREEN)
                        .build()
        ).queue((message) -> message.delete().queueAfter(10, TimeUnit.SECONDS));

        new Clan(this.name, this.leader);
        this.delete();
    }

    public void deny(long messageID) {
        TextChannel adminLogChannel = ClanBot.getInstance().getAdminLogChannel();
        if(adminLogChannel == null) return;

        Message clanRequestMessage = adminLogChannel.retrieveMessageById(this.messageID).complete();

        if(clanRequestMessage == null) {
            adminLogChannel.sendMessageEmbeds(
                    new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("The clan request message could not be found. Please contact a developer.")
                            .setFooter("*Please note that the request has been automatically deleted.*")
                            .build()
            ).queue((message) -> message.delete().queueAfter(10, TimeUnit.SECONDS));
            return;
        }

        User userLeader = ClanBot.getInstance().getJdaInstance().retrieveUserById(this.leader).complete();
        Member memberLeader = null;
        if(ClanBot.getInstance().getGuild().isMember(userLeader)) {
            memberLeader = ClanBot.getInstance().getGuild().retrieveMember(userLeader).complete();
        }

        if(memberLeader == null) {
            adminLogChannel.sendMessageEmbeds(
                    new EmbedBuilder()
                            .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                            .setDescription("The leader of the clan request could not be found. Please contact a developer.")
                            .setFooter("*Please note that the request has been automatically deleted.*")
                            .build()
            ).queue((message) -> message.delete().queueAfter(10, TimeUnit.SECONDS));
            return;
        }

        clanRequestMessage.delete().queue();
        clanRequestMessage.replyEmbeds(
                new EmbedBuilder()
                        .setDescription("You have just denied the clan named `" + this.name + "`, requested by " + memberLeader.getAsMention())
                        .setColor(Color.RED)
                        .build()
        ).queue((message) -> message.delete().queueAfter(10, TimeUnit.SECONDS));

        this.delete();
    }

}
