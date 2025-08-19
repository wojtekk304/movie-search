package com.movie;

import static spark.Spark.*;

import java.io.*;
import java.sql.*;
import java.util.*;

import com.google.gson.Gson;

public class App {
    public static void main(String[] args) {
        String jdbcUrl = "jdbc:h2:./movies";
        String user = "sa";
        String pass = "";

        port(8080);

        loadCsv(jdbcUrl, user, pass);

        before((req, res) -> res.header("Access-Control-Allow-Origin", "*"));

        // Serve frontend
        get("/", (req, res) -> {
            res.type("text/html");
            return new String(java.nio.file.Files.readAllBytes(
                    new File("src/main/resources/public/index.html").toPath()));
        });

        // API: /movies or /movies?genre=comedy
        get("/movies", (req, res) -> {
            String genre = req.queryParams("genre");
            List<Map<String, Object>> movies = new ArrayList<>();

            try (Connection conn = DriverManager.getConnection(jdbcUrl, user, pass)) {
                PreparedStatement stmt;

                if (genre != null && !genre.trim().isEmpty()) {
                    stmt = conn.prepareStatement("SELECT * FROM movies WHERE LOWER(\"Genre\") = LOWER(?)");
                    stmt.setString(1, genre.trim().toLowerCase());
                } else {
                    stmt = conn.prepareStatement("SELECT * FROM movies");
                }

                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    Map<String, Object> movie = new LinkedHashMap<>();
                    movie.put("Title", rs.getString("Film"));
                    movie.put("Genre", rs.getString("Genre"));
                    movie.put("Studio", rs.getString("Lead_Studio"));
                    movie.put("Audience %", rs.getInt("Audience_Score_pc"));
                    movie.put("Profitability", rs.getDouble("Profitability"));
                    movie.put("Rotten Tomatoes %", rs.getInt("Rotten_Tomatoes_pc"));
                    movie.put("Gross (M)", rs.getDouble("Worldwide_Gross"));
                    movie.put("Year", rs.getInt("Year"));

                    movies.add(movie);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            res.type("application/json");
            return new Gson().toJson(movies);
        });
    }

    private static void loadCsv(String url, String user, String pass) {
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement()) {

            stmt.execute("DROP TABLE IF EXISTS movies");

            stmt.execute("CREATE TABLE IF NOT EXISTS movies (" +
                    "\"Film\" VARCHAR(255), " +
                    "\"Genre\" VARCHAR(100), " +
                    "\"Lead_Studio\" VARCHAR(100), " +
                    "\"Audience_Score_pc\" INT, " +
                    "\"Profitability\" DOUBLE, " +
                    "\"Rotten_Tomatoes_pc\" INT, " +
                    "\"Worldwide_Gross\" DOUBLE, " +
                    "\"Year\" INT)");

            BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/films.csv"));
            String line;
            boolean skip = true;

            while ((line = reader.readLine()) != null) {
                if (skip) {
                    skip = false;
                    continue;
                }

                String[] parts = line.split(",", -1);
                if (parts.length < 8) continue;

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO movies (\"Film\", \"Genre\", \"Lead_Studio\", \"Audience_Score_pc\", \"Profitability\", \"Rotten_Tomatoes_pc\", \"Worldwide_Gross\", \"Year\") " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {

                    ps.setString(1, parts[0].trim());
                    ps.setString(2, parts[1].trim());
                    ps.setString(3, parts[2].trim());
                    ps.setInt(4, Integer.parseInt(parts[3].trim()));
                    ps.setDouble(5, Double.parseDouble(parts[4].trim()));
                    ps.setInt(6, Integer.parseInt(parts[5].trim()));
                    ps.setDouble(7, Double.parseDouble(parts[6].trim()));
                    ps.setInt(8, Integer.parseInt(parts[7].trim()));
                    ps.executeUpdate();

                } catch (Exception e) {
                    System.err.println("❌ Error inserting row: " + Arrays.toString(parts));
                }
            }

            System.out.println("✅ CSV loaded successfully");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
