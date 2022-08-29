package dev.swiss.clanbot.mongodb;

import lombok.*;

/**
 * @author Swiss (swiss@swissdev.com)
 */

@Data
@AllArgsConstructor
public class MongoCredentials {

  private boolean legacy;

  private String hostname;
  private int port;
  private String database;

  private boolean auth;

  private String username;
  private String password;
}
