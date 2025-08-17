# TxLens

TxLens is a light Java library for declarative transaction management using annotations, with support for read and write transactions, and multi-datasource.

## Status

Proyect in early development. Contributions and feedback are welcome!

## Basic usage

Configure your main read and write datasources implementing `TxLensConfig`.

Anotate your methods with:

```java
@TxRead
public User findUser(Long id) { ... }

@TxWrite
public void saveUser(User user) { ... }
