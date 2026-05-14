# СвітлоЄ? (SvitloYe-ZT) 💡❓
Неофіційний Android-застосунок для відстеження графіків відключень електроенергії в Житомирській області.

## 📱 Про додаток
"СвітлоЄ?" — це ваш персональний помічник для моніторингу енергосистеми Житомирської області. Дізнавайтеся першими, чи є світло за вашою адресою.

**Навіщо цей застосунок?**
* 📍 **Зручний пошук** за адресою та чергою.
* 🔔 **Миттєві сповіщення** про зміну графіків або екстрені відключення.
* 🕒 **Таймер** до наступного ввімкнення/вимкнення.
* 🗺️ **Охоплення всієї області** (не тільки Житомир).

> **Важливо:** Це неофіційний застосунок, створений спільнотою для зручності мешканців. Дані беруться з відкритих джерел АТ "Житомиробленерго".

## 🛠️ Технічні деталі
* **Мова:** Kotlin
* **UI:** Jetpack Compose (Material 3)
* **Архітектура:** Clean Architecture + MVVM
* **Мережа:** Retrofit
* **Фон:** WorkManager, Foreground Services
* **Min SDK:** 26 (Android 8.0)

## 📥 Завантаження
Актуальну версію можна завантажити у розділі [Releases](https://github.com/dmitthedazed/svitlo-ye-zhytomyr/releases).

---

## 🛠 TODO: Налаштування App Links (GitHub Pages)
Для того, щоб посилання `https://dmitthedazed.github.io/svitlo-ye-zhytomyr/` автоматично відкривали додаток, необхідно виконати наступні кроки:

1. **Отримати SHA-256**: Виконайте `./gradlew signingReport` у терміналі та скопіюйте SHA-256 відбитки для `debug` та `release` ключів.
2. **Оновити assetlinks.json**: Вставте отримані відбитки у файл `gh-pages-content/.well-known/assetlinks.json`.
3. **Створити гілку gh-pages**:
   ```bash
   git checkout --orphan gh-pages
   git rm -rf .
   # Скопіюйте вміст папки gh-pages-content у корінь гілки
   git add .
   git commit -m "docs: add app links verification and redirector"
   git push origin gh-pages
   ```
4. **Увімкнути GitHub Pages**: В налаштуваннях репозиторію (Settings -> Pages) оберіть гілку `gh-pages`.
5. **Верифікація**: Перевірте статус посилань у Google Play Console або через `adb shell am start -a android.intent.action.VIEW -d "https://dmitthedazed.github.io/svitlo-ye-zhytomyr/schedule?streetId=1&houseId=1"`.

---
*Розроблено з турботою про енергонезалежність.* 🇺🇦
