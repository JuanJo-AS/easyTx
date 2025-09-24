# What is easyTx

easyTx is a Java library designed to make the use of transactions and multiple data sources easier, more readable, manageable, and declarative.

It is based on Spring Framework JDBC and Spring Transactions, and it offers the following features:
- Easy dataSource, JdbcTemplate, RoutingDatasource, EntityManager and TransactionTemplate schema configuration.
- Declarative syntax to manage a primary database and a replicas (with the posibility of more dataSource).
  - Using annotations.
  - Using the TransanctionService.
- Micrometer integration.
  - Number of executions grouped by the function name.
  - Success/failure transactions identification.
  - Read/Write transaction identification.
  - Transaction time metrics.
- Loggs
  - Start and end events, execution time, or both.

*Using this library doesn't interfeer with the tradictional usages of Spring Framework JDBC and Spring Transactions*

# Why should I use easyTx ?

## 1. Declarative syntax

The goal is to make database operations clearer for developers, especially regarding which data source is being used for which operation.

In other words: it makes it easier to decide whether to use the master, a replica, or a transaction for different operations.

Common usage of transactions (manually configured without easyTx):
```java
// Read Transaction
public User findUser(Long id) {
  return transactionService.withinTransaction(Boolean.TRUE, () -> userDao.findUser(id));
}
// Write Transaction
public User findUser(UserDto userDto) {
  return transactionService.withinTransaction(Boolean.FALSE, () -> userDao.createUser(userDto));
}
```
easyTx aproachment:
- annotated:
```java
// Read Transaction
@TxRead
public User findUser(Long id) {
  return userDao.findUser(id);
}
// Write Transaction
@TxWrite
public User findUser(UserDto userDto) {
  return userDao.createUser(userDto);
}
```
- inline (automatically configured, ready to go):
```java
// Read Transaction
public User findUser(Long id) {
  return transactionService.read(() -> userDao.findUser(id));
}
// Write Transaction
public User findUser(UserDto userDto) {
  return transactionService.write(() -> userDao.createUser(userDto));
}
```
## 2. Ready to go

You only need to configure your DataSource beans, and the library provides the full JDBC and transaction environment out of the box.

## 3. Logging and metrics

This library allows you to add logs and gather metrics about your transactions.

## 4. Spring Framework compatibily

Using this library doesn't create conflicts with the standard usages of Spring Framework JDBC and Spring Transactions.

# Status

Proyect in early development. Contributions and feedback are welcome!

Currently working: 
- Supporting all three approaches (annotations transactional, non-transactional, and transactionService methods) for all features.
- Creating a side project with examples using this library..
- Expanding the documentation.
- Providing more configuration options.
- Supporting single DataSource setups.

This project was originally focused on transaction but I wanted to add dataSource routing, that’s why the transaction side is currently more feature-rich.

# Configuration

## Tx read and write dataSources

Create two javax.sql.DataSource beans qualified as txWriteSource and txReadSource.

# Basic usage

## Annotations

For transactions anotate your methods with:
```java
@TxRead
public User findUser(Long id) { ... }

@TxWrite
public void saveUser(User user) { ... }
```
For routing (switching dataSources without transactions):
```java
@Read
public User findUser(Long id) { ... }

@Write
public void saveUser(User user) { ... }
```

## Inline

Transactions:
```java
transactionService.read(() -> ...);
transactionService.write(() -> ...);
```
