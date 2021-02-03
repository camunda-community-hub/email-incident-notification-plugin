# Plugin zur Benachrichtigung über Fehler via E-mail

## Wie können Sie davon profitieren?

Der Zweck dieses Plugins besteht darin, BenutzerInnen über Fehler (incidents) in Camunda-Prozessen via E-Mail zu verständigen.

## Wie funktioniert es?

Wenn ein Fehler auftritt, werden seine Daten in einer Liste gespeichert (siehe Methode `BufferingIncidentHandler.handleIncident`).

In regelmässigen Zeitabständen (z. B. alle fünf Minuten), überprüft eine Routine, ob es Fehlerdaten in dieser Liste gibt.

Ist dies der Fall, werden E-mails mit Informationen über einzelne Fehler, verschickt.

## Wie sieht eine E-Mail mit Fehlerinformationen aus?

![Beispiel-E-Mail][sample-email]

## An wen werden die E-Mails verschickt?

Die E-Mail-Adressen der Empfänger werden über Prozessvariablen in BPMN-Diagrammen definiert. Ein Beispiel können Sie in [sample-process.bpmn](docs/sample-process.bpmn) finden. 

![Prozessvariablen][process-variables]

Prozessvariablen

 * `incidentNotificationReceiver` und
 * `incidentNotificationCc`

legen fest, an welche Empfänger- bzw. CC-Adressen die Fehler-Emails verschickt werden sollen.

Im Service-Task `Set up incident listener` kann ein leeres `JavaDelegate`
[SetupIncidentListener](src/main/java/at/jit/incidentlistener/SetupIncidentListener.java) verwendet werden.

Wenn die Adressen im BPMN-Diagramm nicht definiert sind, werden Default-Werte aus der Plugin-Konfiguration (Datei `bpm-platform.xml`) verwendet.

## Wie kann ich das Plugin installieren, wenn ich Camunda im Apache Tomcat-Server verwende?

### Schritt 1

Finden Sie die Camunda-Version Ihres Zielsystems heraus (jenes Systems, wo Sie das Plugin installieren wollen).

### Schritt 2

Klonen Sie (`git clone`) dieses Repository.

### Schritt 3

Überprüfen Sie die Camunda-Version (Eigenschaft `camunda.version`) in der [pom.xml](pom.xml)-Datei:

```xml
    <properties>
        <java.version>1.8</java.version>
        <maven.compiler.target>1.8</maven.compiler.target>
        <maven.compiler.source>1.8</maven.compiler.source>
        <camunda.version>7.14.0</camunda.version>
    </properties>
```

Wenn die Camunda-Version hier mit jener im Schritt 1 übereinstimmt, machen Sie bitte mit Schritt 4 weiter. Andernfalls ist es notwendig, die Eigenschaft `camunda.version` in der `pom.xml`-Datei anzupassen (sicherzustellen, dass im Plugin und auf Ihrem Zielsystem die gleiche Camunda-Version verwendet wird).

### Schritt 4

Bauen Sie die JAR-Datei des Plugins mit `mvn clean package`. Danach müsste eine Datei mit dem Namen `incident-listener-1.0.0-jar-with-dependencies.jar` im Verzeichnis `target` erstellt werden.

### Schritt 5

Kopieren Sie die JAR-Datei aus Schritt 4 in das Verzeichnis `lib` Ihrer Tomcat-Installation (z. B. `camunda/apache-tomcat-9.0.37/lib`).

### Schritt 6

Öffnen Sie bitte die Konfigurationsdatei `bpm-platform.xml` (z. B. `camunda/apache-tomcat-9.0.37/conf/bpm-platform.xml`).

Fügen Sie folgende Pluginkonfiguration zu dieser Datei hinzu:

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

Die einzelnen Einstellungen sind unten beschrieben.

#### Konfigurationsparameter `intervalMs`

Intervall in Millisekunden, in welchem das Plugin überprüft, ob es zu meldende Fehler gibt oder nicht.

Ist `intervalMs` gleich einer Minute (60000 Millisekunden), werden die E-Mails höchstens jede Minute verschickt (vorausgesetzt, es gibt Fehler, die in dieser Zeit aufgetreten sind). Wenn es z. B. einen Prozess gibt, in dem ein Fehler jede Sekunde auftritt, werden die E-Mails jede Minute verschickt.

Wenn `intervalMs` auf 5 Minuten (300000 Millisekunden) gesetzt ist und Fehler jede Sekunde auftreten, dann werden E-Mails alle 5 Minuten verschickt.

Wieviele E-Mails verschickt jedes Mal verschickt werden, hängt -- außer von `intervalMs` und dem Vorhandensein von Fehlern -- davon ab, wieviele Address- und CC-Paare es gibt.

Angenommen,

   * die Prozesse sind so konfiguriert, dass alle Fehler-E-Mails an `bob@yourcompany.com` verschickt werden sollen,
   * die Fehler jede Sekunde auftreten und
   * `intervalMs` auf 5 Minuten (300000 Millisekunden) gesetzt ist.

In diesem Fall wird jede Minute jeweils eine E-Mail an `bob@yourcompany.com` verschickt werden.

Nehmen wir einen anderen Fall: In einigen Prozessen sollen die Fehler-E-Mail an `bob@yourcompany.com` verschickt werden und in anderen -- an `alice@yourcompany.com`.

Wenn alles Andere gleich bleibt, dann werden jede Minute höchstens zwei E-Mails (eine an `bob@yourcompany.com`, die zweite an `alice@yourcompany.com`) verschickt.

#### Configuration parameter `url`

URL des Cockpits von Camunda auf dem System, wo das Plugin läuft. Sie wird verwendet, um einen Link zur Seite des Fehlers im Camunda Cockpit zu erstellen.

#### Konfigurationsparameter `fallbackMailReceiver`

Eine mit Beistrichen (`,`) getrennte Liste aller E-Mail-Adressen, an welche Fehler-E-Mails dann verschickt werden sollen, falls keine Adressen im jeweiligen BPMN-Diagramm angegeben sind.

#### Konfigurationsparameter `username`

Benutzername des Servers zum Versand von E-Mails (SMTP).

#### Konfigurationsparameter `password`

Passwort des Servers zum Versand von E-Mails.

#### Konfigurationsparameter `subject`

Betreff der Fehler-E-Mails.

#### Konfigurationsparameter `host`

Adresse (Host) des Servers zum E-Mailversand (SMTP).

#### Konfigurationsparameter `port`

Port des SMTP-Servers.

#### Konfigurationsparameter `mailSender`

E-Mail-Adresse, die im `Von`-Feld einer Fehler-E-Mail aufscheint.

#### Konfigurationsparameter `mailBodyTemplate`

Vorlage für den Text der Fehler-E-Mails. Der Platzhalter `@INCIDENTS` wird durch Informationen über einzelne Fehler ersetzt. Jeder Fehler, der dort aufscheint, wird mittels einer Vorlage formatiert, die man über den Konfigurationsparameter `incidentTemplate` einstellt.

#### Konfigurationsparameter `incidentTemplate`

Vorlage für die einzelnen Fehler. Die Informationen über jeden Fehler in einer bestimmten E-Mail werden mit Hilfe dieser Vorlage ausgegeben. Daraufhin werden alle diese Texte verkettet. Der Platzhalter `@INCIDENTS` in der Vorlage `mailBodyTemplate` wird dann durch diesen Text ersetzt.

### Schritt 7

Sobald die Konfigurationsdatei gespeichert wurde, starten Sie Tomcat mittels Shellscripts `shutdown.sh` und `startup.sh` neu.

## Wie kann ich das Plugin mit dem minimal Aufwand ausprobieren?

### Schritt 1

Installieren Sie Docker.

### Schritt 2

Erstellen Sie eine Batch-Datei `run.bat` mit folgendem Inhalt:

```bat
docker run -d ^
-p 8080:8080 ^
-v <directory-1>/incident-listener-demo-app-1.0-SNAPSHOT.war:/camunda/webapps/incident-listener-demo-app.war ^
-v <directory-2>/incident-listener-0.0.1-SNAPSHOT-jar-with-dependencies.jar:/camunda/lib/incident-listener-plugin.jar ^
-v <directory-3>/bpm-platform.xml:/camunda/conf/bpm-platform.xml ^
camunda/camunda-bpm-platform:tomcat-7.14.0
```

`<directory-1>`, `<directory-2>`, and `<directory-3>` sind Verzeichnisse, in denen sich die Demo-Anwendung (`<directory-1>`), das Plugin (`<directory-2>`) und die Konfigurationsdatei (`<directory-3>`) befinden. Unten wird erklärt, was genau in diesen Verzeichnissen stehen soll.

Hier ist ein Beispiel einer solchen Batch-Datei:

```bat
docker run -d ^
-p 8080:8080 ^
-v C:/usr/dp/dev/incident-listener-demo-app/target/incident-listener-demo-app-1.0-SNAPSHOT.war:/camunda/webapps/incident-listener-demo-app.war ^
-v C:/usr/dp/dev/incident-listener/target/incident-listener-0.0.1-SNAPSHOT-jar-with-dependencies.jar:/camunda/lib/incident-listener-plugin.jar ^
-v C:/usr/dp/dev/incident-listener-example-tomcat/bpm-platform.xml:/camunda/conf/bpm-platform.xml ^
camunda/camunda-bpm-platform:tomcat-7.14.0
```

### Schritt 3

Klonen (`git clone`) Sie den [Code der Demo-Anwendung](https://github.com/jit-open/incident-listener-tomcat-demo) in das Verzeichnis `<directory-1>`. 

Setzen Sie die Ausgabeparameter `incidentNotificationReceiver` und `incidentNotificationCc` in der BPMN-Datei `incident-listener-demo-app/src/main/resources/sample-process.bpmn`, Aktivität `Set up incident listener` auf die jeweiligen Werte.

![Wie man die Parameter des Plugins in der Demo-Anwendung für Apache Tomcat einstellen kann][img04]

Führen Sie dann `mvn install` aus.

### Schritt 4

Bauen Sie das Plugin im Verzeichnis `<directory-2>` mit `mvn clean package`.

### Schritt 5

Kopieren Sie die Datei [bpm-platform-template.xml](docs/bpm-platform-template.xml) ins Verzeichnis `<directory-3>` und nennen Sie es auf `bpm-platform.xml` um. Öffnen Sie diese Datei im Editor und navigieren Sie zum Abschnitt, in dem das Plugin konfiguriert wird.

Setzen Sie `url` auf `http://localhost:8080/camunda` und verändern Sie die Eigenschaften

 * `intervalMs`,
 * `fallbackMailReceiver`,
 * `username`,
 * `password`,
 * `host`,
 * `port`, and
 * `mailSender`.

### Schritt 6

Führen Sie die Batch-Datei `run.bat` aus.

### Schritt 7

Sobald der Server gestartet ist, öffnen Sie im Browser die Seite `http://localhost:8080/camunda`. Verwenden Sie die Zugansdaten `demo`/`demo`,
um sich einzuloggen.

### Schritt 8

Starten Sie den Prozess `Test Incident Listener` in der Taskliste. Nach der in `intervalMs` eingestellten Zeit müssten Sie eine E-Mail erhalten.

## Wie kann ich das Plugin in eine Spring Boot basierte Anwendung installieren?

### Schritt 1

Bauen Sie das Plugin mit `mvn clean install`.

### Schritt 2

Fügen Sie die Abhängigkeit des Plugins zur `pom.xml`-Datei Ihres Spring Boot-Projekts hinzu.

```xml
<dependency>
    <groupId>at.jit</groupId>
    <artifactId>incident-listener</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### Schritt 3

Fügen Sie die Einstellungen des Plugins zur Datei `src/main/resources/application.yaml` hinzu, sodass sie in etwa so aussieht:

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

Bitte ersetzen Sie die Platzhalter `TODO` mit den tatsächlichen Werten jeweiliger Eigenschaften. 

### Schritt 4

Erstellen Sie eine Klasse, die mit der Annotation `org.springframework.context.annotation.Configuration` markiert ist. Injizieren Sie die Werte aus `application.yaml` in diese Klasse. Fügen Sie eine Methode hinzu, die eine Instanz des Plugins erstellt und diese mit Werten aus `application.yaml` initialisiert.

Danach wird die Klasse folgendermaßen aussehen:

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

### Schritt 5

Starten Sie die Anwendung. Sie müssten nun E-Mails bekommen, wenn Fehler auftreten.

## Wo kann ich ein Beispiel des Plugins finden, das in einer Spring Boot-Anwendung läuft?

Sie können die Demo-Anwendung [incident-listener-spring-boot-demo](https://github.com/jit-open/incident-listener-spring-boot-demo/tags) verwenden. Es gibt dort zwei Tags:

 * `Initial_Version` ist die ursprüngliche Version der Anwendung ohne Plugin.
 * `Incident_Listener_Works` ist die Version, in der das Plugin teilweise eingerichtet ist. Damit es funktioniert, benennen Sie bitte die Datei `src/main/resources/application.yaml-template` in `application.yaml` um und ersetzen Sie die `TODO`-Platzhalter durch die jeweiligen Werte. Stellen Sie danach die Adressen der Empfänger der Fehler-E-Mails in `src/main/resources/sample-process.bpmn` ein (Aktivität `Set up incident listener`, Registerkarte `Input/Output`, Abschnitt `Output`). Danach können Sie die Anwendung starten und das Plugin sollte funktionieren.

## Ich habe einen Prozess mit vielen Call Activities. Wie kann ich die Empfänger der Fehler-E-Mails so konfigurieren, dass sie nur ein Mal definiert und dann in Unterprozessen wiederverwendet werden?

Um sicherzustellen, dass die Einstellungen des Plugins an die Unterprozesse übertragen werden, fügen Sie `In Mappings` im Abschnitt `Variables` im Camunda Modeler hinzu. Es muss zwei Mappings für `incidentNotificationReceiver` und `incidentNotificationCc` geben. In beiden Fällen sind folgende Einstellungen zu verwenden:

 * `Type`: `Source`
 * `Source` und `Target` müssen auf den Namen der jeweiligen Variable gesetzt sein (`incidentNotificationReceiver` und `incidentNotificationCc`).

![Wie man die Variablen des Plugins an Unterprozesse übertragen kann][subprocess]

Ein Beispiel dafür finden Sie in [subprocess_example_parent.bpmn](docs/subprocess_example_parent.bpmn). Sie können es auch in Aktion im [Spring Boot Beispiel](https://github.com/jit-open/incident-listener-spring-boot-demo/) sehen. Dazu machen Sie bitte Folgendes:

1. Öffnen Sie [subprocess_example_parent.bpmn](https://github.com/jit-open/incident-listener-spring-boot-demo/blob/master/src/main/resources/subprocess_example_parent.bpmn).
2. Setzen Sie die Variablen `incidentNotificationReceiver` and `incidentNotificationCc` auf tatsächliche E-Mail-Adressen in Aktivität `Set up incident listener`.
3. Starten Sie die Anwendung.
4. Starten Sie den Prozess `Subprocess Example (Parent)` in der Taskliste.

## Versionen

 * `1.0.0`: Erstfassung.

## Contributors

 * Dmitrii Pisarenko
 * Miguel De los Santos La Paz

## Sponsor

"J-IT" IT-Dienstleistungs GesmbH

Vorgartenstraße 206B

5ter Stock

A-1020 Wien

Österreich

![JIT logo][logo]

[logo]: docs/jit_logo.png "JIT Logo"
[sample-email]: docs/img01.png "Beispiel einer Fehler-E-Mail"
[process-variables]: docs/img02.png "Prozessvariablen"
[subprocess]: docs/img03.png "Wie man die Variablen des Plugins an Unterprozesse übermitteln kann"
[img04]: docs/img04.png "Wie man die Parameter des Plugins in der Apache Tomcat Demo-Anwendung verändern kann"
