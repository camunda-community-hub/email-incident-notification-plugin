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

Find out the Camunda version of your target system (system where you want to install the incident listener).

### Шаг 2

Check out this Git repository.

### Шаг 3

Check the Camunda version (property `camunda.version`) in the [pom.xml](pom.xml) file:

```xml
    <properties>
        <java.version>1.8</java.version>
        <maven.compiler.target>1.8</maven.compiler.target>
        <maven.compiler.source>1.8</maven.compiler.source>
        <camunda.version>7.14.0</camunda.version>
    </properties>
```

If it matches the version from step 1, continue with step 4. Otherwise you need to adapt `camunda.version` in `pom.xml`
of the incident listener repository.

### Шаг 4

Build the JAR file using `mvn clean package`. This will create the file 
`incident-listener-1.0.0-jar-with-dependencies.jar` in the directory
`target`.

### Шаг 5

Place the JAR file from step 4 into the `lib` directory of your Tomcat installation
(e. g. `camunda/apache-tomcat-9.0.37/lib`).

### Шаг 6

Open the configuration file `bpm-platform.xml` (e. g.
`camunda/apache-tomcat-9.0.37/conf/bpm-platform.xml`).

Add a plugin configuration entry to this file:

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

Individual settings are described below.

#### Configuration parameter `intervalMs`

Interval in milliseconds in which the incident listener
checks whether or not there are incidents to report.

If `intervalMs` is equal to 1 minute (60000 milliseconds), e-mails
will be sent at most every minute (provided that there are 
incidents to report). That is, if there is a process in which
an incident occurs every second, e-mails will be sent every
minute.

If `intervalMs` is set to 5 minutes (300000 milliseconds) and
incidents occur every second, e-mails will be sent every five
minutes.

How many e-mails will be sent each time depends -- apart from
`intervalMs` and the presence of new incidents -- how many
address/CC pairs there are.

Imagine, 

   * the processes are configured so that all incident e-mails
are sent to `bob@yourcompany.com`,
   * incidents occur every second, and
   * `intervalMs` is set to 5 minutes (`300000` milliseconds).

In this case, one e-mail will be sent to `bob@yourcompany.com` 
every five minutes.

Now imagine that in some processes the incident e-mail
recipient is `bob@yourcompany.com`, and in 
others -- `alice@yourcompany.com`.

Other things being equal, this means that every five minutes
at most 2 e-mails (one to `bob@yourcompany.com`, one to 
`alice@yourcompany.com`) will be sent.

#### Configuration parameter `url`

URL of the Camunda cockpit of the Camunda instance in question.
It is used to create a link to the incident page in Camunda
cockpit.

#### Configuration parameter `fallbackMailReceiver`

Comma-separated list of e-mail addresses to which incident
e-mails are sent, if no recipients are configured in 
the BPMN diagram of the process where the incident
occured.

#### Configuration parameter `username`

User name of the e-mail server (SMTP).

#### Configuration parameter `password`

Password of the e-mail server.

#### Configuration parameter `subject`

Subject of the incident e-mails

#### Configuration parameter `host`

Address (host) of the e-mail server (SMTP).

#### Configuration parameter `port`

Port of the e-mail server.

#### Configuration parameter `mailSender`

E-Mail which is written in the `From` field of incident
e-mails.

#### Configuration parameter `mailBodyTemplate`

Template for the text of the incident e-mails. The placeholder
`@INCIDENTS` marks the place where information about individual
incidents will be output to. Each incident will be output as
specified in the parameter `incidentTemplate`.

#### Configuration parameter `incidentTemplate`

Template for each individual incident. Each of the incidents 
for a particular e-mail will be rendered using this template.
Then, all these texts are concatenated and put instead of the
`@INCIDENTS` placeholder in `mailBodyTemplate`.

### Шаг 7

When the configuration file has been saved, restart Tomcat
using `shutdown.sh` and `startup.sh` scripts.

## How can I try out the incident listener with minimal effort?

### Шаг 1

Install Docker.

### Шаг 2

Create a batch `run.bat` file with following contents:

```bat
docker run -d ^
-p 8080:8080 ^
-v <directory-1>/incident-listener-demo-app-1.0-SNAPSHOT.war:/camunda/webapps/incident-listener-demo-app.war ^
-v <directory-2>/incident-listener-1.0.0-jar-with-dependencies.jar:/camunda/lib/incident-listener-plugin.jar ^
-v <directory-3>/bpm-platform.xml:/camunda/conf/bpm-platform.xml ^
camunda/camunda-bpm-platform:tomcat-7.14.0
```

`<directory-1>`, `<directory-2>`, and `<directory-3>` are directories with the source code of demo application,
the incident listener plugin code, and the configuration file, respectively. Below it will be explained what to put into
each of these directories.

Here is a real-life example of such file:

```bat
docker run -d ^
-p 8080:8080 ^
-v C:/usr/dp/dev/incident-listener-demo-app/target/incident-listener-demo-app-1.0-SNAPSHOT.war:/camunda/webapps/incident-listener-demo-app.war ^
-v C:/usr/dp/dev/incident-listener/target/incident-listener-1.0.0-jar-with-dependencies.jar:/camunda/lib/incident-listener-plugin.jar ^
-v C:/usr/dp/dev/incident-listener-example-tomcat/bpm-platform.xml:/camunda/conf/bpm-platform.xml ^
camunda/camunda-bpm-platform:tomcat-7.14.0
```

### Шаг 3

Check out the [code of the demo application](https://github.com/jit-open/incident-listener-tomcat-demo) into 
directory `<directory-1>`. 

In the BPMN file `incident-listener-tomcat-app/src/main/resources/sample-process.bpmn`, activity `Set up incident listener`,
change the values of output parameters `incidentNotificationReceiver` and `incidentNotificationCc` to their
respective values.

![How to change incident listener parameters in the Apache Tomcat demo app][img04]


Then run `mvn install`.

### Шаг 4

Build the incident listener plugin in `<directory-2>` using `mvn clean package`.

### Шаг 5

Copy the file [bpm-platform-template.xml](docs/bpm-platform-template.xml) to `<directory-3>` and rename it to 
`bpm-platform.xml`. Open that file in an editor and navigate to the section where the incident listener plugin is
configured.

Set `url` to `http://localhost:8080/camunda` and change the properties

Change the properties

 * `intervalMs`,
 * `fallbackMailReceiver`,
 * `username`,
 * `password`,
 * `host`,
 * `port`, and
 * `mailSender`.

### Шаг 6

Run the batch file `run.bat`.

### Шаг 7

When the server has started, navigate your browser to http://localhost:8080/camunda. Use the credentials `demo`/`demo`
to login.

### Шаг 8

Start the process `Test Incident Listener` in the task list. You should receive an e-mail after the time specified in 
`intervalMs` passed.

## How can I install the incident listener plugin with Spring Boot?

### Шаг 1

Build the incident listener plugin with `mvn clean install`.

### Шаг 2

Add the dependency of the incident listener to the `pom.xml` file of your Spring Boot project.

```xml
<dependency>
    <groupId>at.jit</groupId>
    <artifactId>incident-listener</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Шаг 3

Add settings related to the incident listener to `src/main/resources/application.yaml` so that it looks something
like this:

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

Please substitute `TODO` markers with the actual values of the respective properties.

### Шаг 4

Create a class with the `org.springframework.context.annotation.Configuration` annotation. Inject the values from
`application.yaml` into this class. Finally, add a method that creates an instance of the incident listener
plugin and initializes it with the data from `application.yaml`.

At the end, the class will look like this:

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

Start the application. Now you should receive e-mails in case of incidents.

## Where can I find an example of incident listener running in Spring Boot?

You can use the [incident-listener-spring-boot-demo](https://github.com/jit-open/incident-listener-spring-boot-demo/tags)
demo application. It has two tags:

 * `Initial_Version` is the version of the application without incident listener
 * `Incident_Listener_Works` is the version with the incident listener partially set up. In order for it to work, please
   rename the file `src/main/resources/application.yaml-template` to `application.yaml` and replace `TODO` markers in
   this file with the actual parameter values. Set the recipient e-mail addresses in 
   `src/main/resources/sample-process.bpmn` (activity `Set up incident listener`, `Input/Output` tab, `Output` panel). 
   Thereafter you can start the Spring Boot application and the incident
   listener should work.

## I have a process with many call activities. How can I define the incident e-mail recipients once (in the top-level process)?

To make sure that the incident listener settings are passed to the subprocesses, add their mappings in the `Variables`
section of the Camunda Modeler. There should be two `In Mappings` for `incidentNotificationReceiver` and 
`incidentNotificationCc`. In both cases, you need to use following settings:

 * `Type`: `Source`
 * `Source` and `Target` must be set to the name of the respective variable (`incidentNotificationReceiver` and
   `incidentNotificationCc`).

![How to pass incident listener variables to subprocesses][subprocess]

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
[subprocess]: docs/img03.png "How to pass incident listener variables to subprocesses"
[img04]: docs/img04.png "How to change incident listener parameters in the Apache Tomcat demo app"
