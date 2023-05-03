package iudx.gis.server.apiserver.service;

import static iudx.gis.server.authenticator.Constants.CAT_ITEM_PATH;
import static iudx.gis.server.authenticator.Constants.CAT_SEARCH_PATH;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import iudx.gis.server.authenticator.Constants;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CatalogueService {
  private static final Logger LOGGER = LogManager.getLogger(CatalogueService.class);

  public static WebClient catWebClient;
  private static String catHost;
  private static int catPort;
  private static String catSearchPath;
  private static String catItemPath;
  private final Cache<String, String> groupCache =
      CacheBuilder.newBuilder()
          .maximumSize(1000)
          .expireAfterAccess(Constants.CACHE_TIMEOUT_AMOUNT, TimeUnit.MINUTES)
          .build();
  /*private final Cache<String, String> idCache = CacheBuilder.newBuilder().maximumSize(1000)
  .expireAfterAccess(Constants.CACHE_TIMEOUT_AMOUNT, TimeUnit.MINUTES).build();*/
  private String catBasePath;

  public CatalogueService(Vertx vertx, JsonObject config) {
    catHost = config.getString("catServerHost");
    catPort = config.getInteger("catServerPort");
    catBasePath = config.getString("dxCatalogueBasePath");
    catItemPath = catBasePath + CAT_ITEM_PATH;
    catSearchPath = catBasePath + CAT_SEARCH_PATH;

    WebClientOptions options =
        new WebClientOptions().setTrustAll(true).setVerifyHost(false).setSsl(true);
    if (catWebClient == null) {
      catWebClient = WebClient.create(vertx, options);
    }

    // populateGroupCache(catWebClient).onComplete(handler -> populateResourceCache(catWebClient));
    populateGroupCache(catWebClient);

    vertx.setPeriodic(
        TimeUnit.DAYS.toMillis(1),
        handler -> {
          populateGroupCache(catWebClient);
        });

    vertx.setPeriodic(
        TimeUnit.DAYS.toMillis(1),
        handler -> {
          populateResourceCache(catWebClient);
        });
  }

  public Future<Boolean> populateGroupCache(WebClient client) {
    Promise<Boolean> promise = Promise.promise();
    catWebClient
        .get(catPort, catHost, catSearchPath)
        .addQueryParam("property", "[type]")
        .addQueryParam("value", "[[iudx:ResourceGroup]]")
        .addQueryParam("filter", "[accessPolicy,id]")
        .expect(ResponsePredicate.JSON)
        .send(
            handler -> {
              if (handler.succeeded()) {
                JsonArray response = handler.result().bodyAsJsonObject().getJsonArray("results");
                response.forEach(
                    json -> {
                      JsonObject res = (JsonObject) json;
                      String id = res.getString("id");
                      String[] idArray = id.split("/");
                      groupCache.put(id, res.getString("accessPolicy", "SECURE"));
                    });
                LOGGER.debug("Cache has been populated!");
                LOGGER.debug(groupCache.size());
                promise.complete(true);
              } else if (handler.failed()) {
                promise.fail(handler.cause());
              }
            });
    return promise.future();
  }

  public Future<Boolean> populateResourceCache(WebClient client) {
    Promise<Boolean> promise = Promise.promise();
    catWebClient
        .get(catPort, catHost, catSearchPath)
        .addQueryParam("property", "[type]")
        .addQueryParam("value", "[[iudx:Resource]]")
        .addQueryParam("filter", "[accessPolicy,id]")
        .expect(ResponsePredicate.JSON)
        .send(
            handler -> {
              if (handler.succeeded()) {
                JsonArray response = handler.result().bodyAsJsonObject().getJsonArray("results");
                response.forEach(
                    json -> {
                      // JsonObject res = (JsonObject) json;
                      // String id = res.getString("id");
                      // String groupId = id.substring(0, id.lastIndexOf("/"));
                      // idCache.put(id, res.getString("accessPolicy",
                      // groupCache.getIfPresent(groupId)));
                    });
                promise.complete(true);
              } else if (handler.failed()) {
                promise.fail(handler.cause());
              }
            });
    return promise.future();
  }

  /*public Future<Boolean> isIdPresent(String id) {
    Promise<Boolean> promise = Promise.promise();
    if (id.equalsIgnoreCase(idCache.getIfPresent(id))) {
      promise.complete(true);
    }
    else {
      callCatalogueItemApi(id).onSuccess(handler -> promise.complete(true))
          .onFailure(handler -> promise.fail("Invalid id"));
    }
    return promise.future();
  }

  public Future<Void> callCatalogueItemApi(String id) {
    Promise<Void> promise = Promise.promise();
    String groupId = id.substring(0, id.lastIndexOf("/"));
    catWebClient.get(catPort, catHost, catItemPath).addQueryParam("id", id).send(catHandler -> {
      if (catHandler.succeeded()) {
        JsonArray response = catHandler.result().bodyAsJsonObject().getJsonArray("results");
        response.forEach(json -> {
          JsonObject res = (JsonObject) json;
          if (res.containsKey("accessPolicy"))
            idCache.put(id, res.getString("accessPolicy"));
          else if (groupCache.getIfPresent(groupId) != null) {
            idCache.put(id, groupCache.getIfPresent(groupId));
          } else {
            catWebClient.get(catPort, catHost, catItemPath).addQueryParam(id, groupId)
                .send(catHandler1 -> {
                  JsonArray res1 = catHandler.result().bodyAsJsonObject().getJsonArray("results");
                  res1.forEach(json1 -> {
                    JsonObject res2 = (JsonObject) json1;
                    groupCache.put(groupId, res2.getString("accessPolicy"));
                    idCache.put(id, res2.getString("accessPolicy"));
                  });
                });
          }
        });
        promise.complete();
      } else
        promise.fail("Something went wrong!");
    });
    return promise.future();
  }

  private <T> List<T> toList(JsonArray arr) {
    if (arr == null) {
      return null;
    } else {
      return (List<T>) arr.getList();
    }
  }*/

  public Future<Boolean> isItemExist(String id) {
    LOGGER.trace("isItemExist() started");
    Promise<Boolean> promise = Promise.promise();
    catWebClient
        .get(catPort, catHost, catItemPath)
        .addQueryParam("id", id)
        .expect(ResponsePredicate.JSON)
        .send(
            responseHandler -> {
              if (responseHandler.succeeded()) {
                HttpResponse<Buffer> response = responseHandler.result();
                JsonObject responseBody = response.bodyAsJsonObject();
                if (responseBody.getString("type").equalsIgnoreCase("urn:dx:cat:Success")
                    && responseBody.getInteger("totalHits") > 0) {
                  promise.complete(responseHandler.succeeded());
                  /*if(responseBody.getJsonArray("results").getJsonObject(0)
                  .getJsonArray("type").contains("iudx:Resource")) {
                    promise.complete(true);
                  } else {
                    promise.fail(responseHandler.cause());
                  }*/
                } else {
                  promise.fail(responseHandler.cause());
                }
              } else {
                promise.fail(responseHandler.cause());
              }
            });
    return promise.future();
  }
}
