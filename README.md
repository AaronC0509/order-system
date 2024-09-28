# **Order Management System**

说明：
1. 订单表（id,订单编号，用户ID，商品编码，数据量，金额，总金额），商品表（id,商品编码，商品名，库存数量）
2. 实现一个下单接口（这里会有很多高并发订单）
3. 订单支付接口
4. 库存不能超卖
5. 查询订单信息
6. 30分钟没付款就自动关闭交易。

要求：
1. redis 缓存商品信息
2. rocketmq 订单交易成功，交易失败，发送消息通知

## **Table of Contents**

- [Prerequisites](#prerequisites)
- [API Endpoints](#api-endpoints)
- [Configuration](#configuration)

## **Prerequisites**

Before running the application, ensure you have the following installed:

- **Java 17+**
- **Spring Boot 3.3.4**
- **MySQL**
- **Redis**
- **RocketMQ**
- **Maven**

## **API Endpoints**

1. Place an order
   - Endpoint: /orders/pay
   - Method: POST
   - Description: Place a new order for a product. It support high concurrency orders at the same time.
```json
{
	"userId": 1,
	"productCode": "P001",
	"quantity": 5
}
```

2. Pay for an Order
   - Endpoint: /orders/pay
   - Method: POST
   - Description: Pay for an existing order.
```json
{
	"orderNumber": "your-order-number"
}
```

3. Get Order Details
   - Endpoint: /orders/{orderNumber}
   - Method: GET
   - Description: Retrieve details of a specific order.

## **Configuration**
1) Make sure you are running redis-server on your machine. If your port is different than 6379, please change it on **application.properties**.
2) Make sure you are running RocketMQ client on your machine. For more information, please visit to this link (https://github.com/apache/rocketmq). Once you configured it on your machine, please change the configuration on **application.properties**.
3) Make sure you are having MySQL installed on your machine. 
4) Before running the application, you can execute the script below to have two products to test.

SQL Script
```sql
INSERT INTO products (price, product_code, product_name, stock_quantity) VALUES(50.00, 'P001', 'Mouse', 19);
INSERT INTO products (price, product_code, product_name, stock_quantity) VALUES(30.00, 'P002', 'Keyboard', 200);
```
