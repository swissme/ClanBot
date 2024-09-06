package dev.swiss.clanbot;

import com.fasterxml.jackson.databind.*;
import com.google.gson.*;
import dev.swiss.clanbot.clan.*;
import dev.swiss.clanbot.clan.commands.*;
import dev.swiss.clanbot.clan.listeners.*;
import dev.swiss.clanbot.config.Config;
import dev.swiss.clanbot.listeners.*;
import dev.swiss.clanbot.mongodb.*;
import lombok.*;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.*;
import net.dv8tion.jda.api.exceptions.*;
import net.dv8tion.jda.api.interactions.commands.*;
import net.dv8tion.jda.api.interactions.commands.build.*;
import net.dv8tion.jda.api.requests.*;
import net.dv8tion.jda.api.requests.restaction.*;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.regex.*;
import java.util.regex.Matcher;

/**
 * @author Swiss (swiss@swissdev.com)
 */
public class ClanBot {

    @Getter
    private Guild guild;

    @Getter
    private final Config config;

    @Getter
    private MongoHandler mongoHandler;

    @Getter
    private Category clanCategory;

    @Getter
    private TextChannel adminLogChannel;

    @Getter
    private RGB embedRGBColor;

    @Getter
    private Role leaderRole;

    @Getter
    private Role coLeaderRole;

    @Getter
    private final Gson gson = new Gson();

    @Getter
    private JDA jdaInstance;

    @Getter
    private static ClanBot instance;

    public static void main(String[] args) {
        new ClanBot();
    }

    @SneakyThrows
    public ClanBot() {
        instance = this;
        ObjectMapper objectMapper = new ObjectMapper();
        /* ---- Creates the config.json and/or loads it. ---- */
        File file = getResourceAsFile("config.json");
        String userDirectory = System.getProperty("user.dir");
        File newFile = new File(userDirectory + "/config.json");

//        if (!newFile.exists()) Files.move(
//                file.getAbsoluteFile().toPath(),
//                newFile.getAbsoluteFile().toPath()
//        );

        Path path = newFile.getAbsoluteFile().toPath();
        byte[] data = Files.readAllBytes(path);
        config = objectMapper.readValue(data, Config.class);
        /* ---------------------------------------------------*/
        if (config.getToken().isEmpty()) {
            System.out.println("Token is invalid");
            System.exit(0);
            return;
        }
        JDA jda = JDABuilder.createDefault(config.getToken()).build();
        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        jdaInstance = jda;
        if (config.getGuild().isEmpty()) {
            System.out.println("Guild is invalid");
            System.exit(0);
            return;
        }
        guild = jda.getGuildById(config.getGuild());
        if (guild == null) {
            System.out.println("Guild is null");
            System.exit(0);
            return;
        }
        if (config.getServerName().isEmpty()) {
            System.out.println("Server Name is invalid");
            System.exit(0);
            return;
        }
        clanCategory = guild.getCategoryById(config.getClanCategory());
        if(clanCategory == null) {
            System.out.println("Clan Category is null");
            System.exit(0);
            return;
        }
        adminLogChannel = guild.getTextChannelById(config.getAdminLogChannel());
        if(adminLogChannel == null) {
            System.out.println("Admin Log Channel is null");
            System.exit(0);
            return;
        }
        leaderRole = guild.getRoleById(config.getLeaderRole());
        if(leaderRole == null) {
            System.out.println("Leader Role is null");
            System.exit(0);
            return;
        }
        coLeaderRole = guild.getRoleById(config.getCoLeaderRole());
        if(coLeaderRole == null) {
            System.out.println("CoLeader Role is null");
            System.exit(0);
            return;
        }
        String status = config.getStatus();
        if(status.isEmpty()) {
            System.out.println("Status is invalid");
            System.exit(0);
            return;
        }
        String prefix = config.getPrefix();
        if(prefix.isEmpty()) {
            System.out.println("Prefix is invalid");
            System.exit(0);
            return;
        }
        String embedColor = config.getEmbedColor();
        embedRGBColor = RGB.fromHex(config.getEmbedColor());
        if(embedColor == null) {
            System.out.println("Embed Color is invalid");
            System.exit(0);
            return;
        }
        jda.getPresence().setActivity(Activity.playing(status));
        mongoHandler =
                new MongoHandler(
                        new MongoCredentials(
                                config.isMongo_legacy(),
                                config.getMongo_host(),
                                config.getMongo_port(),
                                config.getMongo_database(),
                                config.isMongo_auth(),
                                config.getMongo_username(),
                                config.getMongo_password()
                        )
                );

        guild.upsertCommand("clan", "Manages all clan related options")
            .addSubcommands(
                    new SubcommandData("create", "Creates a new clan")
                            .addOption(OptionType.STRING, "name", "Name of the clan", true)
            )
            .addSubcommands(
                    new SubcommandData("disband", "Disbands the clan you are currently in")
            )
            .addSubcommands(
                    new SubcommandData("info", "Shows information about a selected clan")
                            .addOption(OptionType.STRING, "name", "Name of the clan", false)
            )
            .addSubcommands(
                    new SubcommandData("changeleader", "Change your clans leader")
                            .addOption(OptionType.MENTIONABLE, "newleader", "New leader", true)
            )
            .addSubcommands(
                    new SubcommandData("promote", "Promote a member to coleader")
                            .addOption(OptionType.MENTIONABLE, "member", "The Member", true)
            )
            .addSubcommands(
                    new SubcommandData("demote", "Demote a member to member")
                            .addOption(OptionType.MENTIONABLE, "member", "The Member", true)
            )
            .addSubcommands(
                    new SubcommandData("invite", "Invite a member to your clan")
                            .addOption(OptionType.MENTIONABLE, "member", "The Member", true)
            )
            .addSubcommands(
                    new SubcommandData("kick", "Kick a member from your clan")
                            .addOption(OptionType.MENTIONABLE, "member", "The Member", true)
            )
            .addSubcommands(
                    new SubcommandData("setlogo", "Sets the logo of your clan")
                            .addOption(OptionType.STRING, "logo", "Logo URL", true)
            )
            .addSubcommands(
                    new SubcommandData("setcolor", "Sets the color of your clan")
                            .addOption(OptionType.STRING, "color", "Hex Color Code", true)
            )
            .addSubcommands(
                    new SubcommandData("setdiscord", "Sets the discord of your clan")
                            .addOption(OptionType.STRING, "discord", "Discord Invite URL", true)
            )
            .addSubcommands(
                    new SubcommandData("setyoutube", "Sets the youtube of your clan")
                            .addOption(OptionType.STRING, "youtube", "Youtube Channel URL", true)
            )
            .addSubcommands(
                    new SubcommandData("leave", "Leaves your current clan")
            )
            .queue();

        guild.upsertCommand("staff", "Manages all staff related options")
            .addSubcommands(
                    new SubcommandData("deleteclan", "Deletes a clan forcibly")
                            .addOption(OptionType.STRING, "name", "Clan Name", true)
            )
            .queue();

        guild.upsertCommand("wipelist", "Manages all of the wipe lists")
                .addSubcommands(
                        new SubcommandData("create", "Creates a wipe list")
                                .addOption(OptionType.STRING, "timestamp", "Timestamp for when the wipe is", true)
                )
                .addSubcommands(
                        new SubcommandData("delete", "Deletes a wipe list")
                                .addOption(OptionType.STRING, "uuid", "UUID of the wipe list", true)
                )
                .queue();

        jda.addEventListener(new MessageCommandListener());
        jda.addEventListener(new ClanCommands());
        jda.addEventListener(new StaffCommands());
        jda.addEventListener(new WipeListCommands());
        jda.addEventListener(new UserLeaveListener());
        jda.addEventListener(new WipelistButtonListener());
        jda.addEventListener(new ButtonClickListener());
        jda.addEventListener(new ClanRequestButtonListener());
    }

    public EmbedBuilder getHelpMessage() {
        return new EmbedBuilder()
                .setTitle(config.getServerName() + " Rust Server's Clan Management Commands")
                .setColor(this.embedRGBColor.getColorFromRGB())
                .setDescription("Please revert to the slash commands, to get started use `/`. After that you can click on our discord bot then see what commands there are." +
                        "\n\nUse `/clan create <clan tag>` - To make your first clan!\n\n" +
                        "**Leader**\n" +
                        "> **/clan promote** *<member>* - Promote a user to Co-Leader\n" +
                        "> **/clan demote** *<member>* - Demote a user from Co-Leader\n" +
                        "> **/clan disband** - Disband your clan\n" +
                        "> **/clan changeleader** *<new leader's id>* - Change your clan's leader\n\n" +
                        "**Co-Leader**\n" +
                        "> **/clan invite** *<member>* - Invites a user to your clan.\n" +
                        "> **/clan kick** *<member>* - Kick someone from your clan.\n" +
                        "> **/clan setlogo** *<image link>* - Set a clanlogo/avatar.\n" +
                        "> **/clan setcolor** *<hexcode>* - Change your clans role & embed's color.\n" +
                        "> **/clan setdiscord** *<discord invite>* - Add your clans discord.\n" +
                        "> **/clan setyoutube** *<youtube link>* - Add your youtubes discord.\n\n" +
                        "**Misc**\n" +
                        "> **/clan info** *<clan tag>* - Show information about a clan or your own.\n" +
                        "> **/clan leave** - Leave the clan you're currently in.");
    }

    private File getResourceAsFile(String resourcePath) {
        try {
            InputStream in = ClassLoader
                    .getSystemClassLoader()
                    .getResourceAsStream(resourcePath);
            if (in == null) {
                return null;
            }

            File tempFile = File.createTempFile(
                    String.valueOf(in.hashCode()),
                    ".tmp"
            );
            tempFile.deleteOnExit();

            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                //copy stream
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isValidHexCode(String string) {
        Pattern colorPattern = Pattern.compile("#([0-9a-f]{3}|[0-9a-f]{6}|[0-9a-f]{8})");
        Matcher m = colorPattern.matcher(string);
        return m.matches();
    }

    public static boolean isValidDiscordInvite(String url) {
        return url.startsWith("https://discord.gg/");
    }

    public static boolean isValidYoutube(String url) {
        return url.startsWith("https://www.youtube.com/");
    }

    public static boolean isValid(String url)
    {
        try {
            new URL(url).toURI();
            return true;
        }

        catch (Exception e) {
            return false;
        }
    }

    public static void fastInviteClan(SlashCommandEvent event, Member member, Clan clan) {
        member.getUser().openPrivateChannel()
                .flatMap(channel ->
                    stuff(clan, member, channel, event.getTextChannel())
                )
                .queue(null, new ErrorHandler()
                        .handle(ErrorResponse.CANNOT_SEND_TO_USER,
                                (ex) -> {
                                    event.replyEmbeds(
                                        new EmbedBuilder()
                                                .setDescription(member.getAsMention() + " has dms closed")
                                                .build())
                                            .queue();
                                }
                        ));
    }

    public static MessageAction stuff(Clan clan, Member member, PrivateChannel channel, TextChannel textChannel) {
        clan.invite(member);
        return textChannel.sendMessageEmbeds(
                new EmbedBuilder()
                        .setDescription("You have invited " + member.getAsMention() + " to `" + clan.getName() + "`")
                        .build()
        );
    }

    @Data
    public static class RGB {

        private final int red;
        private final int green;
        private final int blue;

        public RGB(int red, int green, int blue) {
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        public Color getColorFromRGB() {
            return new Color(this.red, this.blue, this.green);
        }

        public static RGB fromHex(String hex) {
            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }

            if (hex.length() == 3) {
                hex = hex.substring(0, 1) + hex.substring(0, 1) +
                        hex.substring(1, 2) + hex.substring(1, 2) +
                        hex.substring(2, 3) + hex.substring(2, 3);
            }

            return new RGB(
                    Integer.valueOf(hex.substring(0, 2), 16),
                    Integer.valueOf(hex.substring(2, 4), 16),
                    Integer.valueOf(hex.substring(4, 6), 16)
            );
        }

    }

}
