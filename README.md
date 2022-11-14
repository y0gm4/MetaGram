# MetaGram Api
***
[![](https://jitpack.io/v/CarbonCock/MetaGram.svg)](https://jitpack.io/#CarbonCock/MetaGram)

Simple extension of [rubenlaugs/TelegramBots](https://github.com/rubenlagus/TelegramBots) library to simplify creating a **Telegram** bot in **Java**

## Dependencies
***
- For *Maven* Add the JitPack repository and the dependencies into the `pom.xml` file:
   ```xml
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
	```
    ```xml
	<dependency>
		<groupId>com.github.CarbonCock</groupId>
		<artifactId>MetaGram</artifactId>
		<version>1.1.0</version>
	</dependency>
	```
- For *Gradle* Add the JitPack repository in your root `build.gradle` at the end of repositories, and add the dependency as shown:
  ```groovy
  allprojects {
      repositories {
          maven { url 'https://jitpack.io' }
      }
  }
  ```
  ```groovy
  dependencies {
      implementation 'com.github.CarbonCock:MetaGram:1.1.0'
  }
  ```

## Index
***
- [`Register events`](#Register-events)
- [`Command`](#Command)
  - [Permission example](#Command-permission)
  - [Help command](#Command)
- [`Callback`](#Callback)
  - [Permission example](##Callback-permission)
- [`Permission`](#Command-permission)
  - [Permission Handler](#Permission-hanlder)
- [`Default Listener`](#Default-listener)
  - [Filters]() | **Coming soon** |

## About
***
MetaGram is an extension of the **TelegramBots library**, written in **java**, to make the code more clean and easy to read/update.
It simplifies the creation of **commands**, their **help command**, listening to **callbacks**, and also a list of users (user ids) that *will* or *will not* have **permission** to send certain commands or certain callbacks.

# How to use
***

# Register events
There are **2 ways** to **register** an **event**:

1. Using the `registerEvent(Listener l)` method.
   This method is used to register one of the 3 events (`CommandListener`, `CallbackListener`, `UpdateListener`)

```java
public class Main {

  private static final String token = "bot token";
  private static final String username = "bot username";

  public static void main(String... args) throws TelegramApiException {
    TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class); // TelegramBots api rubenlaugs
    MetaGramApi bot = new MetaGramApi(); // MetaGram api CarbonCock
    bot.setBotToken(token);
    bot.setBotUsername(username);

    api.registerBot(bot); // TelegramBots api

    bot.registerEvent(new MyCommand()); // MetaGramApi register single generic event
  }
}
```

2. Using the `registerEvents(String packagePath)` method.
   This method allows you to **register all listener classes** in a package by passing the **package path**.

```java
bot.registerEvents("org.carboncock.metagram.test"); // It will register all the events of the package "test"
```
***
# Command

Let's create a class to handle our command, then implement the `CommandListener` interface that includes 2 methods:

- `public void onCommand(...)` It will be executed when the user writes a command
- `public void onHelpCommand(...)` It will be executed when the user writes /help [command]

To make it clear what kind of command we are talking about the `@Command(...)` annotation comes into our help, it requires the following **fields**:

- `String` **name** ---> *The name of the command*
- `int` **args** ----> *The number of arguments of the command* | **By default** zero
- `String[]` **aliases** ----> *Aliases of the command* | **By default** empty
- `boolean` **checkedArgs** ----> *Checking the number of arguments to execute the command* | **By default** true

### Example

```java
@Command(name = "say", args = 1, aliases = {"write", "w"})
public class MyCommand implements CommandListener {
  @Override
  public void onCommand(TelegramLongPollingBot bot, Update update) {
    String command = update.getMessage().getText();
    SendMessage mex = new SendMessage();
    mex.setChatId("" + update.getMessage().getChatId());
    mex.setText(command.substring(command.indexOf(" ")));
    try {
      bot.execute(mex);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onHelpCommand(TelegramLongPollingBot bot, Update update) {
    SendMessage mex = new SendMessage();
    mex.setChatId("" + update.getMessage().getChatId());
    mex.setText("usage: /say [word]");
    try {
      bot.execute(mex);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }
}
```
<img src="https://i.imgur.com/Kftug6u.png" align="center">

For handling a *variable number of arguments*, simply set the `checkedArgs` field to `false`.
***
# Command permission

A command can be used by everyone or only some people can use it.
By **annotating** the class of the command with `@Permission(...)` we will set other characteristics to the command:

- `Class<?>` **listLocation** ---> *The class that contains, as a field, the list of ids.* | *This field must be* **annotated** *with* `@PermissionHandler`
- `PermissionType` **type** ---> *The type of permission that users will have* | **By default** `PermissionType.ABLE_TO_DO`
- `SendMethod` **send** ---> *The sending method of the error message for missing permission*

Finally, we need to **implement** the `Permissionable` interface and **override** the `public String onPermissionMissing()` method.

### Example

```java
@Command(name = "say", args = 1, aliases = {"write", "w"})
@Permission(listLocation = BotManager.class, send = SendMethod.REPLY_MESSAGE)
public class MyCommand implements CommandListener, Permissionable {
  @Override
  public void onCommand(TelegramLongPollingBot bot, Update update) {
    String command = update.getMessage().getText();
    SendMessage mex = new SendMessage();
    mex.setChatId("" + update.getMessage().getChatId());
    mex.setText(command.substring(command.indexOf(" ")));
    try {
      bot.execute(mex);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onHelpCommand(TelegramLongPollingBot bot, Update update) {
    SendMessage mex = new SendMessage();
    mex.setChatId("" + update.getMessage().getChatId());
    mex.setText("usage: /say [word]");
    try {
      bot.execute(mex);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  @Override
  public String onPermissionMissing() {
    return "\uD83D\uDEAB You are not authorized to use this command!";
  }
}
```
# Permission handler
The class containing the `List<Long> admins` field is shown below.

```java
public class BotHandler {
  @PermissionHandler
  List<Long> admins = new ArrayList<>();

  // stuffs
}
```

Each time the **/say** command is sent this *list of ids* will be *checked*, if the id of the user who sent the command is **NOT present** in the list, the `public String onPermissionMissing()` method will be executed and the returned content will be sent (according to the sending method) to the user.

<img src="https://i.imgur.com/HP3IRZY.png" align="center">

In some cases we might have a list of admins that is present on a database or a file (we would need to add them before the permission check).
Here we are helped by the `@PermissionHandler` annotation seen earlier on the list field.
If we annotate a method without arguments it will be executed before the permission check, as a result it can be used to modify the id list before this check.

```java
public class BotHandler {
  @PermissionHandler
  List<Long> admins = new ArrayList<>();

  // stuffs

  @PermissionHandler
  public void updateAdmins(){
    // might execute a query to the database to update admins list
  }
}
```
***

## Callback

Let's create a class to handle our callback, then implement the `CallbackListener` interface that includes 1 method:

- `public void onCallback(...)` It will be executed when the bot recives a callback

To make it clear what kind of callback we are trying to handle the `@Callback(...)` annotation comes into our help, it requires the following **fields**:

- `String` **query** ---> *The data of the callback*
- `CallbackFilter` **filter** ---> *The type of filter for the query data* | **By default** `CallbackFilter.EQUALS`

### Example

```java
@Callback(query = "test")
public class MyCallback implements CallbackListener {
  @Override
  public void onCallback(TelegramLongPollingBot bot, Update update) {
    EditMessageText mex = new EditMessageText();
    mex.setChatId("" + update.getCallbackQuery().getMessage().getChatId());
    mex.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
    mex.setText("Clicked!");
    try {
      bot.execute(mex);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }
}
```

<img src="https://i.imgur.com/yqgi6qn.png" align="center">

<img src="https://i.imgur.com/2AnOWkM.png" align="center">

If we had a callback with a **variable part** we can set a different filter for example `CallbackFilter.START_WITH` or `CallbackFilter.CONTAINS`, it depends on the variable part.
***

# Callback permission

A callback can be sent by everyone or only some people can send it.
This part has already been covered in the [`Command permission`](##Command-permission) section

```java
@Callback(query = "test")
@Permission(listLocation = BotManager.class, send = SendMethod.ANSWER_CALLBACK_QUERY)
public class MyCallback implements CallbackListener, Permissionable {
  @Override
  public void onCallback(TelegramLongPollingBot bot, Update update) {
    EditMessageText mex = new EditMessageText();
    mex.setChatId("" + update.getCallbackQuery().getMessage().getChatId());
    mex.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
    mex.setText("Clicked!");
    try {
      bot.execute(mex);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  @Override
  public String onPermissionMissing() {
    return "\uD83D\uDEAB You're not allowed to click me!";
  }
}
```

<img src="https://i.imgur.com/lNjLPlt.png" align="center">

***
# Default listener

Let’s create a class to handle every update, then implement the UpdateListener interface that includes 1 method:
`public void onCallback(...)` It will be executed when the bot recives an update such the default **TelegramLongPollingBot**'s method
`public void onUpdateRecives(Update update)`

```java
public class MyUpdateClass implements UpdateListener {
  @Override
  public void onUpdate(TelegramLongPollingBot bot, Update update){
    // stuffs
  }
}
```

# EventHandler

Suppose we do not want to have one **command/callback** per **class**, but want to have *everything together*.
Let's create a class that extends the `Listener.class` interface and annotate it with the `EventHandler.class` annotation.
Next we create our methods for handling commands and callbacks and annotate them like this.

```java
@EventHandler
public class MyEventClass implements Listener {

  @Command("start")
  public void onStartCommand(TelegramLongPollingBot bot, Update update) {
    //stuffs
  }

  @Command(value = "removeuser", args = 1)
  @HelpIdentifier("removeuserhelp")
  @Permission(listLocation = Bot.class, send = SendMethod.REPLY_MESSAGE, onMissingPermission = "You are not able to do that!")
  public void onRemoveUser(TelegramLongPollingBot bot, Update update) {
    //stuffs
  }

  @Callback("home")
  public void onHomeCallback(TelegramLongPollingBot bot, Update update) {
    //stuffs
  }

  @HelpIdentifier("removeuserhelp")
  public void onRemoveUserHelp(TelegramLongPollingBot bot, Update update) {
    //stuffs
  }

}
```
