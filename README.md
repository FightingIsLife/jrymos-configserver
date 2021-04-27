## 在spring cloud configserver的基础上，以文件的配置方式的配置中心实现

## 使用介绍：

#### 服务端配置方式

application设置了文件路径：
`spring.cloud.config.extension.bathPath=classpath:/config/extension`

sky项目,beta环境：
resources/config/extension/sky/beta/product/drinks.yml

```
sprite:
  name: 雪碧
  price: 2
qingDaoBeer:
  name: 青岛啤酒
  price: 4.5
```

#### 客户端使用方式
```
@Configuration
@ConfigurationProperties("product.drinks") // product映射product目录，drinks映射drinks文件名称
@Data
public class DrinksProperties {
   private Product sprite; //对应配置中的sprite {"name":"雪碧","price":2}
   private Product qingDaoBeer; //对应配置中的qingDaoBeer {"name":"青岛啤酒","price":4.5}
}

@Data
public class Product {
   private String name;
   private double price;
}
```

#### 服务端配置开发流程，以drinks.yml为例
###### 1. 在服务端配置一个文件resources/config/extension/sky/beta/product/drinks.yml
```
sprite:
  name: 雪碧
  price: 2
qingDaoBeer:
  name: 青岛啤酒
  price: 4.5
```


###### 2. 启动运行服务端本地程序TestApplication，访问http://localhost:8888/sky-beta.json 或者 http://localhost:8888/sky/beta

验证配置是否正确
```
{
    "product":{
        "drinks":{
            "sprite":{
                "name":"雪碧",
                "price":2
            },
            "mengniu":{
                "price":"3.5",
                "name":""蒙牛""
            },
            "QingDaoBeer":{
                "name":"青岛啤酒",
                "price":4.5
            }
        }
    }
}
```

_提示：上面发现多了一个mengniu的配置，这是common里面也配置drinks（resources/config/extension/sky/common/product/drinks.yml）导致，
逻辑是：先从common里面查找drinks文件, 再从beta里面找drinks，把drinks里面的配置覆盖common同名的配置。
这样设计的目的是因为不同环境极大多数配置最终和common会一致，使用common能尽量减少配置，而且避免由于其他环境缺少必须的配置而需要反复修改配置文件_

###### 3. 提交配置review， arc diff
###### 4. 配置审核通过后，push代码，部署configserver服务，大约30s就能部署完成
