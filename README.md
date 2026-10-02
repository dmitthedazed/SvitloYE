<p align="center">
  <img src="docs/banner.png" alt="СвітлоЄ? — графік відключень світла Житомирщини" width="100%">
</p>

<p align="center">
  <a href="https://github.com/dmitthedazed/SvitloYE/releases/latest"><img src="https://img.shields.io/github/v/release/dmitthedazed/SvitloYE?style=flat-square&color=FFB800&label=%D0%B2%D0%B5%D1%80%D1%81%D1%96%D1%8F" alt="Release"></a>
  <a href="https://github.com/dmitthedazed/SvitloYE/releases"><img src="https://img.shields.io/github/downloads/dmitthedazed/SvitloYE/total?style=flat-square&color=1A4FB8&label=%D0%B7%D0%B0%D0%B2%D0%B0%D0%BD%D1%82%D0%B0%D0%B6%D0%B5%D0%BD%D1%8C" alt="Downloads"></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android 8.0+">
  <img src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin + Compose">
  <img src="https://img.shields.io/badge/Material%203-Expressive-6750A4?style=flat-square&logo=materialdesign&logoColor=white" alt="Material 3">
  <img src="https://img.shields.io/badge/%D1%80%D0%B5%D0%BA%D0%BB%D0%B0%D0%BC%D0%B0-%D0%BD%D0%B5%D0%BC%D0%B0%D1%94-success?style=flat-square" alt="Без реклами">
  <a href="LICENSE"><img src="https://img.shields.io/github/license/dmitthedazed/SvitloYE?style=flat-square&color=lightgrey" alt="License"></a>
</p>

<p align="center">
  <b>Відкрив — і одразу бачиш, чи є світло вдома, коли його вимкнуть і коли повернуть.</b><br>
  Неофіційний Android-застосунок для графіків відключень АТ «Житомиробленерго» по всій області.
</p>

<p align="center">
  <a href="https://github.com/dmitthedazed/SvitloYE/releases/latest"><b>⬇️ Завантажити APK</b></a> ·
  <a href="https://dmitthedazed.github.io/SvitloYE/">🌐 Сайт</a> ·
  <a href="https://github.com/dmitthedazed/SvitloYE/issues">🐞 Повідомити про проблему</a>
</p>

---

## ✨ Що вміє

<table>
  <tr>
    <td align="center" width="25%"><img src="docs/screenshots/home.png" alt="Головна" width="220"></td>
    <td align="center" width="25%"><img src="docs/screenshots/bell.png" alt="Повідомлення" width="220"></td>
    <td align="center" width="25%"><img src="docs/screenshots/addr.png" alt="Мої адреси" width="220"></td>
    <td align="center" width="25%"><img src="docs/screenshots/more.png" alt="Додатково" width="220"></td>
  </tr>
  <tr>
    <td valign="top"><b>💡 Статус зараз</b><br><sub>Велика картка «Світло є / немає», час до наступної зміни та підсумок дня для вашої черги.</sub></td>
    <td valign="top"><b>📢 Офіційні новини</b><br><sub>Оголошення Житомиробленерго прямо в застосунку — з посиланням на першоджерело.</sub></td>
    <td valign="top"><b>📍 Кілька адрес</b><br><sub>Дім, робота, батьки. Пошук за адресою, геолокацією або QR-кодом, перетягування та свайп для видалення.</sub></td>
    <td valign="top"><b>📊 Статистика дня</b><br><sub>Скільки годин зі світлом і без, швидкий доступ до налаштувань і сервісів ZTOE.</sub></td>
  </tr>
</table>

### А ще

- 🔔 **Розумні сповіщення** — попередження перед вимкненням і ввімкненням, сповіщення про зміну графіка, постійний статус у шторці.
- 🧩 **Віджети** — компактний статус, картка з поточним станом і детальний графік на день.
- ⚡ **Плитка швидких налаштувань** — статус світла в один свайп.
- 🚗 **Android Auto** — графік на екрані автомобіля.
- 🎨 **Material You** — динамічні кольори з шпалер, світла й темна теми.
- 🗺️ **Вся область** — не лише Житомир: Бердичів, Коростень, Звягель і всі населені пункти ZTOE.

## 🆕 Що нового у 2.0

- Повністю переписана система сповіщень: точні будильники, детектор змін графіка, менше дублів.
- Нові віджети на Glance з адаптивним дизайном.
- Оновлений інтерфейс на Material 3 Expressive, нова навігація та новий логотип.
- Анімований splash-екран і переосмислений онбординг.
- Коректна робота з часовим поясом Києва незалежно від налаштувань телефону.

## 📥 Встановлення

1. Завантажте `app-debug.apk` з останнього **[релізу](https://github.com/dmitthedazed/SvitloYE/releases/latest)**.
2. Дозвольте встановлення з невідомих джерел, якщо Android попросить.
3. Відкрийте застосунок, оберіть адресу — готово.

> **Важливо:** це неофіційний застосунок, створений для зручності мешканців. Дані беруться з відкритих джерел АТ «Житомиробленерго». У разі розбіжностей орієнтуйтеся на [ztoe.com.ua](https://www.ztoe.com.ua).

## 🛠️ Під капотом

| | |
|---|---|
| **Мова** | Kotlin |
| **UI** | Jetpack Compose · Material 3 Expressive |
| **Архітектура** | Clean Architecture + MVVM |
| **Мережа** | Retrofit |
| **Фонова робота** | AlarmManager · WorkManager · Foreground Service |
| **Віджети** | Jetpack Glance |
| **Сканер QR** | CameraX + ML Kit |
| **Авто** | Android for Cars App Library |
| **Min SDK** | 26 (Android 8.0) |

### Збірка

```bash
./gradlew assembleDebug
```

Потрібен JDK 21.

---

<p align="center"><sub>Розроблено з турботою про енергонезалежність 🇺🇦</sub></p>
