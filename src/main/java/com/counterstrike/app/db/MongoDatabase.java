package com.counterstrike.app.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;

public final class MongoDatabase {
    private final String uri;
    private final String dbName;
    private MongoClient client;

    public MongoDatabase(String uri, String dbName) {
        this.uri = uri;
        this.dbName = dbName;
    }

    public com.mongodb.client.MongoDatabase getDatabase() {
        if (client == null) {
            client = MongoClients.create(uri);
        }
        return client.getDatabase(dbName);
    }

    public void testConnection() {
        getDatabase().runCommand(new Document("ping", 1));
    }

    public void close() {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    public String getUri() { return uri; }
    public String getDbName() { return dbName; }
}
