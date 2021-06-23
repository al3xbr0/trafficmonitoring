# Traffic Monitoring
## Подготовка
Для подготовки базы данных нужно выполнить два sql-скрипта, которые находятся в директории [src/main/resources/database](src/main/resources/database). Для этого можно использовать консольный клиент psql.
```shell
postgres@Desktop:~$ psql
```
Скрипты запускаются командой `\i FILE`:
```
postgres=# \i postgresql-schema.sql
postgres=# \i postgresql-dataload.sql
```
## Работа с приложением
Клонировать репозиторий. Выполнить
```shell
user@Desktop:~/trafficmonitoring$ gradlew shadowJar
```
Затем запустить приложение:
```shell
user@Desktop:~/trafficmonitoring/build/libs$ java -jar trafficmonitoring-1.0-SNAPSHOT-all.jar config.properties
```
Возникновение ошибок, связанных с Pcap4J, говорит о том, что приложению [требуются root-права](https://github.com/kaitoy/pcap4j#others).

В качестве аргумента необходимо передавать properties-файл, содержащий параметры приложения. [Пример](config.properties) такого файла, составленный по требованиям исходного задания.
