import java.util.Scanner;
import java.util.Map;
import java.sql.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public final class JDBCUtil {
    private static String URL;
    private static String USERNAME;
    private static String PASSWORD;
    private static Connection CONNECTION;
    private static PreparedStatement COMMAND;
    private static ResultSet RESULTSET;

    /**
     * 建立与MySQL的连接并创建数据库
     * @throws SQLException
     */
    public static void sqlConnect() throws SQLException {
        try {
            // 登录MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");
            URL = "jdbc:mysql://localhost:3306/?useUnicode=true&characterEncoding=UTF-8&useSSL=true&allowMultiQueries=true";
            USERNAME = "root";
            Scanner scanner = new Scanner(System.in);
            System.out.print("请输入MySQL的root用户密码：");
            PASSWORD = scanner.nextLine();
            scanner.close();

            // 删除原先存在的ordermanagesys数据库并创建空白的同名数据库
            CONNECTION = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            CONNECTION.setAutoCommit(false);
            COMMAND = CONNECTION.prepareStatement("drop database if exists ordermanagesys;create database ordermanagesys charset=utf8;");
            COMMAND.executeUpdate();
            CONNECTION.commit();

            // 连接ordermanagesys数据库
            URL = "jdbc:mysql://localhost:3306/ordermanagesys?useUnicode=true&characterEncoding=UTF-8&useSSL=true&allowMultiQueries=true";
            CONNECTION = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            CONNECTION.setAutoCommit(false);

            //创建商品信息表和订单信息表
            COMMAND = CONNECTION.prepareStatement("""
                    create table `goodslist`(
                    `id` int,
                    `name` varchar(50),
                    `price` double
                    );
                    create table `orderslist`(
                    `id` int,
                    `information` json,
                    `time` datetime,
                    `price` double
                    );
                    """);
            COMMAND.executeUpdate();
            CONNECTION.commit();
        } catch (Exception ex) {
            try {
                CONNECTION.rollback();
                System.out.println("sqlConnect()发生错误，操作已回滚。");
            } catch (Exception ex1) {
                System.out.println("sqlConnect()发生错误，操作回滚失败。");
            }
            closeSqlConnect();
            throw new SQLNonTransientConnectionException();
        }
    }

    /**
     * 保存新商品
     * @param goods 待添加商品
     * @throws SQLException
     */
    public static void saveGoods(Goods goods) throws SQLException {
        if (goods.getPrice() <= 0) {
            System.out.println("商品价格不合法！");
            return;
        }
        try {
            //确认商品是否已在列表中
            CONNECTION.setAutoCommit(false);
            COMMAND = CONNECTION.prepareStatement("select `name` from `goodslist` where `name`=?");
            COMMAND.setObject(1, goods.getName());
            RESULTSET = COMMAND.executeQuery();

            if (RESULTSET.next()) {
                System.out.println("该商品已存在！");
            } else {
                COMMAND = CONNECTION.prepareStatement("insert into `goodslist` values (?,?,?)");
                COMMAND.setObject(1, goods.getId());
                COMMAND.setObject(2, goods.getName());
                COMMAND.setObject(3, goods.getPrice());
                COMMAND.executeUpdate();
                System.out.println("商品信息导入成功！");
            }
            CONNECTION.commit();
        } catch (Exception ex) {
            try {
                CONNECTION.rollback();
                System.out.println("saveGoods()发生错误，操作已回滚。");
            } catch (Exception ex1) {
                System.out.println("saveGoods()发生错误，操作回滚失败。");
            }
            closeSqlConnect();
            throw new SQLNonTransientConnectionException();
        }
    }

    /**
     * 查找指定名字的商品的相关信息
     * @param name 商品名称
     * @throws SQLException
     */
    public static void getGoods(String name) throws SQLException {
        try {
            //确认商品是否存在
            CONNECTION.setAutoCommit(false);
            COMMAND = CONNECTION.prepareStatement("select `name` from `goodslist` where `name`=?");
            COMMAND.setObject(1, name);
            RESULTSET = COMMAND.executeQuery();
            if (RESULTSET.next()) {
                COMMAND = CONNECTION.prepareStatement("select `id`,`price` from `goodslist` where `name`=?");
                COMMAND.setObject(1, name);
                RESULTSET = COMMAND.executeQuery();
                if (RESULTSET.next()) {
                    System.out.println("查询成功，商品信息如下：");
                    System.out.printf("编号：%d\n名称：%s\n单价：%.2f元\n", RESULTSET.getInt(1), name, RESULTSET.getDouble(2));
                }
            } else {
                System.out.println("查询失败，请检查商品名称是否有误！");
            }
            CONNECTION.commit();
        } catch (Exception ex) {
            try {
                CONNECTION.rollback();
                System.out.println("getGoods()发生错误，操作已回滚。");
            } catch (Exception ex1) {
                System.out.println("getGoods()发生错误，操作回滚失败。");
            }
            closeSqlConnect();
            throw new SQLNonTransientConnectionException();
        }
    }

    /**
     * 更新商品的信息（如价格、名称等）
     * @param goods 商品的最新信息
     * @throws SQLException
     */
    public static void updateGoods(Goods goods) throws SQLException {
        if (goods.getPrice() <= 0) {
            System.out.println("商品价格不合法！");
            return;
        }
        try {
            //确认商品是否存在
            CONNECTION.setAutoCommit(false);
            COMMAND = CONNECTION.prepareStatement("select `name`,`price` from `goodslist` where `id`=?");
            COMMAND.setObject(1, goods.getId());
            RESULTSET = COMMAND.executeQuery();
            if (RESULTSET.next()) {
                //修改指定商品所涉订单的相关信息
                //id: 订单编号，sumPrice：订单总额，originalName：商品原名，originalPrice：商品原单价，goodslist：查询到的采购清单
                String originalName = RESULTSET.getString(1);
                double originalPrice = RESULTSET.getDouble(2), sumPrice;
                int id;
                Gson gson = new Gson();
                boolean exist;
                Map goodslist;
                COMMAND = CONNECTION.prepareStatement("select `id`,`information`,`price` from `orderslist`");
                RESULTSET = COMMAND.executeQuery();
                while (RESULTSET.next()) {
                    exist = false;
                    id = RESULTSET.getInt(1);
                    sumPrice = RESULTSET.getDouble(3);
                    goodslist = gson.fromJson(RESULTSET.getString(2), new TypeToken<>() {
                    }.getType());
                    //确认采购清单中是否有相关商品，有则更新信息
                    for (Object entry : goodslist.keySet()) {
                        if (entry.toString().equals(originalName)) {
                            int buyNum = (int) Double.parseDouble(goodslist.get(entry).toString());
                            sumPrice = sumPrice - originalPrice * buyNum + goods.getPrice() * buyNum;
                            exist = true;
                            break;
                        }
                    }
                    //没有则跳过，减少访问数据库的频率
                    if (!exist) {
                        continue;
                    }

                    COMMAND = CONNECTION.prepareStatement("update `orderslist` set `information`=?,`price`=? where `id`=?");
                    COMMAND.setObject(1, RESULTSET.getString(2).replaceAll(originalName, goods.getName()));
                    COMMAND.setObject(2, sumPrice);
                    COMMAND.setObject(3, id);
                    COMMAND.executeUpdate();
                }

                //更新商品信息
                COMMAND = CONNECTION.prepareStatement("update `goodslist` set `name`=?,`price`=? where `id`=?");
                COMMAND.setObject(1, goods.getName());
                COMMAND.setObject(2, goods.getPrice());
                COMMAND.setObject(3, goods.getId());
                COMMAND.executeUpdate();
                System.out.println("成功修改商品信息，包含该商品的订单信息已同步修改！");
            } else {
                System.out.println("修改失败，该商品不存在！");
            }
            CONNECTION.commit();
        } catch (Exception ex) {
            try {
                CONNECTION.rollback();
                System.out.println("updateGoods()发生错误，操作已回滚。");
            } catch (Exception ex1) {
                System.out.println("updateGoods()发生错误，操作回滚失败。");
            }
            closeSqlConnect();
            throw new SQLNonTransientConnectionException();
        }
    }

    /**
     * 移除指定名称的商品
     * @param name 商品名称
     * @throws SQLException
     */
    public static void removeGoods(String name) throws SQLException {
        try {
            //确认商品是否存在
            CONNECTION.setAutoCommit(false);
            COMMAND = CONNECTION.prepareStatement("select `name` from `goodslist` where `name`=?");
            COMMAND.setObject(1, name);
            RESULTSET = COMMAND.executeQuery();
            if (RESULTSET.next()) {
                COMMAND = CONNECTION.prepareStatement("delete from `goodslist` where `name`=?");
                COMMAND.setObject(1, name);
                COMMAND.executeUpdate();
                //删除该商品所涉订单
                COMMAND = CONNECTION.prepareStatement("select `id`,`information` from `orderslist`");
                RESULTSET = COMMAND.executeQuery();
                Gson gson = new Gson();
                Map goodslist;
                while (RESULTSET.next()) {
                    goodslist = gson.fromJson(RESULTSET.getString(2), new TypeToken<>() {
                    }.getType());
                    for (Object entry : goodslist.keySet()) {
                        if (entry.toString().equals(name)) {
                            COMMAND = CONNECTION.prepareStatement("delete from `orderslist` where `id`=?");
                            COMMAND.setObject(1, RESULTSET.getInt(1));
                            COMMAND.executeUpdate();
                            break;
                        }
                    }
                }
                System.out.println("删除成功，该商品所涉订单亦已同步删除。");
            } else {
                System.out.println("删除失败，请检查商品名称是否有误！");
            }
            CONNECTION.commit();
        } catch (Exception ex) {
            try {
                CONNECTION.rollback();
                System.out.println("removeGoods()发生错误，操作已回滚。");
            } catch (Exception ex1) {
                System.out.println("removeGoods()发生错误，操作回滚失败。");
            }
            closeSqlConnect();
            throw new SQLNonTransientConnectionException();
        }
    }

    /**
     * 添加新订单
     * @param order 新订单信息
     * @throws SQLException
     */
    public static void saveOrder(Order order) throws SQLException {
        try {
            //确认订单是否存在
            CONNECTION.setAutoCommit(false);
            COMMAND = CONNECTION.prepareStatement("select `id` from `orderslist` where `id`=?");
            COMMAND.setObject(1, order.getID());
            RESULTSET = COMMAND.executeQuery();
            if (RESULTSET.next()) {
                System.out.println("提交失败，该订单已存在！");
            } else {
                COMMAND = CONNECTION.prepareStatement("select `id` from `goodslist`", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                RESULTSET = COMMAND.executeQuery();

                //核查订单中是否有不存在的商品
                boolean exist;
                for (Goods entry : order.getGoodsList().keySet()) {
                    exist = false;
                    while (RESULTSET.next()) {
                        if (entry.getId() == RESULTSET.getInt(1)) {
                            exist = true;
                            break;
                        }
                    }
                    if (!exist) {
                        System.out.println("提交失败，采购清单中包含不存在的商品！");
                        return;
                    }
                    RESULTSET.beforeFirst();
                }

                COMMAND = CONNECTION.prepareStatement("insert into `orderslist` values (?,?,?,?)");
                COMMAND.setObject(1, order.getID());
                COMMAND.setObject(2, order.turnToJson(order.getGoodsList()));
                COMMAND.setObject(3, order.getSubmitTime());
                COMMAND.setObject(4, order.getSumPrice());
                COMMAND.executeUpdate();
                System.out.println("成功提交订单！");
            }
            CONNECTION.commit();
        } catch (Exception ex) {
            try {
                CONNECTION.rollback();
                System.out.println("saveOrder()发生错误，操作已回滚。");
            } catch (Exception ex1) {
                System.out.println("saveOrder()发生错误，操作回滚失败。");
            }
            closeSqlConnect();
            throw new SQLNonTransientConnectionException();
        }
    }

    /**
     * 查询指定订单的信息
     * @param order 指定订单
     * @throws SQLException
     */
    public static void getOrder(Order order) throws SQLException {
        try {
            //确认订单是否存在
            CONNECTION.setAutoCommit(false);
            COMMAND = CONNECTION.prepareStatement("select `id` from `orderslist` where id=?");
            COMMAND.setObject(1, order.getID());
            RESULTSET = COMMAND.executeQuery();
            if (RESULTSET.next()) {
                COMMAND = CONNECTION.prepareStatement("select `information`,`time`,`price` from `orderslist` where id=?");
                COMMAND.setObject(1, order.getID());
                RESULTSET = COMMAND.executeQuery();
                System.out.printf("查询成功，订单信息如下：\n编号：%d\n采购清单：\n", order.getID());
                while (RESULTSET.next()) {
                    Gson gson = new Gson();
                    Map goodslist = gson.fromJson(RESULTSET.getString(1), new TypeToken<>() {
                    }.getType());
                    for (Object entry : goodslist.keySet()) {
                        System.out.print(entry);
                        System.out.printf("    %d件\n", ((Double) goodslist.get(entry)).intValue());
                    }
                    System.out.printf("提交时间：%s\n总价：%.2f元\n", RESULTSET.getString(2), RESULTSET.getDouble(3));
                }
            } else {
                System.out.println("查询失败，该订单不存在！");
            }
            CONNECTION.commit();
        } catch (Exception ex) {
            try {
                CONNECTION.rollback();
                System.out.println("getOrder()发生错误，操作已回滚。");
            } catch (Exception ex1) {
                System.out.println("getOrder()发生错误，操作回滚失败。");
            }
            closeSqlConnect();
            throw new SQLNonTransientConnectionException();
        }
    }

    /**
     * 更新指定订单的信息
     * @param order 订单的最新信息
     * @throws SQLException
     */
    public static void updateOrder(Order order) throws SQLException {
        try {
            //确认订单是否存在
            CONNECTION.setAutoCommit(false);
            COMMAND = CONNECTION.prepareStatement("select `id` from `orderslist` where id=?");
            COMMAND.setObject(1, order.getID());
            RESULTSET = COMMAND.executeQuery();
            if (RESULTSET.next()) {
                //核查订单中是否有不存在的商品
                boolean exist;
                for (Goods entry : order.getGoodsList().keySet()) {
                    exist = false;
                    while (RESULTSET.next()) {
                        if (entry.getId() == RESULTSET.getInt(1)) {
                            exist = true;
                            break;
                        }
                    }
                    if (!exist) {
                        System.out.println("更新失败，采购清单中包含不存在的商品！");
                        return;
                    }
                    RESULTSET.beforeFirst();
                }

                COMMAND = CONNECTION.prepareStatement("update `orderslist` set `information`=?,`time`=?,`price`=? where `id`=?");
                COMMAND.setObject(1, order.turnToJson(order.getGoodsList()));
                COMMAND.setObject(2, order.getSubmitTime());
                COMMAND.setObject(3, order.getSumPrice());
                COMMAND.setObject(4, order.getID());
                COMMAND.executeUpdate();
                System.out.println("订单修改成功！");
            } else {
                System.out.println("订单修改失败，该订单不存在！");
            }
            CONNECTION.commit();
        } catch (Exception ex) {
            try {
                CONNECTION.rollback();
                System.out.println("updateOrder()发生错误，操作已回滚。");
            } catch (Exception ex1) {
                System.out.println("updateOrder()发生错误，操作回滚失败。");
            }
            closeSqlConnect();
            throw new SQLNonTransientConnectionException();
        }
    }

    /**
     * 移除指定订单
     * @param order 指定订单
     * @throws SQLException
     */
    public static void removeOrder(Order order) throws SQLException {
        try {
            //确认订单是否存在
            CONNECTION.setAutoCommit(false);
            COMMAND = CONNECTION.prepareStatement("select `id` from `orderslist` where id=?");
            COMMAND.setObject(1, order.getID());
            RESULTSET = COMMAND.executeQuery();
            if (RESULTSET.next()) {
                COMMAND = CONNECTION.prepareStatement("delete from `orderslist` where id=?");
                COMMAND.setObject(1, order.getID());
                COMMAND.executeUpdate();
                System.out.println("删除成功！");
            } else {
                System.out.println("删除失败，该订单不存在！");
            }
            CONNECTION.commit();
        } catch (Exception ex) {
            try {
                CONNECTION.rollback();
                System.out.println("removeOrder()发生错误，操作已回滚。");
            } catch (Exception ex1) {
                System.out.println("removeOrder()发生错误，操作回滚失败。");
            }
            closeSqlConnect();
            throw new SQLNonTransientConnectionException();
        }
    }

    /**
     * 将商品按价格排序（降序）的结果输出（不改变数据库内的结果）
     * @throws SQLException
     */
    public static void sortGoodsByPrice() throws SQLException {
        try {
            CONNECTION.setAutoCommit(false);
            COMMAND = CONNECTION.prepareStatement("select `id`,`name`,`price` from `goodslist` order by `price` desc");
            ResultSet resultSet = COMMAND.executeQuery();
            if (resultSet.next()) {
                System.out.println("排序结果如下：");
                do {
                    System.out.printf("%d %s %.2f元\n", resultSet.getInt(1), resultSet.getString(2), resultSet.getDouble(3));
                } while (resultSet.next());
            } else {
                System.out.println("仓库可空了，快去进点货吧:-)");
            }
            CONNECTION.commit();
        } catch (Exception ex) {
            try {
                CONNECTION.rollback();
                System.out.println("sortGoodsByPrice()发生错误，操作已回滚。");
            } catch (Exception ex1) {
                System.out.println("sortGoodsByPrice()发生错误，操作回滚失败。");
            }
            closeSqlConnect();
            throw new SQLNonTransientConnectionException();
        }
    }

    /**
     * 将订单按价格排序（降序）的结果输出（不改变数据库内的结果）
     * @throws SQLException
     */
    public static void sortOrdersByPrice() throws SQLException {
        try {
            CONNECTION.setAutoCommit(false);
            COMMAND = CONNECTION.prepareStatement("select `id`,`information`,`time`,`price` from `orderslist` order by `price` desc");
            ResultSet resultSet = COMMAND.executeQuery();
            Gson gson = new Gson();
            Map goodslist;
            if (resultSet.next()) {
                System.out.println("排序结果如下：");
                do {
                    System.out.printf("编号：%d\n采购清单：", resultSet.getInt(1));
                    goodslist = gson.fromJson(resultSet.getString(2), new TypeToken<>() {
                    }.getType());
                    for (Object entry : goodslist.keySet()) {
                        System.out.printf("%s %d件\n", entry.toString(), (int) Double.parseDouble(goodslist.get(entry).toString()));
                    }
                    System.out.printf("提交时间：%s\n总价：%.2f元\n\n", resultSet.getString(3), resultSet.getDouble(4));
                } while (resultSet.next());
            } else {
                System.out.println("生意惨淡，订单空空如也~_~");
            }
            CONNECTION.commit();
        } catch (Exception ex) {
            try {
                CONNECTION.rollback();
                System.out.println("sortOrdersByPrice()发生错误，操作已回滚。");
            } catch (Exception ex1) {
                System.out.println("sortOrdersByPrice()发生错误，操作回滚失败。");
            }
            closeSqlConnect();
            throw new SQLNonTransientConnectionException();
        }
    }

    /**
     * 将订单按提交时间排序（由近到远）的结果输出（不改变数据库内的结果）
     * @throws SQLException
     */
    public static void sortOrdersBySubmitTime() throws SQLException {
        try {
            CONNECTION.setAutoCommit(false);
            COMMAND = CONNECTION.prepareStatement("select `id`,`information`,`time`,`price` from `orderslist` order by `time` desc");
            ResultSet resultSet = COMMAND.executeQuery();
            Gson gson = new Gson();
            Map goodslist;
            if (resultSet.next()) {
                System.out.println("排序结果如下：");
                do {
                    System.out.printf("编号：%d\n采购清单：\n", resultSet.getInt(1));
                    goodslist = gson.fromJson(resultSet.getString(2), new TypeToken<>() {
                    }.getType());
                    for (Object entry : goodslist.keySet()) {
                        System.out.printf("%s %d件\n", entry.toString(), (int) Double.parseDouble(goodslist.get(entry).toString()));
                    }
                    System.out.printf("提交时间：%s\n总价：%.2f元\n\n", resultSet.getString(3), resultSet.getDouble(4));
                } while (resultSet.next());
            } else {
                System.out.println("生意惨淡，订单空空如也~_~");
            }
            CONNECTION.commit();
        } catch (Exception ex) {
            try {
                CONNECTION.rollback();
                System.out.println("sortOrdersBySubmitTime()发生错误，操作已回滚。");
            } catch (Exception ex1) {
                System.out.println("sortOrdersBySubmitTime()发生错误，操作回滚失败。");
            }
            closeSqlConnect();
            throw new SQLNonTransientConnectionException();
        }
    }

    /**
     * 关闭与MySQL的连接，相关资源的清理
     * @throws SQLException
     */
    public static void closeSqlConnect() throws SQLException {
        try {
            RESULTSET.close();
            COMMAND.close();
            CONNECTION.close();
        } catch (Exception ex) {
            try {
                CONNECTION.rollback();
                System.out.println("closeSqlConnect()发生错误，操作已回滚。");
            } catch (Exception ex1) {
                System.out.println("closeSqlConnect()发生错误，操作回滚失败。");
            }
        }
    }
}