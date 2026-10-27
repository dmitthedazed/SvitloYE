<p align="center">
  <img src="docs/logo.svg" alt="СвітлоЄ?" width="128">
</p>

<h1 align="center">СвітлоЄ?</h1>

<p align="center">
  <b>Відкрив і одразу бачиш, чи є світло вдома, коли його вимкнуть і коли повернуть.</b><br>
  Графіки відключень Житомиробленерго для всієї області.
</p>

<p align="center">
  <a href="https://github.com/dmitthedazed/SvitloYE/releases/latest"><img src="https://img.shields.io/github/v/release/dmitthedazed/SvitloYE?style=flat-square&color=FFB800&label=%D0%B2%D0%B5%D1%80%D1%81%D1%96%D1%8F" alt="Версія"></a>
  <a href="https://github.com/dmitthedazed/SvitloYE/releases"><img src="https://img.shields.io/github/downloads/dmitthedazed/SvitloYE/total?style=flat-square&color=1A4FB8&label=%D0%B7%D0%B0%D0%B2%D0%B0%D0%BD%D1%82%D0%B0%D0%B6%D0%B5%D0%BD%D1%8C" alt="Завантаження"></a>
  <a href="https://github.com/dmitthedazed/SvitloYE/actions/workflows/tests.yml"><img src="https://img.shields.io/github/actions/workflow/status/dmitthedazed/SvitloYE/tests.yml?style=flat-square&label=%D1%82%D0%B5%D1%81%D1%82%D0%B8" alt="Тести"></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android 8.0+">
  <img src="https://img.shields.io/badge/%D1%80%D0%B5%D0%BA%D0%BB%D0%B0%D0%BC%D0%B8-%D0%BD%D0%B5%D0%BC%D0%B0%D1%94-success?style=flat-square" alt="Без реклами">
</p>

<p align="center">
  <a href="https://github.com/dmitthedazed/SvitloYE/releases/latest"><b>Завантажити APK</b></a> ·
  <a href="https://dmitthedazed.github.io/SvitloYE/">Сайт</a> ·
  <a href="https://github.com/dmitthedazed/SvitloYE/issues/new/choose">Повідомити про проблему</a>
</p>

<table>
  <tr>
    <td align="center" width="25%"><img src="docs/screenshots/home.png" alt="Головна" width="220"></td>
    <td align="center" width="25%"><img src="docs/screenshots/bell.png" alt="Новини" width="220"></td>
    <td align="center" width="25%"><img src="docs/screenshots/addr.png" alt="Мої адреси" width="220"></td>
    <td align="center" width="25%"><img src="docs/screenshots/more.png" alt="Додатково" width="220"></td>
  </tr>
  <tr>
    <td valign="top"><b>Статус зараз</b><br><sub>Є світло чи ні, скільки до наступної зміни і підсумок дня для вашої черги.</sub></td>
    <td valign="top"><b>Новини</b><br><sub>Оголошення Житомиробленерго з посиланням на джерело.</sub></td>
    <td valign="top"><b>Кілька адрес</b><br><sub>Дім, робота, батьки. Пошук за адресою, геолокацією або QR-кодом.</sub></td>
    <td valign="top"><b>Статистика</b><br><sub>Скільки годин зі світлом і без, сервіси Житомиробленерго під рукою.</sub></td>
  </tr>
</table>

## Що вміє

- **Сповіщення** перед вимкненням і ввімкненням, про зміну графіка, постійний статус у шторці
- **Віджети**: компактний статус, поточний стан і графік на день
- **Плитка** у швидких налаштуваннях
- **Android Auto**: графік на екрані авто
- **Material You**: кольори зі шпалер, світла й темна теми
- **Вся область**: Житомир, Бердичів, Коростень, Звягель і всі населені пункти, які обслуговує Житомиробленерго

## Встановлення

1. Завантажте `app-debug.apk` з [останнього релізу](https://github.com/dmitthedazed/SvitloYE/releases/latest).
2. Дозвольте встановлення з невідомих джерел, якщо Android попросить.
3. Відкрийте застосунок і оберіть адресу.

> [!NOTE]
> Застосунок неофіційний. Дані беруться з відкритих джерел АТ «Житомиробленерго». Якщо щось не збігається, орієнтуйтеся на [ztoe.com.ua](https://www.ztoe.com.ua).

<details>
<summary>Що нового у 2.0</summary>

- Нова система сповіщень: точні будильники, детектор змін графіка, менше дублів
- Нові віджети з адаптивним дизайном
- Material 3 Expressive, нова навігація і новий логотип
- Анімований splash-екран і новий онбординг
- Правильний київський час незалежно від налаштувань телефону

</details>

<details>
<summary>Під капотом</summary>

| | |
| --- | --- |
| Мова | Kotlin |
| UI | Jetpack Compose, Material 3 Expressive |
| Архітектура | Clean Architecture, MVVM |
| Мережа | Retrofit |
| Фонова робота | AlarmManager, WorkManager, Foreground Service |
| Віджети | Jetpack Glance |
| QR-сканер | CameraX, ML Kit |
| Авто | Android for Cars App Library |

Збірка (потрібен JDK 21):

```sh
./gradlew assembleDebug
```

Як долучитися: [CONTRIBUTING.md](CONTRIBUTING.md).

</details>

## Ліцензія

[MIT](LICENSE)
