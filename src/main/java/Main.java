import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import jetbrains.exodus.entitystore.Entity;
import jetbrains.exodus.entitystore.EntityIterable;
import jetbrains.exodus.entitystore.PersistentEntityStore;
import jetbrains.exodus.entitystore.PersistentEntityStores;
import moe.orangelabs.json.Json;
import moe.orangelabs.json.JsonArray;
import moe.orangelabs.json.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.valueOf;
import static moe.orangelabs.json.Json.*;
import static org.eclipse.jetty.http.HttpStatus.FORBIDDEN_403;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        Javalin app = Javalin.create(javalinConfig -> {
            javalinConfig.addStaticFiles("web");
        }).start(8080);

        app.get("/", ctx -> ctx.redirect("/index.html"));

        PersistentEntityStore entityStore = PersistentEntityStores.newInstance("eskurs-db");

        app.put("/stock", ctx -> {
            String name = parse(ctx.body()).getAsObject().getString("name").string;
            entityStore.executeInTransaction(txn -> {
                if (txn.find("stock", "name", name).isEmpty()) {
                    Entity stock = txn.newEntity("stock");
                    stock.setProperty("name", name);
                } else {
                    ctx.status(400);
                    ctx.result("Stock already added");
                }
            });
        });

        app.delete("/stock", ctx -> {
            String name = parse(ctx.body()).getAsObject().getString("name").string;
            entityStore.executeInTransaction(txn -> {
                txn.find("stock", "name", name).forEach(Entity::delete);
            });
        });

        app.get("/stock", ctx -> {
            JsonArray array = new JsonArray();
            entityStore.executeInReadonlyTransaction(txn -> {
                txn.getAll("stock").forEach(entity -> {
                    array.add(string((String) entity.getProperty("name")));
                });
            });
            ctx.result(array.toString());
            ctx.contentType("application/json");
        });

        app.put("/price", (JsonHandler) (ctx, input) -> {
            String name = input.getAsObject().getString("name").string;
            //days since 1 jan 1970
            int date = input.getAsObject().getNumber("date").intValue();
            BigDecimal price = input.getAsObject().getNumber("price").value;
            BigDecimal dividends = input.getAsObject().getNumber("dividends").value;

            int normalPrice = price.intValue() * 100 + price.remainder(ONE).multiply(valueOf(100)).intValue();
            int normalDividends = dividends.intValue() * 100 + dividends.remainder(ONE).multiply(valueOf(100)).intValue();

            entityStore.executeInTransaction(txn -> {
                Entity stock = txn.find("stock", "name", name).getFirst();

                if (stock == null) {
                    ctx.status(400);
                } else {
                    Entity oldPirce = stock.getLinks("price").intersect(txn.find("price", "date", date)).getFirst();

                    if (oldPirce == null) {
                        Entity priceEntity = txn.newEntity("price");
                        priceEntity.setProperty("date", date);
                        priceEntity.setProperty("price", normalPrice);
                        priceEntity.setProperty("dividends", normalDividends);
                        priceEntity.setProperty("name", name);
                        stock.addLink("price", priceEntity);
                        priceEntity.addLink("stock", stock);
                    } else {
                        oldPirce.setProperty("price", normalPrice);
                    }
                }
            });
        });

        //get prices
        app.post("/price", (JsonHandler) (ctx, input) -> {
            String name = input.getAsObject().getString("name").string;
            JsonArray array = new JsonArray();
            entityStore.executeInReadonlyTransaction(txn -> {
                Entity stock = txn.find("stock", "name", name).getFirst();
                if (stock != null) {
                    EntityIterable prices = stock.getLinks("price");
                    prices.forEach(entity -> array.add(object(
                            "price", (Integer) entity.getProperty("price"),
                            "date", entity.getProperty("date"),
                            "dividends", entity.getProperty("dividends")
                    )));
                }
            });
            ctx.result(array.toString());
            ctx.contentType("application/json");
        });

        app.put("/portfolio", (JsonHandler) (ctx, input) -> {
            String userName = input.getAsObject().getString("user").string;
            String stockName = input.getAsObject().getString("name").string;
            int date = input.getAsObject().getNumber("date").intValue();
            int amount = input.getAsObject().getNumber("amount").intValue();

            entityStore.executeInTransaction(txn -> {
                if (txn.find("stock", "name", stockName).isEmpty()) {
                    ctx.status(FORBIDDEN_403);
                } else {
                    Entity user = txn.find("user", "username", userName)
                            .intersect(txn.find("user", "stockname", stockName))
                            .intersect(txn.find("user", "date", date))
                            .getFirst();

                    if (user == null) {
                        user = txn.newEntity("user");
                        user.setProperty("username", userName);
                        user.setProperty("stockname", stockName);
                        user.setProperty("date", date);
                    }

                    user.setProperty("amount", amount);
                }
            });
        });

        app.post("/portfolio", (JsonHandler) (ctx, input) -> {
            String userName = input.getAsObject().getString("user").string;
            JsonObject result = new JsonObject();
            entityStore.executeInReadonlyTransaction(txn -> {
                txn.getAll("stock").forEach(stockEntity -> {
                    JsonObject stockResult = new JsonObject();
                    String stockName = (String) stockEntity.getProperty("name");
                    txn.find("user", "username", userName)
                            .intersect(txn.find("user", "stockname", stockName))
                            .forEach(userEntity -> {
                                stockResult.castAndPut(String.valueOf(userEntity.getProperty("date")), userEntity.getProperty("amount"));
                            });
                    result.castAndPut(stockName, stockResult);
                });
            });
            ctx.result(result.toString());
            ctx.contentType("application/json");
        });

        app.delete("/portfolio", (JsonHandler) (ctx, input) -> {
            String userName = input.getAsObject().getString("user").string;
            String stockName = input.getAsObject().getString("name").string;
            int date = input.getAsObject().getNumber("date").intValue();

            entityStore.executeInTransaction(txn -> {
                txn.find("user", "username", userName)
                        .intersect(txn.find("user", "stockname", stockName))
                        .intersect(txn.find("user", "date", date))
                        .forEach(Entity::delete);
            });
        });

        app.post("/analytics", (JsonHandler) (ctx, input) -> {
            String userName = input.getAsObject().getString("user").string;
            JsonObject result = new JsonObject();

            entityStore.executeInReadonlyTransaction(txn -> {
                int userFirst = (Integer) txn.sort("user", "date", true).getFirst().getProperty("date");
                int userLast = (Integer) txn.sort("user", "date", true).getLast().getProperty("date");
                int priceFirst = (Integer) txn.sort("price", "date", true).getFirst().getProperty("date");
                int priceLast = (Integer) txn.sort("price", "date", true).getLast().getProperty("date");

                final int firstDate = Math.min(userFirst, priceFirst);
                final int lastDate = Math.max(userLast, priceLast);

                //purchase price
                AtomicInteger purchasePrice = new AtomicInteger();
                txn.getAll("stock").forEach(stockEntity -> {
                    txn.find("user", "username", userName)
                            .intersect(txn.find("user", "stockname", stockEntity.getProperty("name")))
                            .forEach(portfolioEntity -> {
                                Entity priceEntity = txn.find("price", "name", portfolioEntity.getProperty("stockname"))
                                        .intersect(txn.find("price", "date", portfolioEntity.getProperty("date"))).getFirst();
                                if (priceEntity != null) {
                                    purchasePrice.addAndGet(
                                            (Integer) portfolioEntity.getProperty("amount") * (Integer) priceEntity.getProperty("price")
                                    );
                                }
                            });
                });
                result.castAndPut("purchase", purchasePrice.get());

                //portfolio stock count by stock by date
                //how many stocks of each stock we have at each day
                JsonObject countByStockByDate = new JsonObject();
                result.castAndPut("countByStockByDate", countByStockByDate);
                txn.getAll("stock").forEach(stockEntity -> {
                    JsonObject priceByDate = new JsonObject();

                    AtomicInteger stockCount = new AtomicInteger(0);
                    txn.find("user", "username", userName)
                            .intersect(txn.find("user", "stockname", stockEntity.getProperty("name")))
                            .intersectSavingOrder(txn.sort("user", "date", true))
                            .forEach(portfolioEntity -> {
                                stockCount.addAndGet((Integer) portfolioEntity.getProperty("amount"));
                                priceByDate.castAndPut(String.valueOf(portfolioEntity.getProperty("date")), stockCount.get());
                            });
                    countByStockByDate.castAndPut(stockEntity.getProperty("name"), priceByDate);
                });
                countByStockByDate.forEach((jsonString, json) -> interpolate(json.getAsObject(), firstDate, lastDate));

                //how much each stock cost at any day grouped by stock
                JsonObject priceByStockByDate = new JsonObject();
                result.castAndPut("byStockByDate", priceByStockByDate);
                countByStockByDate.forEach((stockName, countHistory) -> {
                    JsonObject stockPriceHistory = new JsonObject();
                    priceByStockByDate.put(stockName, stockPriceHistory);
                    countHistory.getAsObject().forEach((date, count) -> {
                        Entity priceEntity = txn.find("price", "name", stockName.string)
                                .intersect(txn.find("price", "date", Integer.valueOf(date.string))).getFirst();
                        if (priceEntity != null) {
                            stockPriceHistory.castAndPut(date, count.getAsNumber().intValue() * (Integer) priceEntity.getProperty("price"));
                        }
                    });
                });
                priceByStockByDate.forEach((jsonString, json) -> interpolate(json.getAsObject(), firstDate, lastDate));


                //portfolio price by date
                JsonObject portfolioPriceByDate = new JsonObject();
                result.castAndPut("portfolioPriceByDate", portfolioPriceByDate);
                priceByStockByDate.forEach((stockName, priceHistory) -> {
                    priceHistory.getAsObject().forEach((date, price) -> {
                        if (!portfolioPriceByDate.containsKey(date)) {
                            portfolioPriceByDate.castAndPut(date, 0);
                        }
                        portfolioPriceByDate.castAndPut(date,
                                portfolioPriceByDate.getNumber(date).intValue() + price.getAsNumber().intValue());
                    });
                });
                interpolate(portfolioPriceByDate, firstDate, lastDate);

                //dividends paid per one stock grouped by stock name and date
                JsonObject dividendsByStockByDate = new JsonObject();
                result.castAndPut("dividendsByStockByDate", dividendsByStockByDate);
                txn.getAll("stock").forEach(stockEntity -> {
                    dividendsByStockByDate.castAndPut(stockEntity.getProperty("name"), object());
                    txn.find("price", "stockname", stockEntity.getProperty("name")).forEach(priceEntity -> {
                        dividendsByStockByDate.getObject(stockEntity.getProperty("name")).castAndPut(
                                String.valueOf(priceEntity.getProperty("date")),
                                priceEntity.getProperty("dividends")
                        );
                    });
                });

                JsonObject dividendsSumByStockByDate = new JsonObject();
                result.castAndPut("dividendsSumByStockByDate", dividendsSumByStockByDate);
                countByStockByDate.forEach((stockName, countHistory) -> {
                    AtomicInteger previousSum = new AtomicInteger();
                    JsonObject sum = new JsonObject();
                    dividendsSumByStockByDate.put(stockName, sum);
                    countHistory.getAsObject().forEach((date, count) -> {
                        if (dividendsByStockByDate.getObject(stockName).containsNumber(date)) {
                            previousSum.addAndGet(count.getAsNumber().intValue() * dividendsByStockByDate.getObject(stockName).getNumber(date).intValue());
                        }
                        sum.castAndPut(date, previousSum);
                    });
                });
            });

            ctx.result(result.toString());
            ctx.contentType("application/json");
        });

        app.options("/stock", ctx -> {
            ctx.header("access-control-allow-origin", "*");
        });
        app.options("/price", ctx -> {
            ctx.header("access-control-allow-origin", "*");
        });
        app.options("/portfolio", ctx -> {
            ctx.header("access-control-allow-origin", "*");
        });
        app.options("/analytics", ctx -> {
            ctx.header("access-control-allow-origin", "*");
        });
    }

    @FunctionalInterface
    public interface JsonHandler extends Handler {
        @Override
        default void handle(@NotNull Context ctx) throws Exception {
            try {
                Json input = parse(ctx.body());
                handleJson(ctx, input);
            } catch (Exception e) {
                LOGGER.error("Error handling request ctx={}", ctx, e);
                ctx.status(400);
            }
        }

        void handleJson(@NotNull Context ctx, @NotNull Json input) throws Exception;
    }

    public static JsonObject interpolate(JsonObject object, int start, int end) {
        ArrayList<Integer> dates = new ArrayList<>();
        object.keySet().forEach(date -> {
            dates.add(Integer.valueOf(date.string));
        });
        Collections.sort(dates);

        int first = dates.size() > 0 ? dates.get(0) : end;
        int last = dates.size() > 0 ? dates.get(dates.size() - 1) : end;

        if (first > start) {
            for (int i = start; i <= first; i++) {
                object.castAndPut(String.valueOf(i), 0);
            }
        }
        if (last < end) {
            for (int i = last; i <= end; i++) {
                object.castAndPut(String.valueOf(i), object.get(String.valueOf(last)));
            }
        }
        for (int i = start + 1; i <= end; i++) {
            if (!object.containsKey(String.valueOf(i))) {
                object.castAndPut(String.valueOf(i), object.get(String.valueOf(i - 1)));
            }
        }
        return object;
    }

}
