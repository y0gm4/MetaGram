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
		<version>1.3.0</version>
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
      implementation 'com.github.CarbonCock:MetaGram:1.3.0'
  }
  ```

## Index
***
- [`Register events`](#Register-events)
- [`Command`](#Command)
  - [Permission example](#Command-permission)
  - [Help command](#Command)
- [`Callback`](#Callback)
  - [Permission example](#Callback-permission)
  - [Callback Filters](#Filters-example)
- [`Permission`](#Command-permission)
  - [Permission Handler](#Permission-handler)
- [`Filters`](#filters)
  - [Custom filters](#custom-filters)
- [`EventHandler`](#Event-Handler)
- [`Default Listener`](#Default-listener)


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

  private static final String TOKEN = "bot token";
  private static final String USERNAME = "bot username";

  public static void main(String... args) throws TelegramApiException {
    TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class); // TelegramBots api rubenlaugs
    MetaGramApi bot = new MetaGramApi(); // MetaGram api CarbonCock
    bot.setBotToken(TOKEN);
    bot.setBotUsername(USERNAME);

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

- `String` **value** ---> *The name of the command*
- `char` **prefix** ---> *The prefix of the command* | **By default** '/'
- `int` **args** ---> *The number of arguments of the command* | **By default** zero
- `String[]` **aliases** ---> *Aliases of the command* | **By default** empty
- `boolean` **checkedArgs** ---> *Checking the number of arguments to execute the command* | **By default** true

### Example

```java
@Command(value = "say", args = 1, aliases = {"write", "w"})
public class MyCommand implements CommandListener {
  @Override
  public void onCommand(CommandData cmd) {
    Update update = cmd.getUpdate();
    String command = update.getMessage().getText();
    SendMessage mex = new SendMessage();
    mex.setChatId("" + update.getMessage().getChatId());
    mex.setText(command.substring(command.indexOf(" ")));
    try {
      cmd.getBotInstance().bot.execute(mex);
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
@Command(value = "say", args = 1, aliases = {"write", "w"})
@Permission(listLocation = BotManager.class, send = SendMethod.REPLY_MESSAGE)
public class MyCommand implements CommandListener, Permissionable {
  @Override
  public void onCommand(CommandData cmd) {
    Update update = cmd.getUpdate();
    String command = update.getMessage().getText();
    SendMessage mex = new SendMessage();
    mex.setChatId("" + update.getMessage().getChatId());
    mex.setText(command.substring(command.indexOf(" ")));
    try {
      cmd.getBotInstance().bot.execute(mex);
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
    // actions ...
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

- `String` **value** ---> *The data of the callback*
- `CallbackFilter` **filter** ---> *The type of filter for the query data* | **By default** `CallbackFilter.EQUALS`

### Example

```java
@Callback("test")
public class MyCallback implements CallbackListener {
  @Override
  public void onCallback(CallbackData callbackData) {
    Update update = callbackData.getUpdate();
    EditMessageText mex = new EditMessageText();
    mex.setChatId("" + update.getCallbackQuery().getMessage().getChatId());
    mex.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
    mex.setText("Clicked!");
    try {
      callbackData.getBotInstance().execute(mex);
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
@Callback("test")
@Permission(listLocation = BotManager.class, send = SendMethod.ANSWER_CALLBACK_QUERY)
public class MyCallback implements CallbackListener, Permissionable {
  @Override
  public void onCallback(CallbackData callbackData) {
    Update update = callbackData.getUpdate();
    EditMessageText mex = new EditMessageText();
    mex.setChatId("" + update.getCallbackQuery().getMessage().getChatId());
    mex.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
    mex.setText("Clicked!");
    try {
      callbackData.getBotInstance().execute(mex);
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

## Filters example
Suppose we have a callback with variable data, that's where **filters** come in.
Depending on our needs, we can choose the filter that suits us best from these:
- `EQUALS`: This filter is already set by default, it is used to check if the received callback data is the same as the one set (not case sensitive)
- `START_WITH`: Checks if the data set matches the start of the received callback data
- `CONTAINS`: Checks if the data set are inside the received callback data
- `CUSTOM_PARAMETER`: This filter follows its own specific pattern:
  `filter_name={key1}&{key2}&{key3}...`
  - `filter_name`: It is the name of the filter (it works like the START_WITH filter) it basically checks if the received callback data starts with that sequence of characters.
  - `=`: Separates filter name from key sequence
  - `{key1}...`:  Sequence of keys which will be associated with the data sent and collected in a `Map<String, Object>` accessed by the `CallbackData` object

### CUSTOM_PARAMETER example
```java
@EventHandler
public class MailSectionServices implements Listener {

  private final DbManager db = MyBot.db;

  @Command("mails")
  public void onCommand(CommandData cmd){
    InlineKeyboardMarkup ikm = new InlineKeyboardMarkup();
    ikm.setKeyboard(Collections.singletonList(Collections.singletonList(InlineKeyboardButton.builder().text("support@CarbonCock.com").callbackData("rmusermail=%s&%s".formatted(0123456789L, "support@CarbonCock.com")).build())));
    cmd.getBotInstance().execute(SendMessage.builder()
            .chatId("" + cmd.getSender().getId())
            .text("mails settings")
            .replyMarkup(ikm)
            .build());
  }

  @Callback(value = "rmusermail={user_id}&{user_mail}", filter = CallbackFilter.CUSTOM_PARAMETER)
  public void onRemoveUserCB(CallbackData callbackData){
    Map<String, Object> parameters = callbackData.getParameters();
    db.removeMailFromUser(parameters.get("user_id"), parameters.get("user_mail"));
  }
}
```

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

# Event Handler

Suppose we do not want to have one **command/callback** per **class**, but want to have *everything together*.
Let's create a class that extends the `Listener.class` interface and annotate it with the `EventHandler.class` annotation.
Next we create our methods for handling commands and callbacks and annotate them like this.

```java
@EventHandler
public class MyEventClass implements Listener {

  @Command("start")
  public void onStartCommand(CommandData cmd) {
    //stuffs
  }

  @Command(value = "removeuser", args = 1)
  @HelpIdentifier("removeuserhelp")
  @Permission(listLocation = Bot.class, send = SendMethod.REPLY_MESSAGE, onMissingPermission = "You are not able to do that!")
  public void onRemoveUser(CommandData cmd) {
    //stuffs
  }

  @Callback("home")
  public void onHomeCallback(CallbackData callbackData) {
    //stuffs
  }

  @HelpIdentifier("removeuserhelp")
  public void onRemoveUserHelp(TelegramLongPollingBot bot, Update update) {
    //stuffs
  }

}
```

# Filters
> Filters allow filtering of received updates in order to split the flow over multiple methods.

Suppose we want to check for a user who writes a private message to the bot, it can be done using the `@Filters(...)` annotation,
it requires the following **fields**

- `FilterType[]` value ---> An array of filters, the update will be filtered by checking if at least one of them is true (logical OR)
- `String[]` fromWho ---> An array of strings, they stand for whether you should filter updates of users, bots, admins, etc... | **By default** "EVERYONE"
- `String[]` fromWhere ---> An array of strings, An array of strings, they stand for whether you should filter channel updates, groups, private chats, etc... | **By default** "EVERYWHERE"
- `String` regex ---> If the update is a message and contains text it will be taken and matched with the regex set | **By default** "" (means no regex)

```java
@EventHandler
public class MyEventClass implements Listener {

  @Filters(
          value = FilterType.MESSAGE,
          fromWhere = FilterType.Chat.PRIVATE,
          regex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$"
  )
  public void onMailReceived(TelegramLongPollingBot bot, Update update) {
    // do stuffs...
  }

}
```
Suppose instead we want to check whether sticker, photo or video are sended in a group or supergroup, so that we can check, in a hypothetical case, whether the user has permission to be able to send them.

```java
@EventHandler
public class MyEventClass implements Listener {

  @Filters(
          value = {FilterType.STICKER, FilterType.PHOTO, FilterType.VIDEO}, // (OR logic)
          fromWhere = {FilterType.Chat.GROUP, FilterType.Chat.SUPER_GROUP}, // (OR logic)
          fromWho = FilterType.By.USER
  )
  public void onStickerReceived(TelegramLongPollingBot bot, Update update) {
    // check user permissions...
  }

}
```

## Custom filters
> A custom filter to filter whenever there is an update.

To create a custom filter you have to create a method similar to the one for the regular filter, but with boolean return.
Then you need annotate it using the `@CustomFilterIdentifier(...)` annotation and specify a unique id.

Finally, we annotate the method in which we want to filter the update by annotation `@CustomFilter(...)` specifying the following **fields** (the annotation is repeatable)

- `Class<?>` cfLocation ---> The class in which the method is contained (custom filter) | **By default** Class.class (means the same class)
- `String` value ---> The value of the unique custom filter id

Suppose we need to filter only updates from users who are subscribed to a subscription or users who are whitelisted

```java
@EventHandler
public class MyEventClass implements Listener {

  @CustomFilter(cfLocation = MyEventClass.AnotherClass, value = "whitelist")
  @CustomFilter("is_subscribed")
  public void onFilteredUpdate(TelegramLongPollingBot bot, Update update) {
    // do stuffs...
  }

  @CustomFilterIdentifier("is_subscribed")
  public boolean isSubscribed(TelegramLongPollingBot bot, Update update) {
    // check subscription...
  }

  public class AnotherClass {

    @CustomFilterIdentifier("whitelist")
    public boolean isInWhitelist(TelegramLongPollingBot bot, Update update){
      // check whitelist...
    }
    
  }
  
}
```

