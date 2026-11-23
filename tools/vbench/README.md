# Стенд сравнения скриншотов с 1.12.2

1. `python make_scenes.py` пишет `scenes.txt`, общий сценарий из 13 сцен.
2. `HarnessScene.java` компилируется против `client-unsigned.jar` 1.12.2. Подписанный `1.12.2.jar` не пускает класс без пакета.
3. `run112.ps1 -Work <папка> -H112 <папка с client-unsigned.jar и cp.txt>` снимает эталон.
4. `runmod.ps1 -Work <папка> -Base <папка с real-cp.txt> [-NoOptiFine] -Tag mod|modof` снимает мод через стадию `visual` (переменная `MC112_SCENE`).
5. `diff.py`, запущенный из `<Work>`, строит `report/*.png` и `metrics.json`.

Файлы Mojang, снимки и миры в репозиторий не кладутся.
