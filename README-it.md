# Plugin per notifiche via Email in caso di problemi

## Come puoi trarne vantaggio

L'obiettivo di questo plugin è quello di notificare le persone responsabili via email in caso di problemi nei processi in Camunda.

## Come funziona?

Qaundo un errore accade, i dati vengono salvati in una lista (guarda `BufferingIncidentHandler.handleIncident`).

Regolarmente (esempio, ogni cinque minuti), un codice controlla se ci sono o meno errori in quella lista.

Nel caso ci fossero errori, degli e-mail verranno spediti con l'informazione riguardante l'errore.

## Come appare l'email di un errore?

![Email di esempio][sample-email]

## A chi vengono spediti gli email?

Gli indirizzi email vengono definiti in variabili di processo nei file BPMN. Apri [sample-process.bpmn](docs/sample-process.bpmn) per avere un esempio.

![Variabili di processo][process-variables]

Variabili di processo

 * `incidentNotificationReceiver` e
 * `incidentNotificationCc`

Indica gli indirizzi del destinatario e di chi deve ricevere la mail per conoscenza.

L'implementazione del Service task `Set up incident listener` può essere impostato ad un Java Delegate vuoto 
[SetupIncidentListener](src/main/java/at/jit/incidentlistener/SetupIncidentListener.java).

Se gli indirizzi dei destinatari non venissero indicati nel diagramma BPM, verrano utilizzati degli indirizzi impostabili nella configurazione (file `bpm-platform.xml`) del plugin.

## Come posso installare il plugin Incident Listener su Apache Tomcat?

### Primo passo

Trova la versione di Camunda del sistema che vuoi targetizzare (sistema dove vuoi integrare il listener).

### Secondo passo

Clona questa repo

### Terzo passo

Controlla la versione di Camunda (proprietà `camunda.version`) nel [pom.xml](pom.xml):

```xml
    <properties>
        <java.version>1.8</java.version>
        <maven.compiler.target>1.8</maven.compiler.target>
        <maven.compiler.source>1.8</maven.compiler.source>
        <camunda.version>7.14.0</camunda.version>
    </properties>
```

Se la versione combacia la versione individuata nel primo passo, continua col passo successivo. Altrimenti devi adattare la proprietà `camunda.version` nel `pom.xml`
del Incident Listener.

### Quarto passo

Costruisci il JAR usando `mvn clean package`. Questo creerà il file `incident-listener-1.0.0-jar-with-dependencies.jar` nella cartella
`target`.

### Quinto passo

Metti il JAR dello step 4 nella cartella `lib` del tuo Tomcat
(e. g. `camunda/apache-tomcat-9.0.37/lib`).

### Sesto passo

Apri il file di configurazione `bpm-platform.xml` (e. g.
`camunda/apache-tomcat-9.0.37/conf/bpm-platform.xml`).

Aggiungi una configurazione per il plugin nel file come segue:

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

Le singole impostazioni vengono descritte di seguito.

#### Paramtero di configurazione `intervalMs`

Intervallo in millisecondi ogni quanto il listener controlla per errori da mandare gli avvisi.

Se `intervalMs` é uguale a 1 minuto (60000 millisecondi), le mail verranno spedite al massimo ogni minuto (nel caso ci fossero errori da riportare). 
Ovviamente se ci sono processi nei quali si sono verificati degli errori.

Nel caso in cui il parametro `intervalMs` venisse impostato a 5 minutei (300000 millisecondi) ed
errori accadono ogni secondo, le mail verranno comunque spedite ogni 5 minuti.

La quantità di email spedie ogni volta dipende, a parte da `intervalMs` e la presenza di nuovi errori, 
anche da quanti indirizzi e CC sono stati impostati.

Immagina, 

   * i processi sono configurati che tutti gli errori mandano un email a `bob@yourcompany.com`,
   * gli errori accadono ogni secondo, e
   * `intervalMs` è impostato a 5 minuti (`300000` millisecondi).

In questo caso, una mail verrà spedita a `bob@yourcompany.com` 
ogni 5 minuti.

Ora immagina che in alcuni processi il destinatario della mail è impostato a `bob@yourcompany.com`, e in 
CC -- `alice@yourcompany.com`.

Alle stesse condizioni di prima per quanto riguarda il resto, con questi destinatari, verranno spedite
al massimo 2 email ogni volta (uno a `bob@yourcompany.com`, uno a 
`alice@yourcompany.com`).

Intervallo in millisecondi.

#### parametro di configurazione `url`

È la URL del cockpit di Camunda, quindi del sistema Camunda targetizzato dal listener..
È utilizzato per connettersi con la pagina degli incidenti di Camunda.

#### Parametro di configurazione `fallbackMailReceiver`

Un elenco di email separati da una virgola ai quali le mail vengono spedite in caso di errori/incidenti, se non ne sono stati configurati 
nel file BPMN del processo in cui l'errore è accaduto.

#### Parametro di configurazione `username`

Nome utente del servizio di posta elettronica (SMTP).

#### Parametro di configurazione `password`

Password per accedere alla posta elettronica del mittente. (SMTP)

#### Parametro di configurazione `subject`

L'oggetto della mail che viene spedita con l'errore.

#### Parametro di configurazione `host`

Indirizzo del server mail (SMTP).

#### Parametro di configurazione `port`

Porta del server mail (SMTP) per la spedizione.

#### Parametro di configurazione `mailSender`

Mittente della mail che viene spedita con l'errore. Campo `From` della mail.

#### Parametro di configurazione `mailBodyTemplate`

Template per il testo della mail. Il placeholder 
`@INCIDENTS` indica il posto dove l'informazione riguardante il singolo errore verrà inserito.
Ogni errore verrà inserito come specificato nel parametro `incidentTemplate`.

#### Parametro di configurazione `incidentTemplate`

Template per ogni singolo errore. Ogni errore per in una mail verrà inserito come specificato nel template.
Poi, tutti gli errori costruiti vengono concatenati ed inseriti nel placeholder `@INCIDENTS` del `mailBodyTemplate`.

### Settimo passo

Quando il file di configurazione è stato salvato, riavvia Tomcat usando gli script `shutdown.sh` e poi `startup.sh`.

## Come posso provare il listener con il minimo sforzo?

### Step 1

Installa Docker.

### Step 2

Crea un file bat (per Windows) `run.bat` con il seguente:

```bat
docker run -d ^
-p 8080:8080 ^
-v <directory-1>/incident-listener-demo-app-1.0-SNAPSHOT.war:/camunda/webapps/incident-listener-demo-app.war ^
-v <directory-2>/incident-listener-1.0.0-jar-with-dependencies.jar:/camunda/lib/incident-listener-plugin.jar ^
-v <directory-3>/bpm-platform.xml:/camunda/conf/bpm-platform.xml ^
camunda/camunda-bpm-platform:tomcat-7.14.0
```

`<directory-1>`, `<directory-2>`, e `<directory-3>` sono le cartelle con la sorgente dell'applicazione demo,
il codice del plugin Incident Listenerthe, ed il file di configurazione rispettivamente. Sotto verrà spiegato cosa inserire in 
ognuna di queste cartelle.

Qua si trova un esempio reale del file:

```bat
docker run -d ^
-p 8080:8080 ^
-v C:/usr/dp/dev/incident-listener-demo-app/target/incident-listener-demo-app-1.0-SNAPSHOT.war:/camunda/webapps/incident-listener-demo-app.war ^
-v C:/usr/dp/dev/incident-listener-a1/target/incident-listener-1.0.0-jar-with-dependencies.jar:/camunda/lib/incident-listener-plugin.jar ^
-v C:/usr/dp/dev/incident-listener-example-tomcat/bpm-platform.xml:/camunda/conf/bpm-platform.xml ^
camunda/camunda-bpm-platform:tomcat-7.14.0
```

### Step 3

Clona il progetto dell'applicazione demo dalla [repository](https://github.com/jit-open/incident-listener-tomcat-demo) nella cartella 
 `<directory-1>`. 

Nel file BPMN `incident-listener-tomcat-app/src/main/resources/sample-process.bpmn`, nell'attività `Set up incident listener`,
cambia il valore del parametro in output `incidentNotificationReceiver` e `incidentNotificationCc` con i rispettivi valori.

![Come cambiare i parametri dell'Incident Listener in Tomcat][img04]


Poi esegui `mvn install`.

### Step 4

Esegui la build del plugin Incident Listener nella cartella `<directory-2>` usando `mvn clean package`.

### Step 5

Copia il file [bpm-platform-template.xml](docs/bpm-platform-template.xml) nella `<directory-3>` e rinominalo in 
`bpm-platform.xml`. Apri il file in un editor e spostati nella sezione in cui il listener è configurato.

Imposta la `url` a `http://localhost:8080/camunda` e poi cambia le altre proprietà

Cambia le proprietà

 * `intervalMs`,
 * `fallbackMailReceiver`,
 * `username`,
 * `password`,
 * `host`,
 * `port`, e
 * `mailSender`.

### Step 6

Esegui il file `run.bat`.

### Step 7

Quando il server è avviato, apri con il browser l'indirizzo http://localhost:8080/camunda. Utilizza le credenzali `demo`/`demo`
per autenticarti.

### Step 8

Avvia il processo `Test Incident Listener` nella task list. Dovresti già ricevere un email con l'errore dopo l'intervallo di tempo indicato in `intervalMs`.

## Come posso installare il plugin con Spring Boot?

### Step 1

Esegui la build del plugin Incident Listener con `mvn clean install`.

### Step 2

Aggiungi la dipendenza al plugin Incident Listener al `pom.xml` del tu progetto Spring Boot.

```xml
<dependency>
    <groupId>at.jit</groupId>
    <artifactId>incident-listener</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Step 3

Aggiungi le impostazioni relative al plugin in `src/main/resources/application.yaml` come segue:

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

Sostituisci i `TODO` con i valori effettivi delle rispettive proprietà.

### Step 4

Crea una classe con l'annotazione `org.springframework.context.annotation.Configuration`. Inserisci in questa classe 
la dipendenza (Con CDI) dei valori definiti in `application.yaml`. Finalmente, aggiungi un metodo che crea un istanza 
del listener e che lo inizializza con i valori caricati da `application.yaml`.

Alla fine la classe sarà come segue:

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

### Step 5

Avvia l'applicazione. Ora riceverai via email gli errori dei processi.

## Dove posso trovare un esempio del plugin Incident Listener lanciato con Spring Boot?

Puoi usare l'applicazione demo [incident-listener-spring-boot-demo](https://github.com/jit-open/incident-listener-spring-boot-demo/tags). 
Essa ha due tag:

 * `Initial_Version` è la versione dell'applicazione senza il plugin
 * `Incident_Listener_Works` è la versione con il plugin incident listener parzialmente impostato. Perchè sia funzionante bisogna
   rinominare il file `src/main/resources/application.yaml-template` in `application.yaml` e sostituire i `TODO` nel file con i valori effettivi. 
   Inserisci l'indirizzo email del destinatario in
   `src/main/resources/sample-process.bpmn` (attività `Set up incident listener`, tab `Input/Output`, pannello `Output`). 
   Successivamente puoi avviare l'applicazione Spring Boot ed il listener si avvia in automatico con esso.

## Ho un processo con molteplici attività chiamate. Come posso definire il destinatario solo una volta?

Per assicurare che le proprietàd del Incident Listener vengano passate ai sotto processi, aggiungi le mappature nella sezione `Variables`
del Camunda Modeler. Ci devono essere 2 `In Mappings` per `incidentNotificationReceiver` e 
`incidentNotificationCc`. In entrambi i casi, devi utilizzare la seguente coinfigurazione:

 * `Type`: `Source`
 * `Source` e `Target` devono valere il nome delle rispettive variabili (`incidentNotificationReceiver` e
   `incidentNotificationCc`).

![Come passare le variabili ai sotto processi][subprocess]

Puoi trovare un esempio nel [subprocess_example_parent.bpmn](docs/subprocess_example_parent.bpmn). Puoi anche vederlo in azione 
nel [esempio con Spring Boot](https://github.com/jit-open/incident-listener-spring-boot-demo/). Per far ciô, 

1. apri [subprocess_example_parent.bpmn](https://github.com/jit-open/incident-listener-spring-boot-demo/blob/master/src/main/resources/subprocess_example_parent.bpmn),
2. nell'attività `Set up incident listener` cambia la variabile `incidentNotificationReceiver` e `incidentNotificationCc` in indirizzi mail effettivi,
3. avvia l'applicazione, e
4. avvia il processo `Subprocess Example (Parent)` nella task list.

## Versioni

 * `1.0.0`: Initial version.

## Maintainer

TODO

## Contributors

TODO

## Sponsor

"J-IT" IT-Dienstleistungs GesmbH

Vorgartenstraße 206B

5th floor

A-1020 Vienna

Austria

![JIT logo][logo]

[logo]: docs/jit_logo.png "JIT Logo"
[sample-email]: docs/img01.png "Sample incident e-mail"
[process-variables]: docs/img02.png "Process Variables"
[subprocess]: docs/img03.png "How to pass incident listener variables to subprocesses"
[img04]: docs/img04.png "How to change incident listener parameters in the Apache Tomcat demo app"
