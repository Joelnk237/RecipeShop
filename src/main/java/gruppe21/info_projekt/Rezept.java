package gruppe21.info_projekt;

import io.vertx.core.json.JsonObject;

public class Rezept {
  private int id;
  private int id_autor;
  private int kat_id;
  private String title;
  private String beschreibung;
  private String urlBild;

  public Rezept(int id, int id_autor, int kat_id, String title, String beschreibung, String urlBild) {
    this.id = id;
    this.id_autor = id_autor;
    this.kat_id = kat_id;
    this.title = title;
    this.beschreibung = beschreibung;
    this.urlBild = urlBild;
  }

  public static Rezept fromJSON(JsonObject json) {
    return new Rezept(
      json.getInteger("Rid"),
      json.getInteger("Nid"),
      json.getInteger("Kid"),
      json.getString("Title"),
      json.getString("Beschreibung"),
      json.getString("Rbild")
    );
  }

  public int getId() {
    return id;
  }

  public int getId_autor() {
    return id_autor;
  }

  public int getKat_id() {
    return kat_id;
  }

  public String getTitle() {
    return title;
  }

  public String getBeschreibung() {
    return beschreibung;
  }

  public String getUrlBild() {
    return urlBild;
  }


  public void setId(int id) {
    this.id = id;
  }

  public void setId_autor(int id_autor) {
    this.id_autor = id_autor;
  }

  public void setKat_id(int kat_id) {
    this.kat_id = kat_id;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setBeschreibung(String beschreibung) {
    this.beschreibung = beschreibung;
  }

  public void setUrlBild(String urlBild) {
    this.urlBild = urlBild;
  }
}
