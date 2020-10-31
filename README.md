# Android mobile SDK - UI library & demo app
LiveTex Mobile SDK предоставляет набор инструментов для организации консультирования пользователей мобильных приложений.

В данном репозитории находится готовая к внедрению UI реализация для SDK, а так же демо проект.

Сам SDK находится в репозитории [sdk-android](https://github.com/LiveTex/sdk-android).

**Демо**

Пример использования библиотеки можно посмотреть вживую в полноценном
демо приложении, поставив его на устройство через официальный [Google Play](https://play.google.com/store/apps/details?id=ru.livetex.demoapp).

Исходники этого демо приложения лежат в папке [demo-app](demo-app/).

Подключение UI library
===============
[![Release](https://jitpack.io/v/LiveTex/sdk-ui-android.svg)](https://jitpack.io/#LiveTex/sdk-ui-android)

Пример подключения (с пушами) есть [демо приложении](demo-app/).

В build.gradle (который в корне проекта) добавить репозиторий jitpack

```gradle
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

В build.gradle (который в модуле приложения) добавить зависимость SDK
актуальной версии (см.
[Releases](https://github.com/LiveTex/sdk-ui-android/releases))

```gradle
dependencies {
	implementation 'com.github.LiveTex:sdk-ui-android:x.y'
}
```

Настройка
=========

В AndroidManifest.xml добавьте запись

```java
<activity
			android:name="ru.livetex.sdk.ui.chat.ChatActivity"
			android:theme="@style/Theme.Chat"
			android:screenOrientation="portrait" />
```

В styles.xml добавьте запись вида

```java
	<style name="Theme.Chat" parent="Theme.YourAppTheme">
		<item name="windowActionBar">false</item>
		<item name="windowNoTitle">true</item>
	</style>
```

где Theme.YourAppTheme это тема вашего приложения. **Если она не Material**, сделайте наследование от Theme.MaterialComponents.*.Bridge . В будущем планируем добавить возможности кастомизации.

Далее нужно инициализировать обьект LiveTex.
Делается это обычно в Application классе
([например App.java](demo-app/src/main/java/ru/livetex/demoapp/App.java)).

```java
new LiveTex.Builder(Const.TOUCHPOINT).build();
```

Укажите Touchpoint (берется в личном кабинете).

В итоге для вызова экрана чата нужно запустить активити

```java
startActivity(new Intent(this, ChatActivity.class));
```

**Пуши**

В демо приложении есть пример того, как подключить пуши и передать токен в LiveTex.
Для подключения пушей нужно сначала подключить Firebase Messaging Service по [их стандартной инструкции](https://firebase.google.com/docs/cloud-messaging/android/client).
С помощью функции `FirebaseInstanceId.getInstance().getInstanceId()` нужно зарегистрировать устройство в Firebase и получить в ответ device token, который в свою очередь нужно передать в билдер LiveTex. Это несинхронная операция которая требует какое-то время, поэтому функция реактивная.

**Внимание** - функция `initLiveTex()` должна быть вызвана до использования класса LiveTex. Поэтому инициализировать его в случае с Firebase стоит заранее (в [SplashActivity](/demo-app/src/main/java/ru/livetex/demoapp/ui/splash/SplashActivity.java) например).
Иначе при первой инициализации может быть передан пустой пуш токен, если не критично то можете игнорировать.

```java
public Completable init() {
	return Completable.create(emitter -> {
		FirebaseInstanceId.getInstance().getInstanceId()
				.addOnCompleteListener(task -> {
					if (!task.isSuccessful()) {
						Log.w(TAG, "getInstanceId failed", task.getException());
						initLiveTex();
						emitter.onComplete();
						return;
					}

					String token = task.getResult().getToken();
					Log.i(TAG, "firebase token = " + token);

					initLiveTex();
					emitter.onComplete();
				});
	});
}

private void initLiveTex() {
	new LiveTex.Builder(Const.TOUCHPOINT)
			.setDeviceToken(FirebaseInstanceId.getInstance().getToken())
			.build();
}
```