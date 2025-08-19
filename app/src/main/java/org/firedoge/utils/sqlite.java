package org.firedoge.utils;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.sqlite.SQLiteConfig;

public class sqlite {
    private Connection connection;

    public void connect() throws SQLException {

        File dbFile = new File("plugins/BuildingScore/data.db");
        if (!dbFile.exists()) {
            try {
                dbFile.createNewFile();
            } catch (IOException e) {
                throw new SQLException("Failed to create database file", e);
            }
        }
        SQLiteConfig config = new SQLiteConfig();
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);

        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath(), config.toProperties());
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS chunk_scores (" +
                    "chunkKey TEXT NOT NULL," +
                    "score DOUBLE NOT NULL," +
                    "PRIMARY KEY (chunkKey))");
        }
    }

    public double getScore(String chunkKey) throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect(); // Ensure the connection is established
        }
        String sql = "SELECT score FROM chunk_scores WHERE chunkKey = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, chunkKey);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble("score");
            } else {
                return 0.0; // Default score if not found
            }
        }
    }

    public void updateScore(String chunkKey, double score) throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect(); // Ensure the connection is established
        }
        String sql = "INSERT INTO chunk_scores (chunkKey, score) VALUES (?, ?) " +
                "ON CONFLICT(chunkKey) DO UPDATE SET score = CASE WHEN score + excluded.score < 0 THEN 0 ELSE score + excluded.score END";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, chunkKey);
            ps.setDouble(2, score);
            ps.executeUpdate();
        }
    }
    public boolean recordExists(String chunkKey) throws SQLException {
    if (connection == null || connection.isClosed()) {
        connect(); // Ensure the connection is established
    }
    String sql = "SELECT 1 FROM chunk_scores WHERE chunkKey = ? LIMIT 1";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
        ps.setString(1, chunkKey);
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next(); // If a record exists, rs.next() will return true
        }
    }
}

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
