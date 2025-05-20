package gruppe21.info_projekt;

import io.vertx.core.json.JsonObject;

public class User {
  private int id; // ID des Benutzers
  private String name; // Name des Benutzers
  private String email; // E-Mail Adresse
  private String urlBild; // path zum Bild
  private String password; // Password


  public User(int id, String name, String email, String urlBild, String password) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.urlBild = urlBild;
    this.password = password;
  }

  static User fromJSON(JsonObject json) {
    return new User(
      json.getInteger("Nid"),
      json.getString("Nname"),
      json.getString("Email"),
      json.getString("ProfilBild"),
      json.getString("Password")
    );
  }


  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getUrlBild() {
    return urlBild;
  }

  public void setUrlBild(String urlBild) {
    this.urlBild = urlBild;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
