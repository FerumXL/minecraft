# Установка

## Последняя версия

[Скачать mc112bridge-0.29.0.jar](https://raw.githubusercontent.com/FerumXL/minecraft/codex/offhand1710-bootstrap/releases/0.29.0/mc112bridge-0.29.0.jar)

Что изменилось: [CHANGELOG.md](CHANGELOG.md).

Для Forge **1.7.10-10.13.4.1614**. Закрой игру, помести JAR в папку `.minecraft/mods` профиля Forge и запусти Minecraft. В папке mods должен быть только один файл `mc112bridge-*.jar`. С версии 0.6.0 левая рука работает как в 1.12.2: слот в инвентаре, F меняет предметы в руках, ПКМ использует левую руку, если основная ничего не сделала. В 0.5.0 были клавиши G (обмен) и V (использование).

Сборка из исходников: сначала `python tools/fetch-assets.py` (восстанавливает 984 файла Mojang из установленного 1.12.2, по умолчанию из `%APPDATA%\.minecraft`), затем `gradlew.bat build --offline` (Java 8, ForgeGradle 1.2). Для игры нужен `build/release/mc112bridge-<версия>.jar`, а не `build/libs`. Без шага с fetch-assets не будет достижений, книги рецептов и части текстур.

## Архив тестовых версий

| Версия | Скачать | Состав |
|---|---|---|
| 0.22.0 | [mc112bridge-0.22.0.jar](https://raw.githubusercontent.com/FerumXL/minecraft/codex/offhand1710-bootstrap/releases/0.22.0/mc112bridge-0.22.0.jar) | Лодки, баннеры, слизь, наблюдатель, чары, торговля 1.8 |
| 0.18.0 | [mc112bridge-0.18.0.jar](https://raw.githubusercontent.com/FerumXL/minecraft/codex/offhand1710-bootstrap/releases/0.18.0/mc112bridge-0.18.0.jar) | Блоки и предметы: свёкла, ящики шалкера, стойки, кровати |
| 0.13.0 | [mc112bridge-0.13.0.jar](https://raw.githubusercontent.com/FerumXL/minecraft/codex/offhand1710-bootstrap/releases/0.13.0/mc112bridge-0.13.0.jar) | Океанские монументы и стражи |
| 0.12.0 | [mc112bridge-0.12.0.jar](https://raw.githubusercontent.com/FerumXL/minecraft/codex/offhand1710-bootstrap/releases/0.12.0/mc112bridge-0.12.0.jar) | Иглу, потолок элитр, исправление вылета при генерации |
| 0.11.0 | [mc112bridge-0.11.0.jar](https://raw.githubusercontent.com/FerumXL/minecraft/codex/offhand1710-bootstrap/releases/0.11.0/mc112bridge-0.11.0.jar) | Элитры и разгон ракетами. Есть вылет при генерации чанков, исправлен в 0.12.0 |
| 0.10.0 | [mc112bridge-0.10.0.jar](https://raw.githubusercontent.com/FerumXL/minecraft/codex/offhand1710-bootstrap/releases/0.10.0/mc112bridge-0.10.0.jar) | Бой 1.9–1.12.2: перезарядка атаки, удар с размаха, значения оружия |
| 0.9.0 | [mc112bridge-0.9.0.jar](https://raw.githubusercontent.com/FerumXL/minecraft/codex/offhand1710-bootstrap/releases/0.9.0/mc112bridge-0.9.0.jar) | Щит и палитра цветов 1.12.2 |
| 0.8.0 | [mc112bridge-0.8.0.jar](https://raw.githubusercontent.com/FerumXL/minecraft/codex/offhand1710-bootstrap/releases/0.8.0/mc112bridge-0.8.0.jar) | Долгое использование левой рукой: еда, зелья, лук |
| 0.7.0 | [mc112bridge-0.7.0.jar](https://raw.githubusercontent.com/FerumXL/minecraft/codex/offhand1710-bootstrap/releases/0.7.0/mc112bridge-0.7.0.jar) | Камни, призмарин, двери, заборы, песчаник, пурпур, тропа, баранина; исправлены ранние блоки |
| 0.6.0 | [mc112bridge-0.6.0.jar](https://raw.githubusercontent.com/FerumXL/minecraft/codex/offhand1710-bootstrap/releases/0.6.0/mc112bridge-0.6.0.jar) | Левая рука как в 1.12.2 |
| 0.5.0 | [mc112bridge-0.5.0.jar](https://raw.githubusercontent.com/FerumXL/minecraft/codex/offhand1710-bootstrap/releases/0.5.0/mc112bridge-0.5.0.jar) | Слот левой руки (клавиши G и V), текстура самородка |
| 0.4.0 | [mc112bridge-0.4.0.jar](https://raw.githubusercontent.com/FerumXL/minecraft/codex/offhand1710-bootstrap/releases/0.4.0/mc112bridge-0.4.0.jar) | Магмовый блок |
| 0.3.0 | [mc112bridge-0.3.0.jar](https://raw.githubusercontent.com/FerumXL/minecraft/codex/offhand1710-bootstrap/releases/0.3.0/mc112bridge-0.3.0.jar) | Глазурованная керамика |
| 0.2.1 | [mc112bridge-0.2.1.jar](https://raw.githubusercontent.com/FerumXL/minecraft/codex/offhand1710-bootstrap/releases/0.2.1/mc112bridge-0.2.1.jar) | Исправлены текстуры бетона |
| 0.2.0 | [mc112bridge-0.2.0.jar](https://raw.githubusercontent.com/FerumXL/minecraft/codex/offhand1710-bootstrap/releases/0.2.0/mc112bridge-0.2.0.jar) | Бетон, порошок и железный самородок |

При переходе между версиями убери предыдущий JAR из `mods`, чтобы Forge не загрузил две копии мода.

Исходный код и история проекта: [репозиторий](https://github.com/FerumXL/minecraft/tree/codex/offhand1710-bootstrap).
