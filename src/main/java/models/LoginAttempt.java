package models;

import java.time.LocalDateTime;
import java.util.UUID;

/** Maps to the existing login_attempt table. */
public class LoginAttempt {

    private UUID          id;
    private UUID          userId;
    private String        ipAddress;   // ip_address
    private String        status;      // SUCCESS or FAILURE
    private String        country;
    private String        city;
    private String        region;
    private String        isp;
    private LocalDateTime timestamp;   // timestamp column
    private String        emailEmail;  // email_email

    public LoginAttempt() {}

    public UUID          getId()                              { return id; }
    public void          setId(UUID id)                       { this.id = id; }

    public UUID          getUserId()                          { return userId; }
    public void          setUserId(UUID userId)               { this.userId = userId; }

    public String        getIpAddress()                       { return ipAddress; }
    public void          setIpAddress(String ipAddress)       { this.ipAddress = ipAddress; }

    public String        getStatus()                          { return status; }
    public void          setStatus(String status)             { this.status = status; }

    public String        getCountry()                         { return country; }
    public void          setCountry(String country)           { this.country = country; }

    public String        getCity()                            { return city; }
    public void          setCity(String city)                 { this.city = city; }

    public String        getRegion()                          { return region; }
    public void          setRegion(String region)             { this.region = region; }

    public String        getIsp()                             { return isp; }
    public void          setIsp(String isp)                   { this.isp = isp; }

    public LocalDateTime getTimestamp()                       { return timestamp; }
    public void          setTimestamp(LocalDateTime timestamp){ this.timestamp = timestamp; }

    public String        getEmailEmail()                      { return emailEmail; }
    public void          setEmailEmail(String emailEmail)     { this.emailEmail = emailEmail; }

    /** e.g. "Tunis, Tunis Governorate, TN" */
    public String locationString() {
        StringBuilder sb = new StringBuilder();
        if (city    != null && !city.isBlank())    sb.append(city);
        if (region  != null && !region.isBlank())  { if (sb.length() > 0) sb.append(", "); sb.append(region); }
        if (country != null && !country.isBlank()) { if (sb.length() > 0) sb.append(", "); sb.append(country); }
        return sb.length() > 0 ? sb.toString() : "Unknown location";
    }
}
