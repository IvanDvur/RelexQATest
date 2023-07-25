package pojo;

public class LoginCredentials {
    private String username;
    private String password;
    private String identity;
    private String secret;

    public LoginCredentials(String username, String password, String identity, String secret) {
        this.username = username;
        this.password = password;
        this.identity = identity;
        this.secret = secret;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
