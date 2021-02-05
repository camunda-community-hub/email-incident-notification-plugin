# Плагин для оповещения о сбоях по электронной почте

## Как он может вам помочь?

Предназначение этого плагина -- оповещать пользователей об инцидентах в процессах Камунды по электронной почте.

## Как он работает?

Когда происходит инцидент, его данные сохраняются в связном списке (см. метод `BufferingIncidentHandler.handleIncident`).

Регулярно, например каждые пять минут, плагин проверяет, есть ли данные об инцидентах в этом списке.

Если есть, он отправляет письма с данными об этих сбоях.

## Как выглядит электронное письмо с данными об инцидентах?

![Пример письма с данными об инцидентах][sample-email]

## Кому отправляются электронные письма?

Адреса получателей устанавливаются в переменных процесса в файлах BPMN. Вы можете открыть файл [sample-process.bpmn](docs/sample-process.bpmn) в качестве примера.

![Переменные процесса][process-variables]

В переменных процесса

 * `incidentNotificationReceiver` и
 * `incidentNotificationCc`

определяется, каким получателям и по каким "адресам в копии" (CC) должны отправляться электронные письма.

Реализацией сервис таска `Set up incident listener` может быть пустой делегат (`JavaDelegate`) [SetupIncidentListener](src/main/java/at/jit/incidentlistener/SetupIncidentListener.java).

Если адреса получателей не указаны в диаграмме BPMN, то используются значения по умолчанию, указанные в конфигурации плагина (файл `bpm-platform.xml`).

## Как я могу установить плагин, если моя Камунда работает внутри Томката?

### Шаг 1

Определите версию Камунды в вашей целевой системе (той системе, где вы хотите установить плагин).

### Шаг 2

Клонируйте (`git clone`) это хранилище.

### Шаг 3

Проверьте версью Камунды (свойство `camunda.version`) в файле [pom.xml](pom.xml):

```xml
    <properties>
        <java.version>1.8</java.version>
        <maven.compiler.target>1.8</maven.compiler.target>
        <maven.compiler.source>1.8</maven.compiler.source>
        <camunda.version>7.14.0</camunda.version>
    </properties>
```

Если она совпадает с версией из шага 1, переходите к шагу 4. Если не совпадает, Вам нужно изменить свойство `camunda.version` в файле `pom.xml` в хранилище плагина.

### Шаг 4

Соберите файл JAR с помощью `mvn clean package`. После этого вызова файл `incident-listener-1.0.0-jar-with-dependencies.jar` появится в директории `target`.

### Шаг 5

Поместите файл JAR из шага 4 в директорию `lib` вашего Томката (например, `camunda/apache-tomcat-9.0.37/lib`).

### Шаг 6

Откройте конфигурационный файл `bpm-platform.xml` (например, `camunda/apache-tomcat-9.0.37/conf/bpm-platform.xml`).

Добавьте раздел конфигурации плагина в этот файл:

````xml
      <plugin>
          <class>at.jit.incidentlistener.CustomIncidentHandlerPlugin</class>
	  <properties>
               <property name="intervalMs">300000</property>
               <property name="url">https://myserver.com/camunda</property>
               <property name="fallbackMailReceiver">recipient1@provider.com,recipient2@provider.com</property>
               <property name="username">MyUserName</property>
               <property name="password">XXXXXXXXXXXXX</property>
               <property name="subject">Incident Report Camunda</property>
               <property name="host">mailserver-host</property>
               <property name="port">587</property>
               <property name="mailSender">camunda@mycompany.at</property>
               <property name="mailBodyTemplate">An error occured in at least one of your processes!
Please check the following processes in your Camunda Cockpit:

@INCIDENTS

******************** AUTOMATED MESSAGE ********************
</property>
               <property name="incidentTemplate">Process Activity: @ACTIVITY
Process Instance ID: @PROCESS_INSTANCE_ID
Incident Message: @MESSAGE
Incident Type: @INCIDENT_TYPE
Time of Occurrence: @TIME
Link to Process Instance: @URL
</property>
	  </properties>
      </plugin>
````

Параметры описаны ниже.

#### Параметр `intervalMs`

Интервал в миллисекундах, в котором плагин проверяет, есть ли инциденты, о которых надо оповестить.

Если `intervalMs` равен одной минуте (60000 миллисекунд), сообщения будут отправляться максимум раз в минуту (при условии, что есть инциденты, о которых надо сообщить). То есть, если у вас процесс, в котором инциденты происходят каждую секунду, электронные письма будут отправляться каждую минуту.

Если `intervalMs` установлен на 5 минут (300000 миллисекунд) и инциденты происходят каждую секунду, электронные письма будут отправляться каждые пять минут.

Сколько писем будет отправлено каждый раз зависит -- кроме значения `intervalMs` и наличия новых инцидентов -- от количества пар адрес/CC-адрес.

Допустим,

   * процессы настроены таким образом, что все сообщения об инцидентах отправляются по адресу `bob@yourcompany.com`,
   * инциденты происходят каждую секунду и
   * `intervalMs` установлен на 5 минут (300000 milliseconds).

В этом случае каждые пять минут по адресу `bob@yourcompany.com` будет отправляться одно письмо.

А теперь представьте, что в одних процессах получатель писем об инцидентах равен `bob@yourcompany.com`, а в других -- `alice@yourcompany.com`.

При прочих равных, это значит, что каждые пять минут будет отправляться максимум 2 письма (один по адресу `bob@yourcompany.com`, второй по адресу `alice@yourcompany.com`).

#### Параметр `url`

Ссылка на кокпит Камунды. Она используется для того, чтобы создать ссылку на инцидент в кокпите Камунды.

#### Параметр `fallbackMailReceiver`

Список адресов, разделенный запятыми (`,`), на которые должны отправляться письма об инцидентах, если получатели не установлены в диаграмме BPMN того процесса, где произошел инцидент.

#### Параметр `username`

Имя пользователя почтового сервера (SMTP).

#### Параметр `password`

Пароль почтового сервера.

#### Параметр `subject`

Тема писем с сообщениями об инцидентах.

#### Параметр `host`

Адрес почтового сервера (SMTP).

#### Параметр `port`

Порт почтового сервера.

#### Параметр `mailSender`

Адрес электронной почты отправителя сообщений об инцидентах.

#### Параметр `mailBodyTemplate`

Шаблон тела писем об инцидентах. Текст `@INCIDENTS` обозначает то место, в котором будет отображаться информация об отдельных инцидентах. Данные о каждом инциденте будут отображены согласно шаблону `incidentTemplate`.

#### Параметр `incidentTemplate`

Шаблон для отображения данных отдельных инцидентов. Данные о каждом инциденте отображаются согласно этому шаблону. После этого, текст `@INCIDENTS` в шаблоне `mailBodyTemplate` заменяется на объединенные тексты отдельных инцидентов. 

### Шаг 7

После того, как конфигурационный файл был сохранен, перезапустите Томкат с помощьюскриптов `shutdown.sh` и `startup.sh`.

## Как я могу попробовать плагин с минимальными усилиями?

### Шаг 1

Установите Докер.

### Шаг 2

Создайте файл `run.bat`, который содержит следующее:

```bat
docker run -d ^
-p 8080:8080 ^
-v <directory-1>/incident-listener-demo-app-1.0-SNAPSHOT.war:/camunda/webapps/incident-listener-demo-app.war ^
-v <directory-2>/incident-listener-1.0.0-jar-with-dependencies.jar:/camunda/lib/incident-listener-plugin.jar ^
-v <directory-3>/bpm-platform.xml:/camunda/conf/bpm-platform.xml ^
camunda/camunda-bpm-platform:tomcat-7.14.0
```

В директориях `<directory-1>`, `<directory-2>` и `<directory-3>` находится код демонстрационного приложения (`<directory-1>`), код плагина (`<directory-2>`) и конфигурационный файл (`<directory-3>`). Подробнее содержимое этих директорий будет описано ниже.

Вот пример такого файла из реальной жизни:

```bat
docker run -d ^
-p 8080:8080 ^
-v C:/usr/dp/dev/incident-listener-demo-app/target/incident-listener-demo-app-1.0-SNAPSHOT.war:/camunda/webapps/incident-listener-demo-app.war ^
-v C:/usr/dp/dev/incident-listener/target/incident-listener-1.0.0-jar-with-dependencies.jar:/camunda/lib/incident-listener-plugin.jar ^
-v C:/usr/dp/dev/incident-listener-example-tomcat/bpm-platform.xml:/camunda/conf/bpm-platform.xml ^
camunda/camunda-bpm-platform:tomcat-7.14.0
```

### Шаг 3

Выгрузите (`git clone`) [код демонстрационного приложения](https://github.com/jit-open/incident-listener-tomcat-demo) в директорию `<directory-1>`.

В файле BPMN `incident-listener-tomcat-app/src/main/resources/sample-process.bpmn`, активность `Set up incident listener`, измение значения параметров `incidentNotificationReceiver` and `incidentNotificationCc`.

![Как изменить параметры плагина в демонстрационной программе на базе Apache Tomcat][img04]

После этого запустите `mvn install`.

### Шаг 4

Соберите плагин в `<directory-2>` с помощью `mvn clean package`.

### Шаг 5

Сделайте копию файла [bpm-platform-template.xml](docs/bpm-platform-template.xml) в директории `<directory-3>` и назовите файл копии `bpm-platform.xml`. Откройте этот файл в редакторе и перейдите к разделу, где конфигурируется плагин.

Присвойте параметру `url` значение `http://localhost:8080/camunda` и измените значения свойств

 * `intervalMs`,
 * `fallbackMailReceiver`,
 * `username`,
 * `password`,
 * `host`,
 * `port`, and
 * `mailSender`.

### Шаг 6

Запустите файл `run.bat`.

### Шаг 7

Когда сервер запустится, перейдите в браузере по ссылке http://localhost:8080/camunda. Войдите в приложение с помощью логина и пароля `demo`/`demo`.

### Шаг 8

Запустите процесс `Test Incident Listener` в списке задач (task list). После того, как пройдет время, указанное в `intervalMs`, вам должно прийти электронное письмо.

## Как я могу установить плагин в приложение на основе Spring Boot?

### Шаг 1

Соберите плагин с помощью `mvn clean install`.

### Шаг 2

Добавьте зависимость плагина в файл `pom.xml` вашего приложения на основе Spring Boot.

```xml
<dependency>
    <groupId>at.jit</groupId>
    <artifactId>incident-listener</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Шаг 3

Добавьте настройки плагина в файл `src/main/resources/application.yaml` после чего он должен выглядеть примерно так:

```yaml
camunda.bpm:
  admin-user:
    id: demo
    password: demo
    firstName: Demo
  filter:
    create: All tasks
incident-listener:
  intervalMs: 1000
  url: http://localhost:8080/camunda
  fallbackMailReceiver: TODO
  username: TODO
  password: TODO
  host: TODO
  port: TODO
  mailSender: TODO
  subject: Incident Report Camunda
  mailBodyTemplate: |
    An error occured in at least one of your processes!
    Please check the following processes in your Camunda Cockpit:

    @INCIDENTS

    ******************** AUTOMATED MESSAGE ********************
  incidentTemplate: |
    Process Activity: @ACTIVITY
    Process Instance ID: @PROCESS_INSTANCE_ID
    Incident Message: @MESSAGE
    Incident Type: @INCIDENT_TYPE
    Time of Occurrence: @TIME
    Link to Process Instance: @URL
```

Пожалуйста замените маркеры `TODO` на соответствующие значения.

### Шаг 4

Создайте класс с аннотацией `org.springframework.context.annotation.Configuration`. Вставьте (inject) значения из файла `application.yaml` в атрибуты этого класса. Наконец, добавьте метод, который создает экземпляр плагина и инициализирует его значениями из `application.yaml`.

Ваш класс будет выглядеть примерно так:

```java
import at.jit.incidentlistener.CustomIncidentHandlerPlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IncidentListenerConfig {
    @Value("${incident-listener.intervalMs}")
    private String intervalMs;

    @Value("${incident-listener.url}")
    private String url;

    @Value("${incident-listener.fallbackMailReceiver}")
    private String fallbackMailReceiver;

    @Value("${incident-listener.username}")
    private String username;

    @Value("${incident-listener.password}")
    private String password;

    @Value("${incident-listener.host}")
    private String host;

    @Value("${incident-listener.port}")
    private String port;

    @Value("${incident-listener.mailSender}")
    private String mailSender;

    @Value("${incident-listener.mailBodyTemplate}")
    private String mailBodyTemplate;

    @Value("${incident-listener.incidentTemplate}")
    private String incidentTemplate;

    @Value("${incident-listener.subject}")
    private String subject;

    @Bean
    public ProcessEnginePlugin createIncidentListenerPlugin() {
        final CustomIncidentHandlerPlugin plugin = new CustomIncidentHandlerPlugin();
        plugin.setIntervalMs(intervalMs);
        plugin.setUrl(url);
        plugin.setFallbackMailReceiver(fallbackMailReceiver);
        plugin.setUsername(username);
        plugin.setPassword(password);
        plugin.setHost(host);
        plugin.setPort(port);
        plugin.setMailSender(mailSender);
        plugin.setMailBodyTemplate(mailBodyTemplate);
        plugin.setIncidentTemplate(incidentTemplate);
        plugin.setSubject(subject);
        return plugin;
    }
}
```

### Шаг 5

Запустите приложение. Теперь вам должны приходить электронные сообщения в случае инцидентов.

## Где я могу найти пример плагина, который работает внутри приложения на базе Spring Boot?

Вы можете использовать демонстрационное приложение [incident-listener-spring-boot-demo](https://github.com/jit-open/incident-listener-spring-boot-demo/tags). У него есть два ярлыка:

 * `Initial_Version` -- это версия приложения без плагина.
 * `Incident_Listener_Works` -- это версия, в которой плагин установлен. Чтобы плагин работал, пожалуйста, переименуйте файл `src/main/resources/application.yaml-template` в `application.yaml` и замените маркеры `TODO` в этом файле на соответствующие значения. Установите адреса получателей в файле `src/main/resources/sample-process.bpmn` (активность `Set up incident listener`, вкладка `Input/Output`, панель `Output`). После этого запустите приложение на Spring Boot. Плагин должен работать.

## У меня процесс с большим количеством подпроцессов. Как мне установить получателей писем этого плагина один раз (в процессе, который вызывает подпроцессы)?

Чтобы настройки плагина передавались подпроцессам, нужно добавить соответствия (mappings) в разделе `Variables` Камунда Моделера. Там должны быть два соответствия (`In Mappings`) `incidentNotificationReceiver` и `incidentNotificationCc`.

В обоих случаях, Вам надо установить следующие значения:

 * `Type`: `Source`
 * `Source` и `Target` должны быть равны названию соответствующей переменной (`incidentNotificationReceiver` и `incidentNotificationCc`).

![Как передавать переменные плагина в подпроцессы][subprocess]

Вы можете найти пример в файле [subprocess_example_parent.bpmn](docs/subprocess_example_parent.bpmn). Также, вы можете попробовать это в действии в [примере на базе Spring Boot](https://github.com/jit-open/incident-listener-spring-boot-demo/).

You can find an example in [subprocess_example_parent.bpmn](docs/subprocess_example_parent.bpmn). You can also see it
in action in the [Spring Boot example](https://github.com/jit-open/incident-listener-spring-boot-demo/). To
do so, 

1. open [subprocess_example_parent.bpmn](https://github.com/jit-open/incident-listener-spring-boot-demo/blob/master/src/main/resources/subprocess_example_parent.bpmn),
2. in the activity `Set up incident listener` change the variables `incidentNotificationReceiver` and `incidentNotificationCc` to actual e-mail addresses,
3. start the application, and
4. start the `Subprocess Example (Parent)` process in the task list.

## Versions

 * `1.0.0`: Initial version.

## Contributors

 * Dmitrii Pisarenko
 * Miguel De los Santos La Paz

## Sponsor

"J-IT" IT-Dienstleistungs GesmbH

Vorgartenstraße 206B

5th floor

A-1020 Vienna

Austria

![JIT logo][logo]

[logo]: docs/jit_logo.png "JIT Logo"
[sample-email]: docs/img01.png "Пример письма с данными об инцидентах"
[process-variables]: docs/img02.png "Переменные процесса"
[subprocess]: docs/img03.png "Как передавать переменные плагина в подпроцессы"
[img04]: docs/img04.png "Как изменить параметры плагина в демонстрационной программе на базе Apache Tomcat"
