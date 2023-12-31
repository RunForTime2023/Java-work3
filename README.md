# 项目结构

项目构建系统为常规的Intellij。目录树如下：

```
├─.idea                                           // 应该是IDEA自身的配置文件夹（不知道有没有用）
│  ├─dataSources
│  │  └─f4bcde30-d9a2-4413-af95-c4826347fec1
│  │      └─storage_v2
│  │          └─_src_
│  │              └─schema
│  └─libraries
├─lib
│  ├─gson-2.10.1.jar                              // 用于 json 与内置类型的转换
│  └─mysql-connector-j-8.0.33.jar                 // JDBC 连接库
├─out                                             // 输出链接文件（应该没用）
│  └─production
│      └─Java work3                               
├─src
│  ├─Goods                                        // 商品类
│  ├─Order                                        // 订单类
│  ├─JDBCUtil                                     // 数据库操作类
│  └─Test                                         // 测试文件，包含测试数据
└─Java work3.iml                                  // IntelliJ 项目配置文件
```

（说明：程序应当从Test.java编译、启动。项目所依赖的库放在lib文件夹中，并已成功在项目中添加为库。）

# 实现功能

~~省流：其实就是说要啥我就写啥。~~

项目实现了一个简易的订单管理系统。信息全部存储在ordermanagesys数据库中，其中商品信息存储在goodslist表中，订单信息存储在orderslist表中。

该系统在对数据库中的数据执行”增删查改“前，会进行信息验证：

- 添加商品信息时会核查商品价格是否为负，然后在数据库中查找是否有同名商品

- 更新商品信息时会核查商品价格是否为负，然后在数据库中查找是否有相同编号商品，若未找到则返回错误信息（而不是帮忙添加进数据库）；否则更新信息，同时更新订单信息（包括采购清单中该商品的名字，订单的总金额）。

- 删除商品信息时会查找是否有同名商品。删除指定商品的信息时，会将采购清单中包含该商品的订单信息全部删除。

- 添加订单信息时会查找是否有相同ID的订单。

- 更新/移除订单信息时会查找是否有指定ID的订单。

该系统支持用户仅提供商品名称来查询、移除对应商品信息，还支持显示按商品价格排序的商品列表、按提交时间/订单总额排序的订单列表。

# 部分特性

为避免SQL注入风险，有关SQL语句全部通过PreparedStatement执行。

为防止处理数据时出现意外而导致数据错误，所有方法中均包含异常处理与事务管理。当且仅当全流程完成时，才会提交事务。当出现异常时，会进行操作回滚，然后关闭数据库。

JDBCUtil类中提供了连接SQL和关闭SQL的静态方法。连接MySQL时，会要求用户输入本机MySQL的root用户的密码。

# 缺陷

若用户输入密码错误会发生异常。

显示排序结果时，不会改变原数据库中的信息排列情况。数据库中的数据仍然显得无序。

限于能力，该系统仅支持一次”增删查改“一种商品或一份订单。

由于使用printStackTrace()输出的错误信息会在程序全部终止后才输出，与预期的“终止程序前输出”不符，故发生错误时将不输出详细错误信息。

# 
