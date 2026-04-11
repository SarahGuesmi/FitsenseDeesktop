package models;

import java.time.LocalDateTime;
import java.util.UUID;

public class User {
    private UUID id;
    private String email;
    private String password;
    private String rolesJson;
    private String firstname;
    private String lastname;
    private String accountStatus;
    private LocalDateTime dateCreation;
    private String googleAuthenticatorSecret;
    private String phoneNumber;
    private String photo;
    private String username;
    /** From {@code app_user.objective} / {@code objectif} when present (Symfony migration). */
    private String accountObjective;
    /** From {@code app_user.gender} / {@code genre} / {@code sexe} when physique row has no gender. */
    private String accountGender;

    public User() {
    }

    public User(UUID id, String email, String password, String rolesJson, String firstname, String lastname,
                String accountStatus, LocalDateTime dateCreation, String googleAuthenticatorSecret,
                String phoneNumber, String photo, String username) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.rolesJson = rolesJson;
        this.firstname = firstname;
        this.lastname = lastname;
        this.accountStatus = accountStatus;
        this.dateCreation = dateCreation;
        this.googleAuthenticatorSecret = googleAuthenticatorSecret;
        this.phoneNumber = phoneNumber;
        this.photo = photo;
        this.username = username;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRolesJson() {
        return rolesJson;
    }

    public void setRolesJson(String rolesJson) {
        this.rolesJson = rolesJson;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getGoogleAuthenticatorSecret() {
        return googleAuthenticatorSecret;
    }

    public void setGoogleAuthenticatorSecret(String googleAuthenticatorSecret) {
        this.googleAuthenticatorSecret = googleAuthenticatorSecret;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAccountObjective() {
        return accountObjective;
    }

    public void setAccountObjective(String accountObjective) {
        this.accountObjective = accountObjective;
    }

    public String getAccountGender() {
        return accountGender;
    }

    public void setAccountGender(String accountGender) {
        this.accountGender = accountGender;
    }

    @Override
    public String toString() {
        return "User{" + "id=" + id + ", email='" + email + '\'' + ", firstname='" + firstname + '\''
                + ", lastname='" + lastname + '\'' + ", accountStatus='" + accountStatus + '\'' + ", username='"
                + username + '\'' + '}';
    }
}