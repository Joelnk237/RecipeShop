package gruppe21.info_projekt;

import io.vertx.core.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine;

import io.vertx.sqlclient.*;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


public class MainVerticle extends AbstractVerticle {

  public static class DatabaseConfig {
    public static Pool createMySQLPool(Vertx vertx) {
      // Options de connexion à MySQL
      SqlConnectOptions connectOptions = new SqlConnectOptions()
        .setHost("ip1-dbs.mni.thm.de")    // Remplace par ton adresse IP/hostname
        .setPort(3306)                     // Port MySQL
        .setDatabase("InfP-WS2425-21")      // Nom de la base de données
        .setUser("InfP-WS2425-21")          // Nom d'utilisateur
        .setPassword("jXfK[qHnqP9Xjo4Q");   // Mot de passe

      // Options du pool de connexions
      PoolOptions poolOptions = new PoolOptions()
        .setMaxSize(5); // Nombre max de connexions simultanées

      // Création du pool
      return Pool.pool(vertx, connectOptions, poolOptions);
    }
  }

  private HandlebarsTemplateEngine engine;
  private JDBCClient jdbcClient;
  Pool pool;
  String pathImages = "../../../../file-uploads/images/" ;
  private JsonObject config = new JsonObject()
    .put("url", "jdbc:mariadb://ip1-dbs.mni.thm.de:3306/InfP-WS2425-21")  // Datenbank
    .put("driver_class", "org.mariadb.jdbc.Driver")
    .put("user", "InfP-WS2425-21")                                // E-Mail
    .put("password", "jXfK[qHnqP9Xjo4Q")                        // E-Mail
    .put("max_pool_size", 5);


  // The following snippet is only necessary if you want to start the server directly using IntelliJ
  public static void main(final String[] args) {
    Launcher.executeCommand("run", MainVerticle.class.getName());
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    // Initialize template engine: Handlebars
    engine = HandlebarsTemplateEngine.create(vertx);

    // Setup Database driver
    //jdbcClient = JDBCClient.createShared(vertx, config);

    pool = DatabaseConfig.createMySQLPool(vertx);

    Router router = Router.router(vertx);
    // Use a local SessionHandler
    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));


    router.route("/pages/*").handler(StaticHandler.create("pages").setCachingEnabled(false));
    //router.route("/file-uploads/images/*").handler(StaticHandler.create("file-uploads/images"));
    router.route("/file-uploads/images/*").handler(StaticHandler.create("file-uploads/images").setCachingEnabled(false));

    //router.route().handler(BodyHandler.create().setUploadsDirectory("file-uploads/images/recipes"));


    // CORS_konfigurationen: unabhängig von dem Server wo die GET-Anfragen gestartet wurden
    router.route().handler(CorsHandler.create()
      .addOrigin("*")
      .allowedMethod(HttpMethod.GET)
      .allowedMethod(HttpMethod.DELETE)
      .allowedMethod(HttpMethod.PUT)
      .allowedMethod(HttpMethod.POST));


    // JWT Setup
    /*JWTAuthOptions config = new JWTAuthOptions().setKeyStore(new KeyStoreOptions().setPassword("SuperSecret"));
    jwtProvider = JWTAuth.create(vertx, config);
    JWTAuthHandler authHandler = JWTAuthHandler.create(jwtProvider);*/



    router.route().handler(BodyHandler.create());

    /**
     * @apiDefine UserNotFound
     *
     * @apiError UserNotFound The id of the User was not found.
     *
     * @apiErrorExample Error-Response:
     *     HTTP/1.1 404 Not Found
     *     {
     *       "error": "UserNotFound"
     *     }
     */

    /**
     * @apiDefine RecipeNotFound
     *
     * @apiError RecipeNotFound Rezept id wurde nicht gefunden
     *
     * @apiErrorExample Error-Response:
     *     HTTP/1.1 404 Not Found
     *     {
     *       "error": "RecipeNotFound"
     *     }
     */

    /**
     * @apiDefine CategoryNotFound
     *
     * @apiError CategoryNotFound Kategorie id wurde nicht gefunden
     *
     * @apiErrorExample Error-Response:
     *     HTTP/1.1 404 Not Found
     *     {
     *       "error": "CategoryNotFound"
     *     }
     */

    /**
     * @apiDefine UserNotAuthorized
     *
     * @apiError UserNotAuthorized der Benutzer hat keine Zugriffsrechte auf die Resource
     *
     * @apiErrorExample Error-Response:
     *     HTTP/1.1 403 Forbidden
     *     {
     *       "error": "Forbidden"
     *     }
     */




    /**
     * Endpunkt für Authentifizierung.
     *
     * @route GET /login
     * @param ctx Kontext der Anfrage.
     * @response 200 erfolgreiche Authentifizierung.
     * @response 401 falsche Zugangsdaten.
     */
    router.get("/")
      .handler(this::home);

    /**
     * Formular des Logins senden.
     *
     * @route POST /login
     * @param ctx Kontext der Anfrage.
     * @response 200 erfolgreiche Authentifizierung.
     * @response 401 falsche Zugangsdaten.
     */
    router.post("/login")
      //.handler(BodyHandler.create())
      .handler(this::login);

    router.get("/logout")
      .handler(this::abmelden);

    router.get("/home/rezepte")
      .handler(this::login);

    router.get("/rezepte/:id")
      .handler(this::showRezeptPage);

    router.get("/users/:Nid/rezepte/:Rid/request")
      .handler(this::showModifRecipePage);

    router.get("/categories/:id/rezepte")
      .handler(this::showRezepteByCatId);

    router.get("/FavoritList/users/:Uid/rezepte/:Rid")
      .handler(this::addInFavListe);

    router.get("/newRezept")
      .handler(this::login);

    router.get("/profil")
      .handler(this::login);

    router.get("/users")
      .handler(this::login);

    router.get("/users/:id")
      .handler(this::login);

    router.get("/users/:id/profil")
      .handler(this::chargeUserInfos);

    router.get("/users/:id/pwd")
      .handler(this::showModifPwdPage);

    router.put("/users/:id/profil")
      .handler(this::updateUser);

    router.post("/users/:id/pwd")
      .handler(this::changePwd);

    router.post("/users/:id/rezepte")
      .handler(this::addRezept);

    router.get("/users/:id/rezepte")
      .handler(this::showRecipeByUserId);

    router.get("/users/:id/rezepte/:Rid")
      .handler(this::login);

    router.delete("/users/:id/rezepte/:Rid")
      .handler(this::deleteRecipe);

    router.post("/users/:id/rezepte/:Rid")
      .handler(this::updateRecipe);

    router.get("/signup")
      .handler(this::createAccount);

    router.post("/signup/addAccount")
      .handler(this::addAccount);

    router.get("/users/:id/favorite")
      .handler(this::showFavListe);


    router.delete("/users/:id")
      .handler(this::deleteAccount);

    router.post("/rezepte/:Rid/comments/:Nid")
      .handler(this::addComments);




    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8888)
      .onComplete(http -> {
        if (http.succeeded()) {
          startPromise.complete();
          System.out.println("HTTP server started on port 8888");
        } else {
          startPromise.fail(http.cause());
        }
      });
  }

  private boolean identPruefen(RoutingContext ctx, int userId){
    if(ctx.session().get("userId") == null){
      return false;
    }
    return ctx.session().get("userId").equals(userId);
  }

  /**
   * Shows login page if no active session. If the user is already logged in it either shows the user's dashboard or the
   * admin panel, depending on the username.
   * @api {get} / Home Page
   * @apiName myAPI
   * @apiGroup Users
   */
  private void home(RoutingContext ctx) {
    //showUserDashboard(ctx);
    String username = ctx.session().get("username");
    if(username == null) {
      // No active login, show login form
      System.out.println("username is null");
      engine.render(ctx.data(), "pages/start.hbs", template -> {      // login.hbs => start.hbs
        ctx.response().setStatusCode(200).putHeader("Content-Type", "text/html").end(template.result());
      });
    } else {
      if("admin".equals(username)) {
        showAdminDashboard(ctx);
      } else {
        showUserDashboard(ctx);
      }
    }
  }

  /**
   * @api {get} /signup Registrierungspage anzeigen
   * @apiName myAPI
   * @apiGroup Users
   *
   * @apiParam {Number} id ID unique de l'utilisateur.
   *
   * @apiSuccess {Number} code Laden der Seite
   *
   *
   * @apiError Seite konnte nicht geladen werden
   */
  private void createAccount(RoutingContext ctx){
    String username = ctx.session().get("username");
    if(username == null) {
      // No active login, show login form
      System.out.println("username is null");
      engine.render(ctx.data(), "pages/registrierung.hbs", template -> {  // signup.hbs => registrierung.hbs
        ctx.response().setStatusCode(200).putHeader("Content-Type", "text/html").end(template.result());
      });
    } else {
      System.out.println("un utilisateur connecte! veillez vous deconnecte");
      ctx.redirect("/");
    }
  }


  /**
   * @api {POST} /users/addccount Senden von Benutzerdaten
   * @apiName myAPI
   * @apiGroup Users
   *
   * @apiParam {Username} Benutzername.
   * @apiParam {Email} Email-Adresse
   * @apiParam {Geschlecht} Geschlecht.
   * @apiParam {Password} Password.
   *@apiParam {ProfilBild} ProfilBild.
   * @apiSuccess {Number} code Landen auf der Startseite.
   *
   * @apiError invalide Daten
   */
  private void addAccount(RoutingContext ctx){
    String username = ctx.request().getParam("username");
    String password = ctx.request().getParam("password");
    String email = ctx.request().getParam("email");
    String gend = ctx.request().getParam("gender");
    //String gend = "M";




    var query = "INSERT INTO Nutzer (Nname, password, email, Geschlecht) VALUES (?, ?, ?, ?) RETURNING * ;";

    pool.preparedQuery(query)
      .execute(Tuple.of(username, password, email, gend))
      .onSuccess(rows -> {
        if(rows.size() > 0) {
          Row row = rows.iterator().next(); // Récupère la première ligne
          int userId = row.getInteger("Nid");



          var queryFavList = "INSERT INTO FavoriteListe (Nid) VALUES (?) ;";
          pool.preparedQuery(queryFavList)
            .execute(Tuple.of(userId))
            .onSuccess(result -> {

              //recuperaction de l'image
              ctx.fileUploads().forEach(fileUpload -> {

                if (fileUpload.size() > 0) {

                  try {
                    // Chemin cible où stocker l'image
                    Path destinationDir = Path.of("file-uploads/images/users");

                    String originalFileName = fileUpload.fileName();

                    if (originalFileName == null || originalFileName.isEmpty()) {
                      System.out.println("Aucun fichier sélectionné, suppression du fichier temporaire.");
                      Files.deleteIfExists(Path.of(fileUpload.uploadedFileName())); // Supprime le fichier vide
                      return;
                    }

                    String fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")); // Récupère l'extension (ex: .jpg)

                    String newFileName = "user_" + userId + fileExtension;

                    // Chemin complet du fichier
                    Path destination = destinationDir.resolve(newFileName);

                    // Déplacer le fichier uploadé vers le dossier "uploads"
                    Files.move(Path.of(fileUpload.uploadedFileName()), destination, StandardCopyOption.REPLACE_EXISTING);

                    System.out.println("Fichier uploadé avec succès : " + newFileName);

                    //sauvegarde en DB
                    var queryPic = "UPDATE Nutzer SET ProfilBild = ? WHERE Nid = ? ;";

                    pool.preparedQuery(queryPic)
                      .execute(Tuple.of(newFileName,userId))
                      .onSuccess(lines -> {
                        System.out.println("image uploaded en DB avec succes");
                      })
                      .onFailure(err -> System.err.println("Erreur lors de sauvegarde profilbild.Erreur SQL: " + err.getMessage()));


                  } catch (Exception e) {
                    System.out.println("Echec upload");
                  }

                }else{
                  System.out.println("Fichier vide détecté, suppression...");
                  try {
                    Files.deleteIfExists(Path.of(fileUpload.uploadedFileName())); // Supprime le fichier temporaire
                  } catch (IOException e) {
                    System.err.println("Erreur lors de la suppression du fichier temporaire : " + e.getMessage());
                  }
                }

              });


            })
            .onFailure(err -> {
              System.err.println("Erreur lors de ajout Favliste.Erreur SQL: " + err.getMessage());
              ctx.put("title", err.getMessage());
              engine.render(ctx.data(), "templates/status.hbs", template -> {
                ctx.response().setStatusCode(401).putHeader("Content-Type", "text/html").end(template.result());
              });
            });

          // Sauvegarde dans la session après succès
          ctx.session().put("username", username);
          ctx.session().put("userId", userId);
          ctx.redirect("/");
        }else{
          System.out.println("L'insert n'a rien retourne");
        }
      }
      )
      .onFailure(err -> {
        System.err.println("Erreur lors de ajout Favliste.Erreur SQL: " + err.getMessage());
        ctx.put("title", err.getMessage());
        engine.render(ctx.data(), "pages/status.hbs", template -> {
          ctx.response().setStatusCode(401).putHeader("Content-Type", "text/html").end(template.result());
        });
      });
  }


  /**
   * @api {get} /login User EInloggin
   * @apiName myAPI
   * @apiGroup Users
   *
   * @apiParam {email} E-Mail Adresse des Benutzers.
   * @apiParam {Password} Password
   *
   * @apiSuccess {Number} code Laden der StartSeite
   *
   *
   * @apiError Seite konnte nicht geladen werden
   */
  private void login(RoutingContext ctx) {
    System.out.println("page login est appelle" );
    String username = ctx.request().getParam("email");
    String password = ctx.request().getParam("password");
    //ctx.redirect("/signup");
    if(username != null && password != null) {
      var query = "SELECT * FROM Nutzer WHERE Email = ? AND Password = ? ;";
      var args = new JsonArray(List.of(username, password));
      System.out.println("QUERY: " + query + " with args: " + args);


      pool.preparedQuery(query)
        .execute(Tuple.of(username, password))
        .onSuccess(rows -> {
          if (rows.size() > 0){
            Row row = rows.iterator().next(); // Récupère la première ligne
            User user= User.fromJSON(row.toJson());
            System.out.println("user: " + user);
            // Sauvegarde dans la session après succès
            ctx.session().put("username", user.getName());
            ctx.session().put("userId", user.getId());
            //ctx.response().setStatusCode(204).end();
            ctx.redirect("/");
          }else{
            //ctx.response().setStatusCode(401).end("Invalid credentials");
            //ctx.redirect("/");
            /*JsonObject rep = new JsonObject();
            rep.put("error", "Email oder Password Falsch. Loggen Sie sich ein oder auf klicken Registrieren Sie sich,falls Sie neu sind !");
            ctx.response().setStatusCode(401).end(rep.encode());*/
            showErrorPage(ctx,401,"Email oder Password Falsch. \n Loggen Sie sich ein oder auf klicken Registrieren Sie sich,falls Sie neu sind !");
          }

          }
        )
        .onFailure(err -> {
          System.err.println("Erreur SQL: " + err.getMessage());
          showErrorPage(ctx,401,err.getMessage());
        });


    } else {
      System.out.println("l'un des param est null");
      showErrorPage(ctx,400,"Bitte alle Felder ausfüllen");
    }
  }


  /**
   * @api {get} /logout User anmelden
   * @apiName myAPI
   * @apiGroup Users
   *
   *
   * @apiSuccess {Number} code Landen auf der Login Seite
   *
   *
   * @apiError Seite konnte nicht geladen werden
   */
  private void abmelden(RoutingContext ctx){
    if(!isConnected(ctx)){
      showErrorPage(ctx,401,"Eine Anmeldung ist erforderlich. Bitte Melden Sie sich ein");
      return;
    }
    System.out.println("utilisateur veut se deconnecter");
    ctx.session().destroy();
    ctx.redirect("/");
  }

  private void showErrorPage (RoutingContext ctx , int statusCode, String msg){
    System.out.println(msg);
    ctx.put("title", msg);
    engine.render(ctx.data(), "pages/status.hbs", template -> {
      ctx.response().setStatusCode(statusCode).putHeader("Content-Type", "text/html").end(template.result());
    });
  }



  private void showAdminDashboard(RoutingContext ctx) {    //int userid = ctx.session().get("userid");
    // Admin can see all trainer pokemon, but trainers only see their own pokemon
    var query = "Select * FROM Nutzer";
    System.out.println("QUERY: " + query);

    pool.query(query)
      .execute()
      .onSuccess(rows -> {
        for (Row row : rows) {
          System.out.println("User: " + row.getString("Email"));
        }
      })
      .onFailure(err -> {
        System.err.println("Erreur SQL: " + err.getMessage());
      });
  }


  /**
   * @api {get} / Home-Page
   * @apiName myAPI
   * @apiGroup Users
   *
   *
   * @apiSuccess {Number} code Laden der StartSeite
   *
   *
   * @apiError Seite konnte nicht geladen werden
   */
  private void showUserDashboard(RoutingContext ctx) {
    //int userid = ctx.session().get("userid");
    if(!isConnected(ctx)){
      showErrorPage(ctx,401,"Eine Anmeldung ist erforderlich. Bitte Melden Sie sich ein");
      return;
    }

    var queryCat = "SELECT * FROM Kategorie ";
    System.out.println("QUERY: " + queryCat);

    pool.preparedQuery(queryCat)
      .execute()
      .onSuccess(rows -> {
          if (rows.size() > 0){
            //je recupere toutes les categorie
            JsonArray categories= new JsonArray();
            for(Row cat: rows){
              categories.add(cat.toJson());
            }

            //je recupere le recettes
            var queryRez = "SELECT r.Rid, r.Title, r.Kid, r.Rbild, a.Nname AS autorname FROM Rezepte r JOIN Nutzer a ON r.Nid = a.Nid ;";
            System.out.println("QUERY: " + queryRez);

            pool.preparedQuery(queryRez)
              .execute()
              .onSuccess(rez -> {
                //je recupere les recette par categorie
                JsonArray CatRez= new JsonArray();
                for(Row line: rez){
                  CatRez.add(line.toJson());
                }

                  // Associer les recettes aux catégories
                  for (int i = 0; i < categories.size(); i++) {
                    JsonArray rezepte = new JsonArray();
                    JsonObject currentCat= categories.getJsonObject(i);
                    for (int j = 0; j < CatRez.size(); j++) {
                      JsonObject currentRez= CatRez.getJsonObject(j);
                      currentRez.put("urlBild","../../../file-uploads/images/recipes/"+ currentRez.getString("Rbild"));
                      if(currentCat.getInteger("Kid").equals(currentRez.getInteger("Kid"))){
                        rezepte.add(currentRez);
                      }

                    }
                    currentCat.put("recipes",rezepte);

                  }
                  ctx.put("categories", categories);
                  ctx.put("connectedUser", ctx.session().get("userId"));
                  engine.render(ctx.data(), "pages/home.hbs", template -> {   // daschboard => home.hbs
                    if (template.succeeded()) {
                      ctx.response()
                        .putHeader("Content-Type", "text/html")
                        .end(template.result());
                    } else {
                      ctx.fail(template.cause());
                    }
                  });

              }
              )
              .onFailure(err -> {
                System.err.println("Erreur de recuperation des reccetes Erreur SQL: " + err.getMessage());
              });

          }else{
            System.out.println("Erreur de recuperation des categories");
            ctx.put("title", "Invalid Login Credentials");
            ctx.redirect("/");
          }

        }
      )
      .onFailure(err -> {
        System.err.println("Erreur SQL: " + err.getMessage());
      });




  }

  /**
   * @api {get} /rezepte/:Rid Rezepte Infos abrufen
   * @apiName myAPI
   * @apiGroup Recipes
   *
   * @apiParam {Rid} Rid Rezept Id
   *
   * @apiSuccess {Title} Rezept Title
   * @apiSuccess {Bild} RezeptsBild
   * @apiSuccess {Zutaten} Zutaten des Rezeptes
   * @apiSuccess {Comments} Kommentare unter dem Rezept
   *
   *
   * @apiUse RecipeNotFound
   */
  private void showRezeptPage(RoutingContext ctx){
    String idR = ctx.pathParam("id");
    int idStr = Integer.parseInt(idR);
    //Rezept r;

    if(!isConnected(ctx)){
      showErrorPage(ctx,401,"Eine Anmeldung ist erforderlich. Bitte Melden Sie sich ein");
      return;
    }

    var query = "SELECT * FROM Rezepte WHERE Rid = ? ;"; // WHERE Rid = ?;
    System.out.println("QUERY: " + query);


    pool.preparedQuery(query)
      .execute(Tuple.of((long)idStr))
      .onSuccess(rows -> {
          if (rows.size() > 0) { // Vérifie si la requête retourne un résultat
            Row row = rows.iterator().next(); // Récupère la première ligne
            System.out.println("Nid "+row.getInteger("Nid"));System.out.println("userId: "+ctx.session().get("userId"));
            if(row.getInteger("Nid").equals(ctx.session().get("userId"))){
              System.out.println("je suis le proprio");
              ctx.put("isUser",true);

              var queryIfNote = "SELECT * FROM Bewertung WHERE Nid = ? AND Rid = ? ;" ;

              pool.preparedQuery(queryIfNote)
                .execute(Tuple.of(ctx.session().get("userId"),idR))
                .onSuccess(result ->{
                  System.out.println("J#ai  deja commente");
                  ctx.remove("noted");
                  if(result.size()>0){
                    ctx.put("noted",true);
                  }
                })
                .onFailure(err ->{System.err.println(err.getMessage());});

            }
            Rezept recipe = Rezept.fromJSON(row.toJson()); // Convertit la ligne en JsonObject et en Rezept
            System.out.println("Recette trouvée : " + recipe.getTitle());


            //Je recupere les Zutaten
            var getZutaten = "SELECT * FROM Rezepte AS r JOIN Zutat AS z ON r.Rid = z.Rid WHERE  r.Rid = ?;";
            pool.preparedQuery(getZutaten)
              .execute(Tuple.of(idStr))
              .onSuccess(ingredients -> {
                JsonArray zutaten= new JsonArray();
                for (Row zutat : ingredients) {
                  zutaten.add(zutat.toJson());
                }

                // recuperation des coms
                var queryGetComm = "SELECT DISTINCT k.Kid, b.Bid, k.Text, n.Nname, n.Email, n.ProfilBild, b.Note FROM Kommentar as k JOIN Nutzer as n ON k.Nid = n.Nid " +
                  "JOIN Bewertung as b ON n.Nid = b.Nid WHERE k.Rid = ? AND b.Rid = ? ;";
                pool.preparedQuery(queryGetComm)
                  .execute(Tuple.of(idStr,idStr))
                  .onSuccess(comments -> {
                    //je recupere tous les commentaires de la recette
                    JsonArray allComs= new JsonArray();
                    for (Row com : comments) {
                      JsonObject current = com.toJson();
                      current.put("fullUrl",pathImages+"users/"+com.getString("ProfilBild"));
                      allComs.add(current);
                    }

                    //prüfen ob, der User noch bewerte darf

                    var queryIfNote = "SELECT * FROM Bewertung WHERE Nid = ? AND Rid = ? ;" ;

                    pool.preparedQuery(queryIfNote)
                      .execute(Tuple.of(ctx.session().get("userId"),idR))
                      .onSuccess(table ->{
                        System.out.println("J#ai  deja commente");
                        ctx.remove("noted");
                        if(table.size()>0){
                          ctx.put("noted",true);
                        }

                        //JSON erstellen
                        //je construis le JSON qui sera lu dynamiquement par la page
                        JsonObject result = new JsonObject();
                        result.put("title",recipe.getTitle());
                        result.put("Rid",recipe.getId());
                        result.put("author",recipe.getId_autor());
                        result.put("kat",recipe.getKat_id());
                        result.put("urlBild",pathImages+"recipes/"+recipe.getUrlBild());
                        result.put("desc",recipe.getBeschreibung());
                        result.put("comments",allComs);
                        result.put("zutaten",zutaten);

                        //ctx.response().setStatusCode(200).end(result.encode());

                        ctx.put("rezept", result);
                        ctx.put("connectedUser", ctx.session().get("userId"));
                        engine.render(ctx.data(), "pages/RezepteInfo.hbs", template -> {
                          if (template.succeeded()) {
                            ctx.response()
                              .putHeader("Content-Type", "text/html")
                              .end(template.result());
                          } else {
                            ctx.fail(template.cause());
                          }
                        });

                        //Ende erstellen JSON
                      })
                      .onFailure(err ->{System.err.println(err.getMessage());});



                  })
                  .onFailure(err -> {
                    System.err.println("Erreur dans la recuperation des Coms. Erreur SQL: " + err.getMessage());
                  });



              })
              .onFailure(err -> {
                System.err.println("Echec lors de le recuperation des Zutaten. Erreur SQL: " + err.getMessage());
              });


          } else {
            System.out.println("Aucune recette trouvee pour ID: " + idStr);
            showErrorPage(ctx,404,"Recipe Not Found");
            //ctx.response().setStatusCode(404).end("Recipe Not Found");
          }
        }

      )
      .onFailure(err -> {
        System.err.println("Erreur de la recuperation de la rezept. Erreur SQL: " + err.getMessage());
        showErrorPage(ctx,400,err.getMessage());
      });

  }


  //Cette fonction construit le json pour la page des favoris d'un User

  /**
   * @api {get} /users/:id/favorite Favoriteliste eines Benutzers anzeigen
   * @apiName myAPI
   * @apiGroup Recipes
   *
   * @apiParam {id} id Bernutzer ID
   * @apiSuccess {Rezepte} List von Lieblingsrezepten
   *
   *
   * @apiUse UserNotAuthorized
   * @apiUse UserNotFound
   */
  private void showFavListe(RoutingContext ctx){
    String idUser = ctx.pathParam("id");

    if(!identPruefen(ctx,Integer.parseInt(idUser))){
      showErrorPage(ctx,403, "Forbidden");
      return;
    }

    //Requete pour recuperer toutes les recettes favorites de l'User
    var query = "SELECT DISTINCT r.Rid, r.Title, r.Rbild FROM Nutzer as u JOIN FavoriteListe AS Fav ON u.Nid = Fav.Nid " +
      "JOIN RezepteFavoriteListe AS RFliste ON Fav.Fid = RFliste.Fid " +
      "JOIN Rezepte AS r ON r.Rid = RFliste.Rid " +
      "WHERE u.Nid = ? ;";

    pool.preparedQuery(query)
      .execute(Tuple.of(idUser))
      .onSuccess(rows -> {
          if(rows.size()>0){
            JsonArray allFavorite= new JsonArray();
            // Recupperation des Favoris
            for(Row row: rows){
              JsonObject current = row.toJson();
              current.put("fullUrl",pathImages+"recipes/"+row.getString("Rbild"));
              allFavorite.add(current);
            }

            ctx.put("favoris",allFavorite);
            ctx.put("connectedUser",ctx.session().get("userId"));
            engine.render(ctx.data(), "pages/Favoris.hbs", template -> {
              if (template.succeeded()) {
                ctx.response()
                  .putHeader("Content-Type", "text/html")
                  .end(template.result());
              } else {
                ctx.fail(template.cause());
              }
            });
          }else{
            System.out.println("Aucun Favori!!");
            ctx.put("connectedUser",ctx.session().get("userId"));
            engine.render(ctx.data(), "pages/Favoris.hbs", template -> {
              if (template.succeeded()) {
                ctx.response()
                  .putHeader("Content-Type", "text/html")
                  .end(template.result());
              } else {
                ctx.fail(template.cause());
              }
            });
          }
        }
      )
      .onFailure(err -> {
        System.err.println("Erreur lors de la recuperation des favoris. Erreur SQL: " + err.getMessage());
        showErrorPage(ctx,400,err.getMessage());
      });
  }


  /**
   * @api {POST} /rezepte/:Rid in Favoriteliste hinzufügen
   * @apiName myAPI
   * @apiGroup FavListe
   *
   * @apiParam {Rid} Rid Rezept Id
   *
   * @apiSuccess {Number} code Hinzufügen in FavoriteListe
   * @apiSuccess {Number} code HTTP 1.0 / OK
   *
   *
   * @apiUse RecipeNotFound
   */
  //cette fonction sert a sauvegarder un favorite en DB
  private void addInFavListe(RoutingContext ctx){


    int idR = Integer.parseInt(ctx.pathParam("Rid"));

    int Uid = Integer.parseInt(ctx.pathParam("Uid"));

    if(!identPruefen(ctx,Uid)){
      showErrorPage(ctx,403, "Forbidden");
      return;
    }

    //recupere le Favlist Id
    var getFavId = "SELECT FavList.Fid FROM FavoriteListe FavList JOIN Nutzer u ON FavList.Nid = u.Nid " +
      "WHERE u.Nid = ? ;";

    pool.preparedQuery(getFavId)
      .execute(Tuple.of(Uid))
      .onSuccess(rows -> {
        if (rows.size() > 0){
          Row row = rows.iterator().next(); // Récupère la première ligne
          int idFav = row.toJson().getInteger("Fid"); // recupere le FavId

          //requete pour ajouter une recette dans la liste des favoris
          var query = "INSERT INTO RezepteFavoriteListe ( Fid, Rid) VALUES ( ?, ?) ;";

          pool.preparedQuery(query)
            .execute(Tuple.of(idFav,idR))
            .onSuccess(lines -> {
                System.out.println(" AJout dans la FavList avec succes");
                //ctx.redirect("/rezepte/:"+idR);
                ctx.response().setStatusCode(204).end();
              }
            )
            .onFailure(err -> {
              System.err.println("Erreur de la requete d'ajout dans la FavoriteListe. Erreur SQL: " + err.getMessage());
              showErrorPage(ctx,400,err.getMessage());
            });

        }else{
          System.out.println("pas de FavLIst ID  assigne");
        }
      }
      )
      .onFailure(err -> {
        System.err.println("Echec lors de l'acquisition de FavList_id. Erreur SQL: " + err.getMessage());
        showErrorPage(ctx,400,"Bad Request");
      });

  }


  /**
   * @api {POST} /users/:id/rezepte ein neues Rezept erstellen
   * @apiName myAPI
   * @apiGroup Recipes
   *
   * @apiParam {id} id User Id
   *
   * @apiSuccess {Number} code Hinzufügen in DB
   * @apiSuccess {Number} code HTTP/ 1.0  OK
   *
   *
   * @apiUse RecipeNotFound
   * @apiUse UserNotAuthorized
   */
  private void addRezept(RoutingContext ctx){

    //ctx.request().setExpectMultipart(true); // Permet de gérer les formulaires multipart/form-data
    //String title = ctx.request().getParam("title");
    //String desc = ctx.request().getParam("desc");
    //String bild = ctx.request().getParam("bild");
    //ctx.request().setExpectMultipart(true);

    //System.out.println("userId = " + ctx.session().get("userId"));
      //if()
    if (!identPruefen(ctx,Integer.parseInt(ctx.pathParam("id")))){
      showErrorPage(ctx,403,"Unauthorized");
      return;
    }

      MultiMap formParams = ctx.request().formAttributes();

      String title = formParams.get("title");
      String desc = formParams.get("desc");
      String bild = formParams.get("bild");
      int kat = Integer.parseInt(formParams.get("category"));
      List<String> zutatsmengeList = formParams.getAll("zMenge");
      List<String> messeeinheitList = formParams.getAll("zMesseinheit");
      List<String> zutatnameList = formParams.getAll("zName");

      List<JsonObject> zutaten = new ArrayList<>();

      for (int i = 0; i < zutatnameList.size(); i++) {
        JsonObject zutat = new JsonObject()
          .put("zMenge", Integer.parseInt(zutatsmengeList.get(i)))
          .put("zMesseinheit", messeeinheitList.get(i))
          .put("zName", zutatnameList.get(i));

        zutaten.add(zutat);
      }


      //ajout de la recette en DB
      var query = "INSERT INTO Rezepte (Nid, Kid, Title, Rbild, Beschreibung)" +
        "VALUES (?, ?, ?, ?, ?) RETURNING * ;";

      pool.preparedQuery(query)
        .execute(Tuple.of(ctx.session().get("userId"), kat, title, bild,desc))
        .onSuccess(rows -> {
          if (rows.size() > 0) {
            Row row = rows.iterator().next(); // Récupérer la première ligne
            int Rid = row.toJson().getInteger("Rid"); //recupere Rezept_id


            //recuperaction de l'image
            ctx.fileUploads().forEach(fileUpload -> {
              System.out.println("je suis entre");
              if (fileUpload.size() > 0) {
                System.out.println("size > 0");
                try {
                  // Chemin cible où stocker l'image
                  Path destinationDir = Path.of("file-uploads/images/recipes");

                  String originalFileName = fileUpload.fileName();

                  if (originalFileName == null || originalFileName.isEmpty()) {
                    System.out.println("Aucun fichier sélectionné, suppression du fichier temporaire.");
                    Files.deleteIfExists(Path.of(fileUpload.uploadedFileName())); // Supprime le fichier vide
                    return;
                  }

                  String fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")); // Récupère l'extension (ex: .jpg)

                  String newFileName = "recipe_" + Rid + fileExtension;

                  // Chemin complet du fichier
                  Path destination = destinationDir.resolve(newFileName);

                  // Déplacer le fichier uploadé vers le dossier "uploads"
                  Files.move(Path.of(fileUpload.uploadedFileName()), destination, StandardCopyOption.REPLACE_EXISTING);

                  System.out.println("Fichier uploadé avec succès : " + newFileName);

                  //sauvegarde en DB
                  var queryPic = "UPDATE Rezepte SET Rbild = ? WHERE Rid = ? ;";

                  pool.preparedQuery(queryPic)
                    .execute(Tuple.of(newFileName,Rid))
                    .onSuccess(lines -> System.out.println("image uploaded en DB avec succes"))
                    .onFailure(err -> System.err.println("Erreur lors de sauvegarde profilbild.Erreur SQL: " + err.getMessage()));


                } catch (Exception e) {
                  System.out.println("Echec upload");
                }

              }else{
                System.out.println("Fichier vide détecté, suppression...");
                try {
                  Files.deleteIfExists(Path.of(fileUpload.uploadedFileName())); // Supprime le fichier temporaire
                } catch (IOException e) {
                  System.err.println("Erreur lors de la suppression du fichier temporaire : " + e.getMessage());
                }
              }

            });



            var zQuery = "INSERT INTO Zutat (Zname, Menge, Messeinheit, Rid)" +
              "VALUES (?, ?, ?, ?) ;";


            //Ajout de tous les Zutaten
            zutaten.forEach(zutat ->{

              String zName = zutat.getString("zName");
              int zMenge = zutat.getInteger("zMenge");
              String zMesseinheit = zutat.getString("zMesseinheit");
              pool.preparedQuery(zQuery)
                .execute(Tuple.of(zName, zMenge, zMesseinheit, Rid))
                .onSuccess(lines -> {
                    //retourner a la page des rezept du User
                    ctx.redirect("/users/"+ctx.session().get("userId")+"/rezepte");
                  }
                )
                .onFailure(err -> {
                  System.err.println("Erreur lors de l'ajout de Zutat. Erreur SQL: " + err.getMessage());
                });

            });

          }

          }
        )
        .onFailure(err -> {
          System.err.println("Erreur lors de l'ajout de Rezept. Erreur SQL: " + err.getMessage());
        });





  }


  /**
   * @api {GET} /users/:id/rezepte Liste der vom User erstellten Rezepte abrufen
   * @apiName myAPI
   * @apiGroup Recipes
   *
   * @apiParam {id} id Benutzer ID
   *
   * @apiSuccess {Number} code Liste aller vom User erstellten Rezepte
   * @apiSuccess {Number} code HTTP/ 1.0  OK
   *
   *
   * @apiUse UserNotAuthorized
   * @apiUse UserNotFound
   */
  private void showRecipeByUserId(RoutingContext ctx){
    int Uid = Integer.parseInt(ctx.request().getParam("id"));

    if(!identPruefen(ctx,Uid)){
      showErrorPage(ctx,403,"Unauthorized");
      return;
    }

    var query = "SELECT * FROM Nutzer AS u JOIN Rezepte AS r ON u.Nid = r.Nid " +
      "JOIN Kategorie AS k ON r.Kid = k.Kid " +
      "WHERE u.Nid = ? ;" ;


    pool.preparedQuery(query)
      .execute(Tuple.of(Uid))
      .onSuccess(rows -> {
        JsonArray meineRezepte = new JsonArray();
          if(rows.size()>0){
            for(Row row: rows){
              String fullUrl = pathImages + "recipes/" + row.getString("Rbild");
              JsonObject r = row.toJson();
              r.remove("Rbild");
              r.put("urlBild", fullUrl ) ;
              //System.out.println("fullUrl: " + fullUrl);
              //System.out.println("ce qui est send: " + r.getString("urlBild"));
              meineRezepte.add(r);
            }
          }else{
            System.out.println("Cet User n'a pas de rezepte");
          }

        ctx.put("meineRezepte",meineRezepte);
          ctx.put("connectedUser", ctx.session().get("userId"));
        engine.render(ctx.data(), "pages/myrecipes.hbs", template -> {
          if (template.succeeded()) {
            ctx.response()
              .putHeader("Content-Type", "text/html")
              .end(template.result());
          } else {
            ctx.fail(template.cause());
          }
        });
      }
      )
      .onFailure(err -> {
        System.err.println("Erreur lors de la recuperation des recettes d'un User.Erreur SQL: " + err.getMessage());
      });

  }

  /**
   * @api {GET} /categories/:id/rezepte Liste der Rezepte einer Kategorie abrufen
   * @apiName myAPI
   * @apiGroup Categories
   *
   * @apiParam {id} id CategorieID
   *
   * @apiSuccess {Number} code Liste aller Rezepte einer Kategorie
   * @apiSuccess {Number} code HTTP/ 1.0  OK
   *
   *
   * @apiUse UserNotAuthorized
   * @apiUse CategoryNotFound
   */
  private void showRezepteByCatId(RoutingContext ctx){
    int kId = Integer.parseInt(ctx.request().getParam("id"));

    if(!isConnected(ctx)){
      showErrorPage(ctx,401,"Eine Anmeldung ist erforderlich. Melden Sie sich ein");
      return;
    }

    var query = "SELECT * FROM Kategorie AS k JOIN Rezepte AS r ON k.Kid = r.Kid " +
      "WHERE k.Kid = ? ;";

    pool.preparedQuery(query)
      .execute(Tuple.of(kId))
      .onSuccess(rows -> {
          JsonArray rezepte = new JsonArray();
          if(rows.size()>0){
            for(Row row: rows){
              JsonObject current = row.toJson();
              current.put("fullUrl", pathImages+"recipes/"+row.getString("Rbild"));
              rezepte.add(row.toJson());
            }
          }else{
            System.out.println("Cette Categorie n'a pas de rezepte");
            showErrorPage(ctx,404,"Diese Kategorie ist nicht vorhanden");
          }

          ctx.put("rezepte",rezepte);
        ctx.put("connectedUser",ctx.session().get("userId"));
          ctx.put("catName",rezepte.getJsonObject(0).getString("KTyp"));
          engine.render(ctx.data(), "pages/recipesCategories.hbs", template -> {
            if (template.succeeded()) {
              ctx.response()
                .putHeader("Content-Type", "text/html")
                .end(template.result());
            } else {
              ctx.fail(template.cause());
            }
          });
        }
      )
      .onFailure(err -> {
        System.err.println("Erreur lors de la recuperation des recettes d'une Categorie.Erreur SQL: " + err.getMessage());
        showErrorPage(ctx,400,"Bad Request");
      });
  }

  /**
   * @api {DELETE} /users/:id Benutzerkonto löschen
   * @apiName myAPI
   * @apiGroup Users
   *
   * @apiParam {id} UserId Benutzer ID
   *
   * @apiSuccess {Number} code 204 bei Ergolg
   * @apiSuccess {Number} code HTTP/ 1.0  OK
   *
   *
   * @apiUse UserNotAuthorized
   * @apiUse UserNotFound
   */
  private void deleteAccount(RoutingContext ctx){
    int id = Integer.parseInt(ctx.pathParam("id"));

    if(!isConnected(ctx)){
      showErrorPage(ctx,401,"Eine Anmeldung ist erforderlich. Melden Sie sich ein");
      return;
    }
    if(!identPruefen(ctx,id)){
      showErrorPage(ctx,403,"Unauthorized");
    }


    var query = "DELETE FROM Nutzer WHERE Nid = ?";

    pool.preparedQuery(query)
      .execute(Tuple.of(id))
      .onSuccess(rows -> {
        ctx.session().destroy();
        //ctx.redirect("/");
        ctx.response().setStatusCode(204).end();
      })
      .onFailure(err -> {System.err.println("Erreur lors de la suppression d'un User.Erreur SQL: " + err.getMessage());});

  }

  /**
   * @api {DELETE} /users/:id/rezepte/:Rid Benutzerkonto löschen
   * @apiName myAPI
   * @apiGroup Recipes
   *
   * @apiParam {id} UserId UserID
   * @apiParam {Rid} Rid Rezept_Id
   *
   * @apiSuccess {Number} code Löschen aus der Datenbank
   * @apiSuccess {Number} code HTTP/ 1.0  OK
   *
   *
   * @apiUse UserNotAuthorized
   * @apiUse UserNotFound
   * @apiUse RecipeNotFound
   */
  private void deleteRecipe(RoutingContext ctx){
    int id = Integer.parseInt(ctx.pathParam("id"));
    int Rid = Integer.parseInt(ctx.pathParam("Rid"));

    if(!isConnected(ctx)){
      showErrorPage(ctx,401,"Eine Anmeldung ist erforderlich. Melden Sie sich ein");
      return;
    }
    if(!identPruefen(ctx,id)){
      showErrorPage(ctx,403,"Forbidden: Aus Berechtigungsgründen können Sie die Aktion durchführen");
      return;
    }
    System.out.println("suppression en cours");

    var query = "DELETE FROM Rezepte WHERE Rid = ?";
    pool.preparedQuery(query)
      .execute(Tuple.of(Rid))
      .onSuccess(rows -> {
        System.out.println("Requete: " + query);
        //System.out.println("To redirect: " + "/users/"+ id + "/rezepte");
        //ctx.redirect("/users/"+ id + "/rezepte");
        ctx.response().setStatusCode(204).end();
      })
      .onFailure(err -> {System.err.println("Erreur lors de la suppression d'une Rezept.Erreur SQL: " + err.getMessage());});
  }

  /**
   * @api {PUT} /users/:id/profil Profil bearbeiten
   * @apiName myAPI
   * @apiGroup Users
   *
   * @apiParam {id} User Id
   *
   * @apiSuccess {Number} code Änderungen wahrnehmen
   * @apiSuccess HTTP/ 1.0  OK
   *
   *
   * @apiUse UserNotAuthorized
   * @apiUse UserNotFound
   */
  private void updateUser(RoutingContext ctx){
    //System.out.println("route trouve");
    int id = Integer.parseInt(ctx.pathParam("id"));
    if(!isConnected(ctx)){
      showErrorPage(ctx,401,"Eine Anmeldung ist erforderlich. Melden Sie sich ein");
      return;
    }
    if(!identPruefen(ctx,id)){
      showErrorPage(ctx,403,"Forbidden: Aus Berechtigungsgründen können Sie die Aktion durchführen");
      return;
    }

    MultiMap formParams = ctx.request().formAttributes();
      String name = formParams.get("Nname");
      String email = formParams.get("email");
      //String bild = formParams.get("urlBild");
      //String gend = formParams.get("gender");

    //recuperaction de l'image
    ctx.fileUploads().forEach(fileUpload -> {
      if (fileUpload.size() > 0) {
        try {
          // Chemin cible où stocker l'image
          Path destinationDir = Path.of("file-uploads/images/users");

          String originalFileName = fileUpload.fileName();

          if (originalFileName == null || originalFileName.isEmpty()) {
            System.out.println("Aucun fichier sélectionné, suppression du fichier temporaire.");
            Files.deleteIfExists(Path.of(fileUpload.uploadedFileName())); // Supprime le fichier vide


            return;
          }

          String fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")); // Récupère l'extension (ex: .jpg)

          String newFileName = "user_" + id + fileExtension;

          // Chemin complet du fichier
          Path destination = destinationDir.resolve(newFileName);

          // Déplacer le fichier uploadé vers le dossier "uploads"
          Files.move(Path.of(fileUpload.uploadedFileName()), destination, StandardCopyOption.REPLACE_EXISTING);

          System.out.println("Fichier uploadé avec succès : " + newFileName);

          //sauvegarde en DB
          var queryPic = "UPDATE Nutzer SET Nname = ? , Email = ? ,ProfilBild = ? WHERE Nid = ? ;";
          System.out.println("Requete "+ queryPic);

          pool.preparedQuery(queryPic)
            .execute(Tuple.of(name, email, newFileName,id))
            .onSuccess(lines -> {
              System.out.println("requete reussi");
              ctx.response().setStatusCode(204).end();
            })
            .onFailure(err -> System.err.println("Erreur lors de sauvegarde profilbild.Erreur SQL: " + err.getMessage()));


        } catch (Exception e) {
          System.out.println("Echec upload");
        }

      }else{
        System.out.println("Fichier vide détecté, suppression...");
        try {
          Files.deleteIfExists(Path.of(fileUpload.uploadedFileName())); // Supprime le fichier temporaire
        } catch (IOException e) {
          System.err.println("Erreur lors de la suppression du fichier temporaire : " + e.getMessage());
        }

        var query = "UPDATE Nutzer " +
          "SET Nname = ? , Email = ?  WHERE Nid = ? ;";
        System.out.println("Requete "+ query);

        pool.preparedQuery(query)
          .execute(Tuple.of(name,email,id))
          .onSuccess(rows -> {
            ctx.response().setStatusCode(200).end();
            //ctx.redirect("/users/"+id+"/profil");
          })
          .onFailure(err -> {System.err.println("Erreur lors de l'update de l'User.Erreur SQL: " + err.getMessage());});

      }
    });
  }

  /**
   * @api {GET} /users/:id/profil Benutzerdaten abrufen
   * @apiName myAPI
   * @apiGroup Users
   *
   * @apiParam {id} UserId User ID
   *
   * @apiSuccess {Number} code Profil anzeigen
   * @apiSuccess {Number} code HTTP/ 1.0  OK
   *
   *
   * @apiUse UserNotAuthorized
   * @apiUse UserNotFound
   */
  private void chargeUserInfos(RoutingContext ctx){
    int id = Integer.parseInt(ctx.pathParam("id"));

    if(!identPruefen(ctx,id)){
      showErrorPage(ctx,403,"Forbidden: Aus Berechtigungsgründen können Sie die Aktion durchführen");
      return;
    }

    var query = "SELECT * FROM Nutzer WHERE Nid = ?";

    pool.preparedQuery(query)
      .execute(Tuple.of(id))
      .onSuccess(rows -> {
        if(rows.size()>0){
          Row row = rows.iterator().next(); // Récupère la première ligne
          String tochargeBild = pathImages + "users/" + row.getString("ProfilBild");

          if(ctx.get("userInfo") != null){
            ctx.remove("userInfo");
            ctx.remove("profilBild");
            ctx.remove("connectedUser");
          }
          ctx.put("connectedUser", ctx.session().get("userId"));
          ctx.put("profilBild",tochargeBild);
          ctx.put("userInfo",row.toJson());
          engine.render(ctx.data(), "pages/konto.hbs", template -> {
            if (template.succeeded()) {
              ctx.response()
                .putHeader("Content-Type", "text/html")
                .end(template.result());
            } else {
              ctx.fail(template.cause());
            }
          });

        }else{
          System.out.println("Pas de User ayant cet ID");
          showErrorPage(ctx,404,"UserNotFound");
          return;
        }
      })
      .onFailure(err -> {System.err.println("Erreur lors de la recuperation des Infos de l'User.Erreur SQL: " + err.getMessage());});

  }


  /**
   * @api {GET} /users/:id/pwd Seite zum Password Ändern laden
   * @apiName myAPI
   * @apiGroup Users
   *
   * @apiParam {id} UserId UserID
   *
   * @apiSuccess {Number} code Interface für die Bearbeitung anzeigen
   * @apiSuccess {Number} code HTTP/ 1.0  OK
   *
   *
   * @apiUse UserNotAuthorized
   * @apiUse UserNotFound
   */
  private void showModifPwdPage(RoutingContext ctx){

    int id = Integer.parseInt(ctx.pathParam("id"));
    if(!isConnected(ctx)){
      showErrorPage(ctx,401,"Eine Anmeldund ist erforderlich. Melden Sie sich ein !");
      return;
    }
    if(!identPruefen(ctx,id)){
      showErrorPage(ctx,403, "Forbidden: Aus Berechtigungsgründen können Sie diese Aktion nicht durchführen");
      return;
    }

    ctx.put("connectedUser",id);
    engine.render(ctx.data(), "pages/changepwd.hbs", template -> {
      if (template.succeeded()) {
        ctx.response()
          .putHeader("Content-Type", "text/html")
          .end(template.result());
      } else {
        ctx.fail(template.cause());
      }
    });
  }

  /**
   * @api {POST} /users/:id/pwd Password ändern
   * @apiName myAPI
   * @apiGroup Users
   *
   * @apiParam {id} User Id
   *
   * @apiSuccess {Number} code Interface für die Bearbeitung anzeigen
   * @apiSuccess {Number} code HTTP/ 1.0  OK
   *
   *
   * @apiUse UserNotAuthorized
   * @apiUse UserNotFound
   */
  private void changePwd(RoutingContext ctx){
    int idUser = Integer.parseInt(ctx.pathParam("id"));

    if(!isConnected(ctx)){
      showErrorPage(ctx,401,"Eine Anmeldung ist erforderlich. Melden Sie sich bitte ein !");
      return;
    }
    if(!identPruefen(ctx,idUser)){
      showErrorPage(ctx,403, "Forbidden: Aus Berechtigungsgründen können Sie diese Aktion nicht durchführen");
      return;
    }

      MultiMap formParams = ctx.request().formAttributes();

      String alt = formParams.get("altPwd");
      String neu = formParams.get("neuPwd");

      var query = "SELECT Password FROM Nutzer WHERE Nid = ?";

      pool.preparedQuery(query)
        .execute(Tuple.of(idUser))
        .onSuccess(rows -> {
          if(rows.size()>0){
            Row row = rows.iterator().next(); // Récupère la première ligne
            String password = row.getString("Password");
            if (password.equals(alt)){

              var query2 = "UPDATE Nutzer SET Password = ? WHERE Nid = ?";
              pool.preparedQuery(query2)
                .execute(Tuple.of(neu,idUser))
                .onSuccess(lines->{
                  ctx.redirect("/users/"+ idUser +"/profil");
                  //ctx.response().setStatusCode(204).end();
                })
                .onFailure(err -> {System.err.println("Erreur lors de l'Update du password de l'User.Erreur SQL: " + err.getMessage());});
            }else{
              System.out.println("Mot de passe incorrect");
              showErrorPage(ctx,403, "Falsches Password. Gehen Sie zurück und geben das richtige aktuelle Password");
              return;
              //showModifPwdPage(ctx);
            }

          }else{
            System.out.println("Pas de User ayant cet ID");
            showErrorPage(ctx,404,"UserNotFound");
          }
        })
        .onFailure(err -> {System.err.println("Erreur lors de la recuperation du password de l'User.Erreur SQL: " + err.getMessage());});



  }

  /**
   * @api {GET} /users/:Nid/rezepte/:Rid/request Liste aller Infos eines bereits erstellten Rezept
   * @apiName myAPI
   * @apiGroup Recipes
   *
   * @apiParam {Nid} UserId Benutzer ID
   * @apiParam {Rid} RecipeID RezeptId
   *
   * @apiSuccess {Number} code Interface für die Bearbeitung anzeigen
   * @apiSuccess {Number} code HTTP/ 1.0  OK
   *
   *
   * @apiUse UserNotAuthorized
   * @apiUse UserNotFound
   * @apiUse RecipeNotFound
   */
  private void showModifRecipePage(RoutingContext ctx){

    //Concept: j'envoies a la page de creation d'une recette le json de la recette construite en Db
    int Nid = Integer.parseInt(ctx.pathParam("Nid"));
    int Rid = Integer.parseInt(ctx.pathParam("Rid"));

    if(!isConnected(ctx)){
      showErrorPage(ctx,401,"Eine Anmeldung ist erforderlich. Melden Sie sich bitte ein !");
      return;
    }
    if(!identPruefen(ctx,Nid)){
      showErrorPage(ctx,403, "Forbidden: Aus Berechtigungsgründen können Sie diese Aktion nicht durchführen");
      return;
    }

    // faudra tester plustard si le Nid correspond au Id de la session

    var queryZutat = "SELECT z.Zname, z.Menge, z.Messeinheit FROM Rezepte AS r " +
      "JOIN Zutat AS z ON r.Rid = z.Rid " +
      "WHERE r.Rid = ? ;";

    var queryKat = "SELECT k.Kid, k.KTyp FROM Kategorie AS k JOIN Rezepte AS r ON k.Kid = r.Kid " +
      "WHERE r.Rid = ? ;";

    var query = "SELECT Title , Rbild , Beschreibung FROM Rezepte WHERE Rid = ? ;";


    pool.preparedQuery(query)
      .execute(Tuple.of(Rid))
      .onSuccess(rows -> {
        if(rows.size()>0){
          Row row = rows.iterator().next();
          String title = row.getString("Title");
          String rbild = row.getString("Rbild");
          String beschreibung = row.getString("Beschreibung");

          // je recupere la categorie
          pool.preparedQuery(queryKat)
            .execute(Tuple.of(Rid))
            .onSuccess(lines -> {
              if(lines.size()>0){
                JsonObject kat = lines.iterator().next().toJson();

                // je recupere ses zutaten
                pool.preparedQuery(queryZutat)
                  .execute(Tuple.of(Rid))
                  .onSuccess(result -> {
                    JsonArray zutaten = new JsonArray();
                    if(result.size()>0){
                      for(Row zutat: result){
                        //zutaten.add(zutat.toJson());
                        JsonObject jsonZutat = new JsonObject();
                        jsonZutat.put("Zname", zutat.getString("Zname"));
                        jsonZutat.put("Menge", zutat.getInteger("Menge"));
                        jsonZutat.put("Messeinheit", zutat.getString("Messeinheit"));
                        zutaten.add(jsonZutat);
                      }
                      //je construis le JSON
                      JsonObject toUpdate = new JsonObject();
                      toUpdate.put("title", title);
                      toUpdate.put("rbild", rbild);
                      toUpdate.put("beschreibung", beschreibung);
                      toUpdate.put("category", kat.getInteger("Kid").toString());
                      toUpdate.put("zutaten", zutaten);

                      //j'envoies le json
                      //ctx.put("recipeToUpdate",toUpdate.encode());
                      System.out.println("j'ai envoye la reponse");
                       ctx.response().setStatusCode(200).putHeader("content-type", "application/json").end(toUpdate.encode());


                    }else{
                      System.out.println("Cette recette n'a pas de Zutaten");
                    }
                  })
                  .onFailure(err -> {System.err.println("Erreur lors de recup zutaten d'une Rezept.Erreur SQL: " + err.getMessage());});

              }else{
                System.out.println("Pas de Categorie ayant cet ID");
                showErrorPage(ctx,404,"Diese Kategorie wurde nicht gefunden");
                return;
              }


            })
            .onFailure(err -> {System.err.println("Erreur lors de recup kat / KatName d'une Rezept.Erreur SQL: " + err.getMessage());});


        }else {
          System.out.println("Pas de Rezepte ayant cet ID");
          showErrorPage(ctx,404,"Rezept nicht gefunden !!!");
        }
        //
      })
      .onFailure(err -> {System.err.println("Erreur lors des infos d'une Rezept.Erreur SQL: " + err.getMessage());});

  }

  /**
   * @api {post} /users/:id/rezepte/:Rid Rezept updaten
   * @apiName GetUser
   * @apiGroup Recipes
   *
   * @apiParam {Nid} UserId BenutzerID
   * @apiParam {Rid} RecipeID Rezept Id
   *
   * @apiSuccess {Number} code Landen auf die Liste der Rezepte
   * @apiSuccess {Number} code HTTP/ 1.0  OK
   *
   *
   * @apiUse UserNotAuthorized
   * @apiUse UserNotFound
   * @apiUse RecipeNotFound
   */
  private void updateRecipe(RoutingContext ctx){
    int userId = Integer.parseInt(ctx.pathParam("id"));
    if(!isConnected(ctx)){
      showErrorPage(ctx,401,"Eine Anmeldung ist erforderlich. Melden Sie sich bitte ein !");
      return;
    }
    if(!identPruefen(ctx,userId)){
      showErrorPage(ctx,403, "Forbidden: Aus Berechtigungsgründen können Sie diese Aktion nicht durchführen");
      return;
    }

    //je dois mettre une condition sur le bouton de la page: il teste si le json est nulle, si non nulle appelle cette methode
    AtomicReference<String> isBild = new AtomicReference<>("");
    AtomicReference<String> isBildVal = new AtomicReference<>("");
    int Rid = Integer.parseInt(ctx.pathParam("Rid"));
    System.out.println("Voici le Rid: "+Rid);
    //ctx.request().bodyHandler(buffer -> {
      MultiMap formParams = ctx.request().formAttributes();

      String title = formParams.get("title");
      String desc = formParams.get("desc");
      String bild = formParams.get("bild");
      int cat = Integer.parseInt(formParams.get("category"));
      List<String> zutatsmengeList = formParams.getAll("zMenge");
      List<String> messeeinheitList = formParams.getAll("zMesseinheit");
      List<String> zutatnameList = formParams.getAll("zName");

      List<JsonObject> zutaten = new ArrayList<>();

      for (int i = 0; i < zutatnameList.size(); i++) {
        JsonObject zutat = new JsonObject()
          .put("Zutatsmenge", Integer.parseInt(zutatsmengeList.get(i)))
          .put("Messeinheit", messeeinheitList.get(i))
          .put("Zutatname", zutatnameList.get(i));

        zutaten.add(zutat);
      }

      //bloc debut
    //recuperaction de l'image
    ctx.fileUploads().forEach(fileUpload -> {
      if (fileUpload.size() > 0) {
        try {
          // Chemin cible où stocker l'image
          Path destinationDir = Path.of("file-uploads/images/recipes");

          String originalFileName = fileUpload.fileName();

          if (originalFileName == null || originalFileName.isEmpty()) {
            System.out.println("Aucun fichier sélectionné, suppression du fichier temporaire.");
            Files.deleteIfExists(Path.of(fileUpload.uploadedFileName())); // Supprime le fichier vide
            return;
          }

          String fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")); // Récupère l'extension (ex: .jpg)

          String newFileName = "recipe_" + Rid + fileExtension;

          // Chemin complet du fichier
          Path destination = destinationDir.resolve(newFileName);

          // Déplacer le fichier uploadé vers le dossier "uploads"
          Files.move(Path.of(fileUpload.uploadedFileName()), destination, StandardCopyOption.REPLACE_EXISTING);

          System.out.println("Fichier uploadé avec succès : " + newFileName);

          //sauvegarde en DB
          isBild.set(", Rbild = ?");
          isBildVal.set(newFileName);
        } catch (Exception e) {
          System.out.println("Echec upload");
        }

      }else{
        System.out.println("Fichier vide détecté, suppression...");
        try {
          Files.deleteIfExists(Path.of(fileUpload.uploadedFileName())); // Supprime le fichier temporaire
        } catch (IOException e) {
          System.err.println("Erreur lors de la suppression du fichier temporaire : " + e.getMessage());
        }
      }
    });
  //Abfrage konstruieren
      var tuple = Tuple.tuple();
    var q =  "UPDATE Rezepte SET Kid = ?, Title = ?, Beschreibung = ?"+isBild.get()+" WHERE Rid = ? ;";
    if (isBild.get().isEmpty() && isBildVal.get().isEmpty()){
      //System.out.println("pas d'image");
      tuple = Tuple.of(cat,title,desc,Rid);
    }else{
      tuple= Tuple.of(cat,title,desc,isBildVal.get(),Rid);
      //System.out.println("il y'a une image. Parametre: isBild=" + isBild+" isbildVal="+isBildVal);
    }
    //bloc fin

      //Update de la recette en DB
      //var query = "UPDATE Rezepte SET Kid = ?, Title = ?, Rbild = ?, Beschreibung = ? WHERE Rid = ? ;" ;
    System.out.println("requete en cours: "+ q);

      pool.preparedQuery(q)
        .execute(tuple)
        .onSuccess(rows -> {
              //suppression des anciens zutaten
              var querySup = "DELETE FROM Zutat WHERE Rid = ? ;";
              pool.preparedQuery(querySup)
                .execute(Tuple.of(Rid))
                .onSuccess(result -> {
                  //ajout des nouveaux Zutaten
                  var zQuery = "INSERT INTO Zutat (Zname, Menge, Messeinheit, Rid) " +
                    "VALUES (?, ?, ?, ?) ;";


                  //Update de tous les Zutaten
                  zutaten.forEach(zutat ->{

                    String zName = zutat.getString("Zutatname");
                    int zMenge = zutat.getInteger("Zutatsmenge");
                    String zMesseinheit = zutat.getString("Messeinheit");
                    pool.preparedQuery(zQuery)
                      .execute(Tuple.of(zName, zMenge, zMesseinheit, Rid))
                      .onSuccess(lines -> {
                          //retourner a la page des rezept du User
                          ctx.redirect("/users/"+ctx.session().get("userId")+"/rezepte");
                        }
                      )
                      .onFailure(err -> {
                        System.err.println("Erreur lors de l'ajout de Zutat. Erreur SQL: " + err.getMessage());
                      });

                  });
                })
                .onFailure(err -> {System.err.println("Erreur lors de la suppression des Zutaten d'une Rezept.Erreur SQL: " + err.getMessage());});





          }
        )
        .onFailure(err -> {
          System.err.println("Erreur lors de l'Update de Rezept. Erreur SQL: " + err.getMessage());
        });

    //});
  }


  /**
   * @api {post} /rezepte/:Rid/comments/:Nid Kommentar erstellen
   * @apiName myAPI
   * @apiGroup Recipes
   *
   * @apiParam {Nid} UserId Benutzer ID
   * @apiParam {Rid} RecipeId Rezept Id
   *
   * @apiSuccess {Number} code
   * @apiSuccess {Number} code HTTP/ 1.0  OK
   *
   *
   * @apiUse UserNotFound
   * @apiUse RecipeNotFound
   */
  private void addComments(RoutingContext ctx){
    //Session prüfen
    if(!isConnected(ctx)){
      showErrorPage(ctx,401,"Eine Anmeldung ist erforderlich. Melden Sie sich bitte ein !");
      return;
    }

    int Nid = Integer.parseInt(ctx.pathParam("Nid"));
    if(!identPruefen(ctx,Nid)){
      showErrorPage(ctx,403, "Forbidden: Aus Berechtigungsgründen können Sie diese Aktion nicht durchführen");
      return;
    }
    int Rid = Integer.parseInt(ctx.pathParam("Rid"));
    MultiMap formParams = ctx.request().formAttributes();
    String com = formParams.get("kommentarInput");

    //ctx.request().bodyHandler(body -> {
      //JsonObject json = body.toJsonObject();


      //String com = json.getString("comment");
      //Integer note = json.getInteger("rating", null);

    //String com = ctx.request().getParam("kommentarInput");

    System.out.println("commentaires "+ com);
    //Integer note = 1;

      var query1 = "INSERT INTO Kommentar (Nid, Rid, Text) VALUES (?, ?, ?) ;";

      var query2 = "INSERT INTO Bewertung (Nid, Rid, Note) VALUES (?, ?, ?) ;" ;

      pool.preparedQuery(query1)
        .execute(Tuple.of(Nid,Rid,com))
        .onSuccess(rows -> {
          if(formParams.get("note") == null){
            ctx.response().setStatusCode(204).end();
          }else{
            Integer note = Integer.parseInt(formParams.get("note"));
            pool.preparedQuery(query2)
              .execute(Tuple.of(Nid,Rid,note))
              .onSuccess(lines -> {
                ctx.response().setStatusCode(204).end();
              })
              .onFailure(err -> {System.err.println("Erreur lors de ajout note.Erreur SQL: " + err.getMessage());});

          }

        })
        .onFailure(err -> {System.err.println("Erreur lors de enregistrement commentaire.Erreur SQL: " + err.getMessage());});

    //});


  }


  private boolean isConnected(RoutingContext ctx){
    return ctx.session().get("userId")!=null && ctx.session().get("username")!=null;
  }



}
