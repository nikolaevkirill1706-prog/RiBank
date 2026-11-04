PiBank — Android Java starter project

Дизайн: тёмный фон, фиолетовые акценты.
Firebase:
- Authentication Email/Password
- Realtime Database: создаёт запись users/<uid> при регистрации

ВАЖНО:
1. google-services.json здесь восстановлен по данным, которые пользователь сообщил ранее. Проверьте в Firebase Console, что Android-приложение имеет package com.pibank.demo и App ID совпадает.
2. В Firebase Console включите Authentication -> Sign-in method -> Email/Password.
3. Создайте Realtime Database и настройте правила доступа. Для теста можно использовать:
   {
     "rules": {
       "users": {
         "$uid": {
           ".read": "auth != null && auth.uid == $uid",
           ".write": "auth != null && auth.uid == $uid"
         }
       }
     }
   }
4. API key — идентификатор проекта, не пароль. Не публикуйте его без необходимости; настройте API restrictions, разрешив Firebase Identity Toolkit API и Firebase Realtime Database API, если требуется.

Сборка:
- Откройте папку PiBank в Android Studio.
- Дождитесь Gradle Sync.
- Выполните: gradle assembleDebug
- APK появится в app/build/outputs/apk/debug/app-debug.apk

Примечание:
Это стартовая версия: регистрация/вход и базовый экран. Магазин, карты и история пока демонстрационные заглушки; реальные платежи не выполняются.


Сборка через GitHub Actions:
1. Создайте новый GitHub repository.
2. Распакуйте этот ZIP и загрузите ВСЕ файлы и папки проекта в корень репозитория (включая скрытую папку .github).
3. Откройте вкладку Actions и разрешите запуск GitHub Actions, если GitHub попросит.
4. Запустите workflow "Build PiBank APK" через Run workflow или сделайте push в ветку main.
5. После успешной сборки откройте запуск workflow и скачайте artifact "PiBank-debug-apk".
