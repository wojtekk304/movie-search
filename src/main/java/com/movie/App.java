package com.movie;

import static spark.Spark.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.*;
import java.util.*;

import com.google.gson.Gson;

public class App {
    public static void main(String[] args) {
        port(getHerokuAssignedPort());
        enableCORS();

        try {
            Connection conn = DriverManager.getConnection("jdbc:h2:mem:movies;DB_CLOSE_DELAY=-1");
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE movies (Film VARCHAR, Genre VARCHAR, Lead_Studio VARCHAR, Audience_Score INT, Profitability DOUBLE, Rotten_Tomatoes INT, Worldwide_Gross VARCHAR, Year INT)");

            BufferedReader reader = new BufferedReader(new FileReader("films.csv"));
            reader.readLine(); // skip header

            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                if (values.length < 8) continue;
                PreparedStatement ps = conn.prepareStatement("INSERT INTO movies VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
                for (int i = 0; i < 8; i++) ps.setString(i + 1, values[i].trim().replace("\"", ""));
                ps.executeUpdate();
            }

            get("/api/movies", (req, res) -> {
                String genre = req.queryParams("genre");
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM movies WHERE Genre = ?");
                ps.setString(1, genre);
                ResultSet rs = ps.executeQuery();
                List<Map<String, String>> results = new ArrayList<>();

                while (rs.next()) {
                    Map<String, String> movie = new HashMap<>();
                    movie.put("Film", rs.getString("Film"));
                    movie.put("Genre", rs.getString("Genre"));
                    movie.put("Lead_Studio", rs.getString("Lead_Studio"));
                    movie.put("Audience_Score", rs.getString("Audience_Score"));
                    movie.put("Profitability", rs.getString("Profitability"));
                    movie.put("Rotten_Tomatoes", rs.getString("Rotten_Tomatoes"));
                    movie.put("Worldwide_Gross", rs.getString("Worldwide_Gross"));
                    movie.put("Year", rs.getString("Year"));
                    results.add(movie);
                }

                res.type("application/json");
                return new Gson().toJson(results);
            });

            System.out.println("App running at /api/movies?genre=Comedy");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static int getHerokuAssignedPort() {
        String port = System.getenv("PORT");
        return port != null ? Integer.parseInt(port) : 4567;
    }

    static void enableCORS() {
        before((req, res) -> res.header("Access-Control-Allow-Origin", "*"));
    }
}
