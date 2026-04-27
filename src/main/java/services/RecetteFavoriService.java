package services;

import models.RecetteNutritionnelle;
import utils.DbConnection;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class RecetteFavoriService {

    private final Connection cnx;

    public RecetteFavoriService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    public void toggleFavorite(String userId, String recipeId) {
        try {
            String check = """
                SELECT 1 FROM recipe_favorites
                WHERE user_id = ? AND recette_nutritionnelle_id = ?
            """;

            PreparedStatement ps = cnx.prepareStatement(check);
            ps.setBytes(1, uuidToBytes(userId));
            ps.setBytes(2, uuidToBytes(recipeId));

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String delete = """
                    DELETE FROM recipe_favorites
                    WHERE user_id = ? AND recette_nutritionnelle_id = ?
                """;

                PreparedStatement psDel = cnx.prepareStatement(delete);
                psDel.setBytes(1, uuidToBytes(userId));
                psDel.setBytes(2, uuidToBytes(recipeId));
                psDel.executeUpdate();

            } else {
                String insert = """
                    INSERT INTO recipe_favorites(user_id, recette_nutritionnelle_id)
                    VALUES (?, ?)
                """;

                PreparedStatement psIns = cnx.prepareStatement(insert);
                psIns.setBytes(1, uuidToBytes(userId));
                psIns.setBytes(2, uuidToBytes(recipeId));
                psIns.executeUpdate();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isFavorite(String userId, String recipeId) {
        try {
            String sql = """
                SELECT 1 FROM recipe_favorites
                WHERE user_id = ? AND recette_nutritionnelle_id = ?
            """;

            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setBytes(1, uuidToBytes(userId));
            ps.setBytes(2, uuidToBytes(recipeId));

            ResultSet rs = ps.executeQuery();
            return rs.next();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Map<RecetteNutritionnelle, Integer> getTop5FavoriteRecipes() {
        Map<RecetteNutritionnelle, Integer> map = new LinkedHashMap<>();

        String sql = """
            SELECT 
                r.id,
                r.title,
                r.image,
                r.kcal,
                COUNT(f.user_id) AS fav_count
            FROM recipe_favorites f
            INNER JOIN recette_nutritionnelle r
                ON r.id = f.recette_nutritionnelle_id
            GROUP BY r.id, r.title, r.image, r.kcal
            ORDER BY fav_count DESC
            LIMIT 5
        """;

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                RecetteNutritionnelle r = new RecetteNutritionnelle();

                r.setId(bytesToUUID(rs.getBytes("id")).toString());
                r.setTitle(rs.getString("title"));
                r.setImage(rs.getString("image"));
                r.setKcal(rs.getInt("kcal"));

                map.put(r, rs.getInt("fav_count"));
            }

            System.out.println("TOP RECIPES SIZE = " + map.size());

        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }

    public Map<String, Integer> getObjectiveDistribution() {
        Map<String, Integer> map = new LinkedHashMap<>();

        map.put("Weight Loss", 0);
        map.put("Muscle Gain", 0);
        map.put("Maintenance", 0);
        map.put("General Health", 0);

        String sql = """
        SELECT os.name, COUNT(DISTINCT au.id) AS total
        FROM app_user au
        INNER JOIN profile_physique pp 
            ON pp.user_id = au.id
        INNER JOIN objectif_sportif os 
            ON os.profile_physique_id = pp.id
        WHERE au.roles LIKE '%ROLE_USER%'
        GROUP BY os.name
    """;

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String name = rs.getString("name");
                int total = rs.getInt("total");

                map.put(name, total);
            }

            System.out.println("OBJECTIVES DISTRIBUTION = " + map);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }
    private byte[] uuidToBytes(String uuidStr) {
        UUID uuid = UUID.fromString(uuidStr);
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    private UUID bytesToUUID(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long high = bb.getLong();
        long low = bb.getLong();
        return new UUID(high, low);
    }
}