import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

public class Test {
    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        Goods newEnergyCar = new Goods(1, "Tesla", 350000);
        Goods rtx4090 = new Goods(2, "RTX4090", 13899);
        Goods gun = new Goods(3, "AK47", 4172.8);
        Goods cola = new Goods(4, "Cokecola", 5.5);
        Goods cigarette = new Goods(5, "SeptwolvesCigarette", 21.37);
        Goods vipCard = new Goods(6, "BilibiliAnnualVIP", 168);

        Map<Goods, Integer> goodsList1 = new HashMap<>() {{
            put(cigarette, 14);
            put(rtx4090, 2);
            put(newEnergyCar, 6);
        }};
        Map<Goods, Integer> goodsList2 = new HashMap<>() {{
            put(newEnergyCar, 59);
            put(gun, 10000);
            put(cola, 87);
            put(vipCard, 30);
        }};
        Map<Goods, Integer> goodsList3 = new HashMap<>() {{
            put(cigarette, 207);
        }};
        Map<Goods, Integer> goodsList4 = new HashMap<>() {{
            put(newEnergyCar, 3);
            put(cigarette, 18);
            put(cola, 50);
            put(vipCard, 414);
            put(gun, 100000000);
            put(rtx4090, 4090);
        }};

        Order order1 = new Order(1, goodsList2, LocalDateTime.of(2020, 4, 30, 17, 38, 44, 1098));
        Order order2 = new Order(2, goodsList4, LocalDateTime.of(2021, 9, 17, 7, 40, 6));
        Order order3 = new Order(3, goodsList3, LocalDateTime.of(2022, 6, 28, 0, 20, 29));
        Order order4 = new Order(4, goodsList1, LocalDateTime.of(2022, 11, 11, 20, 16, 58));
        Order order5 = new Order(5, goodsList1, LocalDateTime.of(2023, 2, 23, 7, 52, 36));

        try {
            //连接数据库
            JDBCUtil.sqlConnect();

            //商品的增删查改
            JDBCUtil.saveGoods(newEnergyCar);
            newEnergyCar.setPrice(-1);
            JDBCUtil.saveGoods(newEnergyCar);
            JDBCUtil.updateGoods(newEnergyCar);
            newEnergyCar.setPrice(300000);
            JDBCUtil.saveGoods(newEnergyCar);
            JDBCUtil.updateGoods(newEnergyCar);
            JDBCUtil.saveGoods(rtx4090);
            JDBCUtil.getGoods("Gun");
            JDBCUtil.saveGoods(gun);
            JDBCUtil.saveGoods(cola);
            JDBCUtil.saveGoods(cigarette);
            JDBCUtil.saveGoods(vipCard);
            newEnergyCar.setName("BYD");
            JDBCUtil.updateGoods(newEnergyCar);
            JDBCUtil.getGoods("Tesla");
            JDBCUtil.getGoods("BYD");
            JDBCUtil.removeGoods("RTX4090");
            JDBCUtil.removeGoods("Cigarette");
            JDBCUtil.getGoods("RTX4090");
            System.out.println();

            //订单的增删查改
            JDBCUtil.sortOrdersByPrice();
            JDBCUtil.saveOrder(order1);
            JDBCUtil.saveOrder(order2);
            JDBCUtil.saveGoods(rtx4090);
            JDBCUtil.saveOrder(order2);
            JDBCUtil.saveOrder(order2);
            JDBCUtil.getOrder(order3);
            JDBCUtil.saveOrder(order3);
            JDBCUtil.getOrder(order3);
            JDBCUtil.saveOrder(order4);
            JDBCUtil.saveOrder(order5);
            JDBCUtil.getOrder(order5);
            goodsList1.remove(cigarette);
            goodsList1.put(cola, 81);
            order5.setGoodsList(goodsList1);
            JDBCUtil.updateOrder(order5);
            JDBCUtil.getOrder(order5);
            JDBCUtil.removeOrder(order4);
            JDBCUtil.removeOrder(order4);
            System.out.println();

            //商品和订单的排序
            JDBCUtil.sortGoodsByPrice();
            System.out.println();
            JDBCUtil.sortOrdersByPrice();
            System.out.println();
            JDBCUtil.sortOrdersBySubmitTime();
            System.out.println();

            //涉及已存在在订单中的商品的更改
            cigarette.setPrice(65.09);
            cigarette.setName("ZHONGHUA Cigarette");
            JDBCUtil.updateGoods(cigarette);
            JDBCUtil.removeGoods("ZHONGHUA Cigarette");

            //关闭数据库
            JDBCUtil.closeSqlConnect();
        } catch (SQLException ex) {
            System.out.println("已终止与MySQL的连接。");
        }
    }
}