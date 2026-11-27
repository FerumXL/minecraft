# sidescan

Ищет в общих (не `client/`) классах мода вызовы того, чего нет на выделенном сервере, — методов и классов с `@SideOnly(Side.CLIENT)` из jar Forge.

```
javac -cp asm-debug-all-5.0.3.jar -d out SideScan.java
java -cp asm-debug-all-5.0.3.jar;out SideScan forgeBin-1.7.10-10.13.4.1614-1.7.10.jar build/classes/main ru/codex/offhand1710/client/
```

Оба jar лежат в кэше Gradle (`~/.gradle/caches`). Что осталось в выводе, нужно проверить вручную: вызывается ли это место только на клиенте.
