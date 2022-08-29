package dev.swiss.clanbot.mongodb;

import com.mongodb.MongoClient;
import com.mongodb.*;
import com.mongodb.client.*;
import dev.swiss.clanbot.clan.*;
import lombok.*;
import org.bson.*;

import java.util.*;

public class MongoHandler {

  private com.mongodb.client.MongoClient mongoClient;
  private MongoClient client;
  private MongoDatabase mongoDatabase;

  @Getter
  private static MongoCollection<Document> clans;

  public MongoHandler(MongoCredentials mongoCredentials) {
    if (mongoCredentials.isAuth()) client =
      new MongoClient(
        new ServerAddress(
          mongoCredentials.getHostname(),
          mongoCredentials.getPort()
        ),
        Collections.singletonList(
          MongoCredential.createCredential(
            mongoCredentials.getUsername(),
            mongoCredentials.getDatabase(),
            mongoCredentials.getPassword().toCharArray()
          )
        )
      ); else if (!mongoCredentials.isLegacy()) client =
      new MongoClient(
        new ServerAddress(
          mongoCredentials.getHostname(),
          mongoCredentials.getPort()
        )
      ); else mongoClient = MongoClients.create(mongoCredentials.getHostname());

    if (!mongoCredentials.isLegacy()) mongoDatabase =
      client.getDatabase(mongoCredentials.getDatabase()); else mongoDatabase =
      mongoClient.getDatabase(mongoCredentials.getDatabase());

    clans = mongoDatabase.getCollection("clans");

    Clan.loadClans();
  }
}
