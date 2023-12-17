import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

import com.google.gson.Gson;

public class Order {
    private int id;
    private Map<Goods, Integer> goodsList = new HashMap<>();
    private LocalDateTime submitTime;
    private double sumPrice;

    public Order(int id, Map<Goods, Integer> goodsList, LocalDateTime submitTime) {
        this.id = id;
        this.goodsList = goodsList;
        this.submitTime = submitTime;
        this.sumPrice = 0;
        for (Goods x : goodsList.keySet()) {
            sumPrice += x.getPrice() * goodsList.get(x);
        }
    }

    public void setGoodsList(Map<Goods, Integer> goodsList) {
        this.goodsList = goodsList;
        this.sumPrice = 0;
        for (Goods x : goodsList.keySet()) {
            sumPrice += x.getPrice() * goodsList.get(x);
        }
    }

    public int getID() {
        return id;
    }

    public Map<Goods, Integer> getGoodsList() {
        return goodsList;
    }

    public String getSubmitTime() {
        return submitTime.toString();
    }

    public double getSumPrice() {
        return sumPrice;
    }

    public String turnToJson(Map<Goods, Integer> goodsList) {
        Gson gson = new Gson();
        Map<String, Integer> tempGoodsList = new HashMap<>();
        for (Goods x : goodsList.keySet()) {
            tempGoodsList.put(x.getName(), goodsList.get(x));
        }
        String json = gson.toJson(tempGoodsList);
        return json;
    }
}
