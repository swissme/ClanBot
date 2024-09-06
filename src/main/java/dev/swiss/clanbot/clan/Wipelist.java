package dev.swiss.clanbot.clan;

import com.mongodb.client.model.Filters;
import dev.swiss.clanbot.ClanBot;
import dev.swiss.clanbot.mongodb.MongoHandler;
import lombok.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.bson.Document;

import java.util.*;

@Data
public class Wipelist {

    private UUID uuid;
    private List<Clan> clans;
    private Message message;
    private TextChannel textChannel;
    private String timestamp;

    private static HashMap<UUID, Wipelist> wipelists = new HashMap<>();

    public Wipelist(UUID uuid, Message message, String timestamp) {
        this.uuid = uuid;
        Document document = MongoHandler.getWipeListClans().find(Filters.eq("uuid", this.uuid.toString())).first();
        if(document == null) {
            this.clans = new ArrayList<>();
            this.message = message;
            this.textChannel = message.getTextChannel();
            this.timestamp = timestamp;
            wipelists.put(uuid, this);

            List<String> clansToNames = new ArrayList<>();

            for (Clan clan : this.clans) clansToNames.add(clan.getName());

            MongoHandler.getWipeListClans().insertOne(
                    new Document()
                            .append("uuid", uuid.toString())
                            .append("clans", clansToNames)
                            .append("message", message.getIdLong())
                            .append("textChannel", textChannel.getIdLong())
                            .append("timestamp", timestamp)
            );
        } else {
            this.clans = new ArrayList<>();
            for (String clanName : document.getList("clans", String.class)) {
                Clan clan = Clan.getByName(clanName);
                if(clan != null) this.clans.add(clan);
            }
            this.message = message;
            this.textChannel = message.getTextChannel();
            this.timestamp = timestamp;
            wipelists.put(uuid, this);
        }
    }

    public static Wipelist getWipelistByUUID(UUID uuid) {
        return wipelists.get(uuid);
    }

    public void delete() {
        MongoHandler.getWipeListClans().deleteOne(Filters.eq("uuid", this.uuid.toString()));
        this.message.delete().queue();
        wipelists.remove(this.uuid);
    }

    public static Wipelist getWipelistByMessage(Message message) {
        for (Wipelist wipelist : wipelists.values()) {
            if(wipelist.getMessage().equals(message)) return wipelist;
        }
        return null;
    }

    public void update() {
        String clanRow1 = "";
        String clanRow2 = "";
        StringBuilder clanRow1Builder = new StringBuilder();
        StringBuilder clanRow2Builder = new StringBuilder();

        for (int i = 0; i < clans.size(); i++) {
            if (i % 2 == 0) { // Even number
                clanRow1Builder
                        .append(clans.get(i).getName())
                        .append(" - ")
                        .append("<@")
                        .append(clans.get(i).getLeader())
                        .append(">")
                        .append(" : ")
                        .append(clans.get(i).getMembers().size())
                        .append("\n");
            } else { // Odd number
                clanRow2Builder
                        .append(clans.get(i).getName())
                        .append(" - ")
                        .append("<@")
                        .append(clans.get(i).getLeader())
                        .append(">")
                        .append(" : ")
                        .append(clans.get(i).getMembers().size())
                        .append("\n");
            }
        }

        clanRow1 = clanRow1Builder.toString();
        clanRow2 = clanRow2Builder.toString();

        int expectedPop = 0;

        for (Clan clan : clans) {
            expectedPop += clan.getMembers().size();
        }

        message.getTextChannel().editMessageEmbedsById(
                message.getIdLong(),
                new EmbedBuilder()
                        .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                        .setTitle(ClanBot.getInstance().getConfig().getServerName() + " Wipelist")
                        .setDescription("Wipe Time? : (" + timestamp + ")")
                        .addField("Clans Signed: " + clans.size(), clanRow1, false)
                        .addField("Expected Pop: " + expectedPop, clanRow2, false)
                        .setFooter(this.uuid.toString())
                        .build()
        ).queue();
    }

    public static boolean isWipelistMessage(Message message) {
        return wipelists.values().stream().anyMatch(wipelist -> wipelist.getMessage().equals(message));
    }

    public static void loadWipelists() {
        wipelists = new HashMap<>();
        for (Document document : MongoHandler.getWipeListClans().find()) {
            TextChannel wipeListTextChannel = ClanBot.getInstance().getJdaInstance().getTextChannelById(document.getLong("textChannel"));
            if(wipeListTextChannel == null) continue;
            Message wipeListMessage = wipeListTextChannel.retrieveMessageById(document.getLong("message")).complete();
            new Wipelist(UUID.fromString(document.getString("uuid")), wipeListMessage, document.getString("timestamp"));
        }
    }

    public EmbedBuilder join(Clan clan) {
        if(clan == null) return new EmbedBuilder().setTitle("Error").setDescription("Clan not found");
        if(clans.contains(clan)) return new EmbedBuilder().setDescription("You are already signed up for this wipe!");
        clans.add(clan);
        update();
        return new EmbedBuilder()
                .setDescription("You have been signed up for this wipe!");
    }

    public EmbedBuilder leave(Clan clan) {
        if(clan == null) return new EmbedBuilder().setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB()).setTitle("Error").setDescription("Clan not found");
        if(!clans.contains(clan)) return new EmbedBuilder().setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB()).setDescription("You are not signed up for this wipe!");
        clans.remove(clan);
        update();
        return new EmbedBuilder()
                .setColor(ClanBot.getInstance().getEmbedRGBColor().getColorFromRGB())
                .setDescription("You have been removed from this wipe!");
    }
}
