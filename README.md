# Hunger Sucks

Forge **1.20.1** (Forge 47.4.10) порт мода
[Hearty Meals](https://github.com/MoriyaShiine/hearty-meals) от MoriyaShiine.
Убирает систему голода — еда лечит вас напрямую и постепенно. Первый кирпичик
более крупного мода на реворк еды.

- Разработчик: **greenmint20**
- modid: `hungersucks`
- Лицензия исходного мода: All Rights Reserved (это порт; уважайте лицензию автора)

## Документация
- **[FEATURES.md](FEATURES.md)** — полное описание механик и настроек для игрока.
- **[DEVELOPMENT.md](DEVELOPMENT.md)** — журнал разработки и уроки (Fabric→Forge,
  1.20.6→1.20.1). Веди его при каждой правке.
- **[TODO.md](TODO.md)** — список задач.

## Сборка
```
gradlew build
```
Готовый jar: `build/libs/hungersucks-<version>.jar`. Нужен JDK 17 — путь к нему
задан в `gradle.properties` (`org.gradle.java.home`); поправьте под свою систему.

## Запуск в dev
```
gradlew runClient
```
