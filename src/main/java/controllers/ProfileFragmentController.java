package controllers;

import app.AppSession;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import models.ObjectifSportif;
import models.ProfilePhysique;
import models.User;
import services.ObjectifSportifService;
import services.ProfilePhysiqueService;
import services.UserService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Account fields from {@code app_user}; physique (height, weight, gender) from {@code profile_physique};
 * objective from {@code objectif_sportif} linked to that profile.
 */
public class ProfileFragmentController {

    @FXML
    private Label profileAvatar;
    @FXML
    private Label profileDisplayName;
    @FXML
    private TextField emailField;
    @FXML
    private TextField firstnameField;
    @FXML
    private TextField lastnameField;
    @FXML
    private ComboBox<String> objectiveCombo;
    @FXML
    private TextField heightField;
    @FXML
    private TextField weightField;
    @FXML
    private ComboBox<String> genderCombo;
    @FXML
    private TextField phoneField;
    @FXML
    private Label profileStatusLabel;

    private final UserService userService = new UserService();
    private final ProfilePhysiqueService profilePhysiqueService = new ProfilePhysiqueService();
    private final ObjectifSportifService objectifSportifService = new ObjectifSportifService();
    private Runnable afterSaveCallback;

    public void setAfterSaveCallback(Runnable afterSaveCallback) {
        this.afterSaveCallback = afterSaveCallback;
    }

    @FXML
    private void initialize() {
        genderCombo.setItems(FXCollections.observableArrayList("Female", "Male"));
    }

    public void reloadFromSession() {
        clearFieldErrors();
        hideStatus();
        User u = AppSession.getCurrentUser();
        if (u == null) {
            return;
        }
        try {
            User fresh = userService.findByEmail(u.getEmail());
            if (fresh != null) {
                AppSession.setCurrentUser(fresh);
                u = fresh;
            }
        } catch (SQLException ignored) {
        }
        applyUserToForm(u);
    }

    private void applyUserToForm(User u) {
        profileDisplayName.setText(displayName(u));
        profileAvatar.setText(avatarGlyph(u));
        emailField.setText(safe(u.getEmail()));
        firstnameField.setText(safe(u.getFirstname()));
        lastnameField.setText(safe(u.getLastname()));
        phoneField.setText(safe(u.getPhoneNumber()));

        String emailTrim = safe(u.getEmail()).trim();
        String objectiveName = loadObjectiveLabelByUserEmail(emailTrim);
        if (objectiveName.isEmpty()) {
            objectiveName = loadPrimaryObjectiveNameForAppUser(u.getId());
        }

        List<ProfilePhysique> profiles = loadProfilesForUser(u.getId());
        if (profiles.isEmpty()) {
            heightField.clear();
            weightField.clear();
            if (objectiveName.isEmpty()) {
                String ou = u.getAccountObjective();
                objectiveName = ou != null ? ou.trim() : "";
            }
            applyGenderAndObjectiveToForm(u, emailTrim, null, objectiveName);
            return;
        }
        ProfilePhysique canonical = pickCanonicalProfile(profiles);
        Float height = firstNonNull(canonical.getHeight(), profiles, ProfilePhysique::getHeight);
        Float weight = firstNonNull(canonical.getWeight(), profiles, ProfilePhysique::getWeight);
        String genderDb = firstNonBlankGender(canonical, profiles);
        if (genderDb == null || genderDb.isBlank()) {
            String g = u.getAccountGender();
            if (g != null && !g.isBlank()) {
                genderDb = g;
            }
        }
        if (genderDb == null || genderDb.isBlank()) {
            genderDb = loadGenderByUserEmail(emailTrim);
        }
        if (height != null) {
            heightField.setText(formatNumber(height.doubleValue()));
        } else {
            heightField.clear();
        }
        if (weight != null) {
            weightField.setText(formatNumber(weight.doubleValue()));
        } else {
            weightField.clear();
        }
        if (objectiveName.isEmpty()) {
            objectiveName = resolveObjectiveNameForUser(profiles, canonical);
        }
        if (objectiveName.isEmpty()) {
            String ou = u.getAccountObjective();
            objectiveName = ou != null ? ou.trim() : "";
        }
        setGenderComboFromDb(genderDb);
        populateObjectiveCombo(objectiveName);
    }

    private void applyGenderAndObjectiveToForm(User u, String emailTrim, String genderDb, String objectiveName) {
        // Overloaded path for empty profiles: combos only; used from early return branch.
        String g = genderDb;
        if (g == null || g.isBlank()) {
            g = u.getAccountGender();
        }
        if (g == null || g.isBlank()) {
            g = loadGenderByUserEmail(emailTrim);
        }
        setGenderComboFromDb(g);
        String obj = objectiveName;
        if (obj.isEmpty()) {
            obj = loadObjectiveLabelByUserEmail(emailTrim);
        }
        if (obj.isEmpty()) {
            obj = loadPrimaryObjectiveNameForAppUser(u.getId());
        }
        if (obj.isEmpty()) {
            String ou = u.getAccountObjective();
            obj = ou != null ? ou.trim() : "";
        }
        populateObjectiveCombo(obj);
    }

    private String loadObjectiveLabelByUserEmail(String emailTrim) {
        try {
            return objectifSportifService.findPrimaryObjectiveLabelByUserEmail(emailTrim);
        } catch (SQLException e) {
            return "";
        }
    }

    private String loadGenderByUserEmail(String emailTrim) {
        try {
            return profilePhysiqueService.findGenderByUserEmail(emailTrim);
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * Preferred path: objectives from {@code objectif_sportif} for this login, via {@code profile_physique.user_id}.
     */
    private String loadPrimaryObjectiveNameForAppUser(UUID appUserId) {
        try {
            List<ObjectifSportif> objs = objectifSportifService.findAllByAppUserId(appUserId);
            if (objs.isEmpty()) {
                return "";
            }
            return objs.stream()
                    .filter(o -> o.getName() != null && !o.getName().isBlank())
                    .max(Comparator.comparing(ObjectifSportif::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(o -> safe(o.getName()).trim())
                    .orElse("");
        } catch (SQLException e) {
            return "";
        }
    }

    private List<ProfilePhysique> loadProfilesForUser(UUID userId) {
        try {
            List<ProfilePhysique> list = profilePhysiqueService.findByUserId(userId);
            return list == null ? List.of() : list;
        } catch (SQLException e) {
            return List.of();
        }
    }

    /**
     * UUID order is not creation time; users can have multiple physique rows (e.g. onboarding vs later saves).
     * Prefer the row that already owns objectives, then gender, then height/weight data.
     */
    private ProfilePhysique pickCanonicalProfile(List<ProfilePhysique> profiles) {
        if (profiles.size() == 1) {
            return profiles.get(0);
        }
        Comparator<ProfilePhysique> byRichness = Comparator
                .comparingInt((ProfilePhysique p) -> countObjectifsSafe(p.getId()))
                .thenComparingInt(ProfileFragmentController::physiqueRichnessScore)
                .thenComparing(ProfilePhysique::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        return profiles.stream().max(byRichness).orElse(profiles.get(0));
    }

    private int countObjectifsSafe(UUID profilePhysiqueId) {
        try {
            return objectifSportifService.findByProfilePhysiqueId(profilePhysiqueId).size();
        } catch (SQLException e) {
            return 0;
        }
    }

    private static int physiqueRichnessScore(ProfilePhysique p) {
        int s = 0;
        if (p.getGender() != null && !p.getGender().isBlank()) {
            s += 4;
        }
        if (p.getHeight() != null) {
            s += 2;
        }
        if (p.getWeight() != null) {
            s += 2;
        }
        return s;
    }

    private static Float firstNonNull(
            Float primary, List<ProfilePhysique> profiles, java.util.function.Function<ProfilePhysique, Float> getter) {
        if (primary != null) {
            return primary;
        }
        for (ProfilePhysique p : profiles) {
            Float v = getter.apply(p);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static String firstNonBlankGender(ProfilePhysique canonical, List<ProfilePhysique> profiles) {
        String g = canonical.getGender();
        if (g != null && !g.isBlank()) {
            return g;
        }
        for (ProfilePhysique p : profiles) {
            if (p.getGender() != null && !p.getGender().isBlank()) {
                return p.getGender();
            }
        }
        return null;
    }

    private String resolveObjectiveNameForUser(List<ProfilePhysique> profiles, ProfilePhysique canonical) {
        String fromCanonical = loadPrimaryObjectiveName(canonical.getId());
        if (!fromCanonical.isEmpty()) {
            return fromCanonical;
        }
        for (ProfilePhysique p : profiles) {
            if (p.getId().equals(canonical.getId())) {
                continue;
            }
            String o = loadPrimaryObjectiveName(p.getId());
            if (!o.isEmpty()) {
                return o;
            }
        }
        return "";
    }

    private String loadPrimaryObjectiveName(UUID profilePhysiqueId) {
        try {
            List<ObjectifSportif> objs = objectifSportifService.findByProfilePhysiqueId(profilePhysiqueId);
            if (objs.isEmpty()) {
                return "";
            }
            return objs.stream()
                    .max(Comparator.comparing(ObjectifSportif::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(o -> safe(o.getName()))
                    .orElse("");
        } catch (SQLException e) {
            return "";
        }
    }

    /**
     * Distinct objective names from {@code objectif_sportif} (same idea as {@link ObjectiveController}).
     */
    private List<String> loadObjectiveCatalogFromDatabase() {
        Set<String> unique = new LinkedHashSet<>();
        try {
            for (ObjectifSportif o : objectifSportifService.read()) {
                if (o.getName() != null && !o.getName().isBlank()) {
                    unique.add(o.getName().trim());
                }
            }
        } catch (SQLException ignored) {
        }
        if (unique.isEmpty()) {
            unique.add("Weight Loss");
            unique.add("Muscle Gain");
            unique.add("Endurance");
            unique.add("Well-being");
        }
        return new ArrayList<>(unique);
    }

    private void populateObjectiveCombo(String currentUserObjective) {
        List<String> items = new ArrayList<>(loadObjectiveCatalogFromDatabase());
        String current = safe(currentUserObjective).trim();
        if (!current.isEmpty()) {
            boolean found = items.stream().anyMatch(s -> s.equalsIgnoreCase(current));
            if (!found) {
                items.add(0, current);
            }
        }
        objectiveCombo.setItems(FXCollections.observableArrayList(items));
        if (current.isEmpty()) {
            objectiveCombo.getSelectionModel().clearSelection();
            objectiveCombo.setValue(null);
            return;
        }
        String selected = items.stream().filter(s -> s.equals(current)).findFirst()
                .orElseGet(() -> items.stream().filter(s -> s.equalsIgnoreCase(current)).findFirst().orElse(null));
        if (selected != null) {
            objectiveCombo.getSelectionModel().select(selected);
        } else {
            objectiveCombo.getSelectionModel().clearSelection();
            objectiveCombo.setValue(null);
        }
    }

    private void setGenderComboFromDb(String dbValue) {
        if (dbValue == null || dbValue.isBlank()) {
            genderCombo.getSelectionModel().clearSelection();
            genderCombo.setValue(null);
            return;
        }
        String n = dbValue.trim();
        if ("1".equals(n)) {
            genderCombo.getSelectionModel().select("Male");
            return;
        }
        if ("0".equals(n) || "2".equals(n)) {
            genderCombo.getSelectionModel().select("Female");
            return;
        }
        for (String item : genderCombo.getItems()) {
            if (item.equalsIgnoreCase(n)
                    || item.replace("-", "").equalsIgnoreCase(n.replace("-", "").replace("_", ""))) {
                genderCombo.getSelectionModel().select(item);
                return;
            }
        }
        if ("male".equalsIgnoreCase(n) || "m".equalsIgnoreCase(n) || "homme".equalsIgnoreCase(n)) {
            genderCombo.getSelectionModel().select("Male");
        } else if ("female".equalsIgnoreCase(n) || "f".equalsIgnoreCase(n) || "femme".equalsIgnoreCase(n)) {
            genderCombo.getSelectionModel().select("Female");
        } else {
            genderCombo.getSelectionModel().clearSelection();
            genderCombo.setValue(null);
        }
    }

    private static String genderUiToDb(String ui) {
        if (ui == null || ui.isBlank()) {
            return null;
        }
        if ("Male".equals(ui)) {
            return "male";
        }
        if ("Female".equals(ui)) {
            return "female";
        }
        return ui.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private static String displayName(User u) {
        String full = (safe(u.getFirstname()) + " " + safe(u.getLastname())).trim();
        if (!full.isEmpty()) {
            return full;
        }
        if (!safe(u.getUsername()).isEmpty()) {
            return u.getUsername();
        }
        return safe(u.getEmail()).isEmpty() ? "Profile" : u.getEmail();
    }

    private static String avatarGlyph(User u) {
        String f = safe(u.getFirstname()).trim();
        String l = safe(u.getLastname()).trim();
        if (!f.isEmpty() && !l.isEmpty()) {
            return ("" + Character.toUpperCase(f.charAt(0)) + Character.toUpperCase(l.charAt(0))).trim();
        }
        if (!f.isEmpty()) {
            return f.substring(0, Math.min(2, f.length())).toUpperCase();
        }
        String uName = safe(u.getUsername());
        if (uName.length() >= 2) {
            return uName.substring(0, 2).toUpperCase();
        }
        if (!uName.isEmpty()) {
            return uName.substring(0, 1).toUpperCase();
        }
        return "👤";
    }

    private static String formatNumber(double v) {
        if (v == (long) v) {
            return String.valueOf((long) v);
        }
        return String.valueOf(v);
    }

    @FXML
    private void onSaveProfile() {
        clearFieldErrors();
        hideStatus();
        User current = AppSession.getCurrentUser();
        if (current == null || current.getId() == null) {
            alert(Alert.AlertType.ERROR, "Session", "You are not signed in.");
            return;
        }

        String email = safe(emailField.getText()).trim();
        String first = safe(firstnameField.getText()).trim();
        String last = safe(lastnameField.getText()).trim();
        String objective = objectiveCombo.getValue() == null ? "" : safe(objectiveCombo.getValue()).trim();
        String phone = safe(phoneField.getText()).trim();
        String genderUi = genderCombo.getValue();

        boolean ok = true;
        if (first.isBlank()) {
            markInvalid(firstnameField);
            ok = false;
        }
        if (last.isBlank()) {
            markInvalid(lastnameField);
            ok = false;
        }
        if (!isValidEmail(email)) {
            markInvalid(emailField);
            ok = false;
        }
        Double heightCm = null;
        String heightText = safe(heightField.getText()).trim();
        if (!heightText.isEmpty()) {
            try {
                heightCm = Double.parseDouble(heightText.replace(',', '.'));
            } catch (NumberFormatException e) {
                markInvalid(heightField);
                ok = false;
            }
        }
        Double weightKg = null;
        String weightText = safe(weightField.getText()).trim();
        if (!weightText.isEmpty()) {
            try {
                weightKg = Double.parseDouble(weightText.replace(',', '.'));
            } catch (NumberFormatException e) {
                markInvalid(weightField);
                ok = false;
            }
        }
        if (heightCm != null && heightCm <= 0) {
            markInvalid(heightField);
            ok = false;
        }
        if (weightKg != null && weightKg <= 0) {
            markInvalid(weightField);
            ok = false;
        }
        if (!ok) {
            showStatus("Please fix the highlighted fields.", true);
            return;
        }

        try {
            User byEmail = userService.findByEmail(email);
            if (byEmail != null && !byEmail.getId().equals(current.getId())) {
                markInvalid(emailField);
                showStatus("This email is already in use.", true);
                return;
            }

            current.setEmail(email);
            current.setFirstname(first);
            current.setLastname(last);
            current.setPhoneNumber(phone.isEmpty() ? null : phone);

            userService.update(current);

            String genderDb = genderUiToDb(genderUi);
            syncPhysiqueAndObjective(current.getId(), heightCm, weightKg, genderDb, objective);

            User reloaded = userService.findByEmail(email);
            if (reloaded != null) {
                reloaded.setPassword(current.getPassword());
                AppSession.setCurrentUser(reloaded);
            } else {
                AppSession.setCurrentUser(current);
            }
            applyUserToForm(AppSession.getCurrentUser());
            if (afterSaveCallback != null) {
                afterSaveCallback.run();
            }
            alert(Alert.AlertType.INFORMATION, "Profile", "Your profile was saved.");
        } catch (SQLException e) {
            alert(Alert.AlertType.ERROR, "Profile", "Could not save: " + e.getMessage());
        }
    }

    /**
     * Persists {@code profile_physique} (height cm, weight kg, gender) and the primary {@code objectif_sportif}
     * row for this user's profile (name = chosen catalog value).
     */
    private void syncPhysiqueAndObjective(
            UUID userId,
            Double heightCm,
            Double weightKg,
            String genderDb,
            String objectiveText) throws SQLException {

        boolean hasPhysiqueData = heightCm != null || weightKg != null || genderDb != null;
        boolean hasObjective = !objectiveText.isEmpty();

        List<ProfilePhysique> existing = profilePhysiqueService.findByUserId(userId);
        ProfilePhysique profile;
        if (existing.isEmpty()) {
            if (!hasPhysiqueData && !hasObjective) {
                return;
            }
            profile = new ProfilePhysique();
            profile.setUserId(userId);
            profile.setHeight(heightCm == null ? null : heightCm.floatValue());
            profile.setWeight(weightKg == null ? null : weightKg.floatValue());
            profile.setGender(genderDb);
            profilePhysiqueService.createPrepared(profile);
        } else {
            profile = pickCanonicalProfile(existing);
            profile.setHeight(heightCm == null ? null : heightCm.floatValue());
            profile.setWeight(weightKg == null ? null : weightKg.floatValue());
            profile.setGender(genderDb);
            profilePhysiqueService.update(profile);
        }

        List<ObjectifSportif> objs = new ArrayList<>(objectifSportifService.findByProfilePhysiqueId(profile.getId()));
        if (objectiveText.isEmpty()) {
            for (ObjectifSportif o : objs) {
                objectifSportifService.delete(o);
            }
            return;
        }
        if (objs.isEmpty()) {
            ObjectifSportif o = new ObjectifSportif();
            o.setName(objectiveText);
            o.setProfilePhysiqueId(profile.getId());
            objectifSportifService.createPrepared(o);
            return;
        }
        objs.sort(Comparator.comparing(ObjectifSportif::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        ObjectifSportif primary = objs.get(objs.size() - 1);
        primary.setName(objectiveText);
        objectifSportifService.update(primary);
        for (int i = 0; i < objs.size() - 1; i++) {
            objectifSportifService.delete(objs.get(i));
        }
    }

    private void showStatus(String message, boolean error) {
        profileStatusLabel.setText(message);
        profileStatusLabel.setManaged(true);
        profileStatusLabel.setVisible(true);
        profileStatusLabel.setStyle(error ? "-fx-text-fill: #ff8a92;" : "-fx-text-fill: #8b96b0;");
    }

    private void hideStatus() {
        profileStatusLabel.setText("");
        profileStatusLabel.setManaged(false);
        profileStatusLabel.setVisible(false);
    }

    private void clearFieldErrors() {
        List.of(emailField, firstnameField, lastnameField, heightField, weightField, phoneField)
                .forEach(f -> f.getStyleClass().removeAll("profile-field-error"));
        objectiveCombo.getStyleClass().removeAll("profile-field-error");
        genderCombo.getStyleClass().removeAll("profile-field-error");
    }

    private static void markInvalid(TextField field) {
        if (!field.getStyleClass().contains("profile-field-error")) {
            field.getStyleClass().add("profile-field-error");
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private static void alert(Alert.AlertType type, String title, String message) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}
