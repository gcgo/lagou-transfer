# 大作业：自定义@Service、@Autowired、@Transactional注解类，完成基于注解的IOC容器（Bean对象创建及依赖注入维护）和声明式事务控制，写到转账工程中，并且可以实现转账成功和转账异常时事务回滚

## 编程题：

自定义注解：

- @Component

  添加在类上，value属性用于设置bean的id，默认值为""

- @Autowired

  添加在属性上，只实现了根据类型注入，即若属性是接口类型，需要该接口的实现类唯一，才可以注入

- @Transactional

  添加在类上，对添加了此注解的类采用动态代理生成代理对象，若该类实现了接口则用jdk动态代理，若没有实现接口，则采用CGlib动态代理。

  工程中，com.lagou.edu.service.impl.TransferServiceCGlib为CGlib动态代理测试类；

  工程中，com.lagou.edu.service.impl.TransferServiceImpl为jdk动态代理测试类。

使用：

在TransferServlet类中如下调用，走的是jdk动态代理

```java
private TransferService transferService = (TransferService) BeanFactoryAnnotation.getBean("transferService");
```

在TransferServlet类中如下调用，走的是CGlib动态代理

```java
private TransferServiceCGlib transferService = (TransferServiceCGlib) BeanFactoryAnnotation.getBean("transferServiceCGlib");
```

配置文件beans.xml只配置一行包扫描即可，框架自动实现包扫描

```xml
<beans>
    <component-scan base-package="com.lagou.edu" />
</beans>
```

数据库字段与课程一致：

```sql
CREATE TABLE `account` (
  `name` varchar(50) DEFAULT NULL,
  `money` int,
  `cardNo` varchar(50) NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
-- 添加数据
INSERT INTO `account` VALUES ('李大雷', 10000,'6029621011000');
INSERT INTO `account` VALUES ('韩梅梅', 10000,'6029621011001');
```

## 循环依赖

[见图片](https://github.com/gcgo/lagou-transfer/blob/master/%E5%BE%AA%E7%8E%AF%E4%BE%9D%E8%B5%96.jpg)