package dev.swiss.clanbot.clan;

import lombok.*;
import net.dv8tion.jda.api.entities.Member;

import java.util.*;

/**
 * @author Swiss (swiss@swissdev.com)
 */

@Data
@AllArgsConstructor
public class Invite {

    private Member member;
    private long expiredTime;
    private long messageID;

}
