package services;

import models.RecetteNutritionnelle;
import utils.DbConnection;
import java.util.UUID;
import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class RecetteNutritionnelleService implements CRUD<RecetteNutritionnelle> {

    public Connection cnx;

    public RecetteNutritionnelleService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    private static RecetteNutritionnelle mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime createdAt =
                ts != null ? ts.toInstant().atZone(ZoneOffset.UTC).toLocalDateTime() : null;

        String objectifsJson = rs.getString("objectifs");

        return new RecetteNutritionnelle(
                bytesToUUID(rs.getBytes("id")),
                rs.getString("title"),
                rs.getString("description"),
                (Integer) rs.getObject("kcal"),
                (Integer) rs.getObject("proteins"),
                rs.getString("type_meal"),
                rs.getString("ingredients"),
                rs.getString("preparation"),
                rs.getString("image"),
                createdAt,
                bytesToUUID(rs.getBytes("coach_id")),
                parseObjectifs(objectifsJson)
        );
    }

    private static String bytesToUUID(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            return null;
        }

        long msb = 0;
        long lsb = 0;

        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (bytes[i] & 0xff);
        }

        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (bytes[i] & 0xff);
        }

        return new UUID(msb, lsb).toString();
    }

    private static byte[] uuidToBytes(String uuid) {
        UUID u = UUID.fromString(uuid);

        byte[] bytes = new byte[16];

        long msb = u.getMostSignificantBits();
        long lsb = u.getLeastSignificantBits();

        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (msb >>> (8 * (7 - i)));
        }

        for (int i = 8; i < 16; i++) {
            bytes[i] = (byte) (lsb >>> (8 * (7 - i)));
        }

        return bytes;
    }

    private static List<String> parseObjectifs(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }

        json = json.trim();
        if (json.startsWith("[")) {
            json = json.substring(1);
        }
        if (json.endsWith("]")) {
            json = json.substring(0, json.length() - 1);
        }

        if (json.isBlank()) {
            return new ArrayList<>();
        }

        String[] parts = json.split(",");
        List<String> objectifs = new ArrayList<>();

        for (String part : parts) {
            objectifs.add(part.replace("\"", "").trim());
        }

        return objectifs;
    }

    private static String toJsonArray(List<String> objectifs) {
        if (objectifs == null || objectifs.isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < objectifs.size(); i++) {
            sb.append("\"").append(objectifs.get(i)).append("\"");
            if (i < objectifs.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static boolean isValidUuid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public void create(RecetteNutritionnelle recette) throws SQLException {
        createPrepared(recette);
    }

    @Override
    public void createPrepared(RecetteNutritionnelle recette) throws SQLException {
        if (recette.getCreatedAt() == null) {
            recette.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        }

        if (!isValidUuid(recette.getCoachId())) {
            throw new SQLException("coachId invalide ou vide. UUID attendu.");
        }

        String generatedId = UUID.randomUUID().toString();

        String sql = "INSERT INTO recette_nutritionnelle " +
                "(id, title, description, kcal, proteins, type_meal, ingredients, preparation, image, created_at, coach_id, objectifs) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            int i = 1;

            stmt.setBytes(i++, uuidToBytes(generatedId));
            stmt.setString(i++, recette.getTitle());
            stmt.setString(i++, recette.getDescription());

            if (recette.getKcal() != null) stmt.setInt(i++, recette.getKcal());
            else stmt.setNull(i++, Types.INTEGER);

            if (recette.getProteins() != null) stmt.setInt(i++, recette.getProteins());
            else stmt.setNull(i++, Types.INTEGER);

            stmt.setString(i++, recette.getTypeMeal());
            stmt.setString(i++, recette.getIngredients());
            stmt.setString(i++, recette.getPreparation());
            stmt.setString(i++, recette.getImage());
            stmt.setTimestamp(i++, Timestamp.valueOf(recette.getCreatedAt()));

            stmt.setBytes(i++, uuidToBytes(recette.getCoachId()));
            stmt.setString(i, toJsonArray(recette.getObjectifs()));

            stmt.executeUpdate();
            recette.setId(generatedId);
        }
    }
    public List<RecetteNutritionnelle> getByUserObjectifs(List<String> userObjectifs) throws SQLException {
        List<RecetteNutritionnelle> allRecipes = read();
        List<RecetteNutritionnelle> filtered = new ArrayList<>();

        if (userObjectifs == null || userObjectifs.isEmpty()) {
            return allRecipes;
        }

        for (RecetteNutritionnelle recette : allRecipes) {
            if (recette.getObjectifs() == null) continue;

            for (String obj : userObjectifs) {
                if (recette.getObjectifs().contains(obj)) {
                    filtered.add(recette);
                    break;
                }
            }
        }

        return filtered;
    }
    public void markAsDone(String userId, RecetteNutritionnelle recette) {
        try {
            String sql = """
        INSERT INTO recette_consommee
        (id, user_id, recette_id, date_consommation, kcal, proteins)
        VALUES (?, ?, ?, NOW(), ?, ?)
        """;

            PreparedStatement ps = cnx.prepareStatement(sql);

            ps.setBytes(1, uuidToBytes(UUID.randomUUID().toString()));
            ps.setBytes(2, uuidToBytes(userId));
            ps.setBytes(3, uuidToBytes(recette.getId().toString()));
            ps.setInt(4, recette.getKcal() == null ? 0 : recette.getKcal());
            ps.setInt(5, recette.getProteins() == null ? 0 : recette.getProteins());

            ps.executeUpdate();

            System.out.println("✔ Recipe saved as consumed");

            // 🔥 AJOUT IMPORTANT
            NutritionService nutritionService = new NutritionService();
            nutritionService.addCalories(userId, recette.getKcal() == null ? 0 : recette.getKcal());

            System.out.println("🔥 Calories updated in daily_nutrition");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void addCustomCalories(String userId, String foodName, int calories) throws SQLException {
        NutritionService nutritionService = new NutritionService();
        nutritionService.addCalories(userId, calories);

        System.out.println("🔥 Custom food calories added to daily_nutrition: " + calories);
    }    public List<RecetteNutritionnelle> getAll() throws SQLException {

        List<RecetteNutritionnelle> list = new ArrayList<>();

        String sql = "SELECT * FROM recette_nutritionnelle";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            RecetteNutritionnelle r = new RecetteNutritionnelle();

            r.setId(bytesToUUID(rs.getBytes("id")));
            r.setTitle(rs.getString("title"));
            r.setDescription(rs.getString("description"));
            r.setKcal(rs.getInt("kcal"));
            r.setProteins(rs.getInt("proteins"));
            r.setImage(rs.getString("image"));

            list.add(r);
        }

        return list;
    }

    @Override
    public List<RecetteNutritionnelle> read() throws SQLException {
        String sql = "SELECT * FROM recette_nutritionnelle";

        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            List<RecetteNutritionnelle> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        }
    }

    @Override
    public void update(RecetteNutritionnelle recette) throws SQLException {
        String sql = "UPDATE recette_nutritionnelle SET " +
                "title = ?, description = ?, kcal = ?, proteins = ?, type_meal = ?, " +
                "ingredients = ?, preparation = ?, image = ?, objectifs = ?, coach_id = ? " +
                "WHERE id = ?";

        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            int i = 1;

            stmt.setString(i++, recette.getTitle());
            stmt.setString(i++, recette.getDescription());

            if (recette.getKcal() != null) {
                stmt.setInt(i++, recette.getKcal());
            } else {
                stmt.setNull(i++, Types.INTEGER);
            }

            if (recette.getProteins() != null) {
                stmt.setInt(i++, recette.getProteins());
            } else {
                stmt.setNull(i++, Types.INTEGER);
            }

            stmt.setString(i++, recette.getTypeMeal());
            stmt.setString(i++, recette.getIngredients());
            stmt.setString(i++, recette.getPreparation());
            stmt.setString(i++, recette.getImage());
            stmt.setString(i++, toJsonArray(recette.getObjectifs()));

            if (isValidUuid(recette.getCoachId())) {
                stmt.setBytes(i++, uuidToBytes(recette.getCoachId()));
            } else {
                stmt.setNull(i++, Types.BINARY);
            }

            if (isValidUuid(recette.getId())) {
                stmt.setBytes(i, uuidToBytes(recette.getId()));
            } else {
                throw new SQLException("id recette invalide ou vide. UUID attendu.");
            }

            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(RecetteNutritionnelle recette) throws SQLException {
        String sql = "DELETE FROM recette_nutritionnelle WHERE id = ?";

        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            if (isValidUuid(recette.getId())) {
                stmt.setBytes(1, uuidToBytes(recette.getId()));
            } else {
                throw new SQLException("id recette invalide ou vide. UUID attendu.");
            }

            stmt.executeUpdate();
        }
    }

}