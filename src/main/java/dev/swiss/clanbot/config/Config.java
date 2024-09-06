package dev.swiss.clanbot.config;

import lombok.*;

/**
 * @author Swiss (swiss@swissdev.com)
 */
@Getter
public class Config {

  private String token;
  private String guild;
  private String serverName;

  private long clanCategory;

  private long adminLogChannel;

  private String embedColor;

  private long leaderRole;
  private long coLeaderRole;

  private boolean mongo_legacy;
  private String mongo_host;
  private int mongo_port;
  private String mongo_database;
  private boolean mongo_auth;
  private String mongo_username;
  private String mongo_password;

  private String status;

  private String prefix;

}
