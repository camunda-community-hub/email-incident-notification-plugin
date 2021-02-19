# Vtičnik za obveščanje o incidentu po e-pošti

## Kako vam ta vtičnik lahko koristi?

Namen tega vtičnika je obveščati ljudi o incidentih v Camunda procesih po e-pošti.

## Kako deluje?

Vsakič, ko se zgodi incident, se njegovi podatki shranijo na seznam (glejte metodo `BufferingIncidentHandler.handleIncident`).

Redno (npr. vsakih pet minut) vtičnik preveri, ali se nahaja kakšen incident na tem seznamu.

V primeu, da je incident tam prisoten, vtičnik ustvari in pošlje e-poštna sporočila z informacijami o dogodkih.

## Kako izgleda e-poštno sporočilo o incidentu?
![Sample e-mail look][sample-email]

## Komu se pošljejo e-poštna sporočila?

E-poštni naslovi prejemnikov so določeni s spremenljivkami procesa
v BPMN datotekah. Za primer si oglejte [sample-process.bpmn](docs/sample-process.bpmn)

![Process Variables][process-variables]

V spremenljivkah procesa

  * `incidentNotificationReceiver` in
  * `incidentNotificationCc`

določite, kateremu prejemniku in KP naslovu naj vtičnik pošlje e-poštna sporočila.

Izvajanje storitvenega Task-a `Set up incident listener` (`Nastavi poslušalca incidentov`) lahko nastavite na prazen Java Delegate
[SetupIncidentListener](src/main/java/at/jit/incidentlistener/SetupIncidentListener.java).

Če naslovi prejemnikov niso nastavljani v diagramu BPMN, se uporavijo nadomestni naslovi v konfiguraciji vtičnika
(datoteka `bpm-platform.xml`).

## Kako lahko namestim vtičnik za poslušanje incidentov na Apache Tomcat?

### 1. korak

Poiščite različico vašega ciljnega sistema Camunda (sistem, kamor želite namestiti poslušalca incidentov).

### 2. korak

Oglejte si to skladišče Git.

### 3. korak

Preverite različico Camunda (spremenljivka `camunda.version`) v datoteki [pom.xml](pom.xml):

```xml
    <properties>
        <java.version>1.8</java.version>
        <maven.compiler.target>1.8</maven.compiler.target>
        <maven.compiler.source>1.8</maven.compiler.source>
        <camunda.version>7.14.0</camunda.version>
    </properties>
```

Če se ujema z različico iz koraka 1, nadaljujte s korakom 4. V nasprotnem primeru morate prilagoditi spremenljivko `camunda.version` v` pom.xml`
v repozitoriju poslušalca incidentov.

### 4. korak

Datoteko JAR ustvarite s pomočjo `mvn clean package`. To bo ustvarilo datoteko
`incident-listener-1.0.0-jar-with-dependencies.jar` v ciljnem imeniku `target`.

### 5. korak

Datoteko JAR iz 4. koraka položite v imenik `lib` namestitve Tomcat
(npr. `camunda/apache-tomcat-9.0.37/lib`).

### 6. korak

Odprite konfiguracijsko datoteko `bpm-platform.xml` 
(npr.camunda/apache-tomcat-9.0.37/conf/bpm-platform.xml`).

V to datoteko dodajte konfiguracijo vtičnika:

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

Posamezne nastavitve so opisane spodaj.

#### Konfiguracijski parameter `intervalMs`

Interval v milisekundah, v katerem vtičnik poslušalec incidenta
preveri, ali obstajajo incidenti, ki jih je treba prijaviti.

Če je `intervalMs` enak 1 minuti (60000 milisekund), bo e-poštno sporočilo
poslano največ vsako minuto (pod pogojem, da obstajajo
incidenti). Se pravi, če obstaja storitev, v kateri se 
incident zgodi vsako sekundo, bodo e-poštna sporočila kljub temu poslana le vsako
minuto.

Če je `intervalMs` nastavljen na 5 minut (300000 milisekund) in se
incidenti zgodijo vsako sekundo, bodo e-poštna sporočila kljub temu poslana vsakih pet
minut.

Koliko e-poštnih sporočil bo poslanih v posameznem intervalu, je odvisno -- izvzemši
`intervalMs` in prisotnost novih incidentov -- od števila nastavljenih naslovov/KP.


Predstavljajte si,

   * procesi so konfigurirani tako, da so vsa e-poštna sporočila poslana na `bojan@tvoje-podjetje.si`,
   * incidenti se zgodijo vsako sekundo in
   * `intervalMs` je nastavljen na 5 minut (`300000` milisekund).

V tem primeru bo eno e-sporočilo poslano na naslov `bojan@tvoje-podjetje.si` vsakih pet minut.

Zdaj pa si predstavljajte, da je za nekatere procese nastavljen e-poštni prejemnik `bojan@tvoje-podjetje.si`,
v drugih pa `alja@tvoje-podjetje.si`.

Takšna konfiguracija pomeni, da bost vsakih pet minut poslani
največ 2 e-poštni sporočili (eno na `bojan@tvoje-podjetje.si`, eno pa na
`alja@tvoje-podjetje.si`).

#### Konfiguracijski parameter `url`

URL naslov Camunda Cockpit-a aktivne Camunda instance.
Uporablja se za ustvarjanje povezave do spletne strani o incidentu v Camunda Cockpit-u.

#### Konfiguracijski parameter `fallbackMailReceiver`

Seznam e-poštnih naslovov, ločenih z vejicami, na katera naj se pošljejo
e-poštna sporočila v primeru, da ni nastavljen noben prejemnik v 
diagramu BPMN procesa, v katerem je prišlo do incidenta.

#### Konfiguracijski parameter `username`

Uporabniško ime e-poštnega strežnika (SMTP).

#### Konfiguracijski parameter `password`

Geslo e-poštnega strežnika.

#### Konfiguracijski parameter `subject`

Predmet e-pošte o incidentu

#### Konfiguracijski parameter `host`

Naslov (host) e-poštnega strežnika (SMTP).

#### Konfiguracijski parameter `port`

Port e-poštnega strežnika.

#### Konfiguracijski parameter `mailSender`

E-poštni naslov, ki je napisana v polju "Pošiljatelj".

#### Konfiguracijski parameter `mailBodyTemplate`

Predloga za besedilo e-poštnih sporočil o incidentu. Nadomestni znak 
`@INCIDENTS` označuje kraj, kjer bodo objavljene informacije o posameznem
incidentu. Vsak incident bo prikazan kot je določeno v parametru `incidentTemplate`.

#### Konfiguracijski parameter `incidentTemplate`

Predloga za vsak posamezen incident. Vsak incident
za določeno e-poštno sporočilo bo upodobljen s to predlogo.
Nato so vsi te incidenti združeni in zapisani na mesto
`@INCIDENTS` v` mailBodyTemplate`.

### 7. korak

Ko je konfiguracijska datoteka shranjena, znova zaženite Tomcat
z uporabo skript `shutdown.sh` in `startup.sh`.

## Kako lahko z minimalnim naporom preizkusim poslušalca incidentov?

### 1. korak

Namestite Docker.

### 2. korak

Ustvarite skripto `run.bat` z naslednjo vsebino:

```bat
docker run -d ^
-p 8080:8080 ^
-v <directory-1>/incident-listener-demo-app-1.0-SNAPSHOT.war:/camunda/webapps/incident-listener-demo-app.war ^
-v <directory-2>/incident-listener-1.0.0-jar-with-dependencies.jar:/camunda/lib/incident-listener-plugin.jar ^
-v <directory-3>/bpm-platform.xml:/camunda/conf/bpm-platform.xml ^
camunda/camunda-bpm-platform:tomcat-7.14.0
```

`<directory-1>`, `<directory-2>`, in `<directory-3>` so imeniki z izvorno kodo demo aplikacije,
koda vtičnika poslušalca incidentov in konfiguracijska datoteka. Nadalje bo razloženo, kaj vložiti v 
vsakega od teh imenikov.

Tu je dejanski primer take datoteke:

```bat
docker run -d ^
-p 8080:8080 ^
-v C:/usr/dp/dev/incident-listener-demo-app/target/incident-listener-demo-app-1.0-SNAPSHOT.war:/camunda/webapps/incident-listener-demo-app.war ^
-v C:/usr/dp/dev/incident-listener/target/incident-listener-1.0.0-jar-with-dependencies.jar:/camunda/lib/incident-listener-plugin.jar ^
-v C:/usr/dp/dev/incident-listener-example-tomcat/bpm-platform.xml:/camunda/conf/bpm-platform.xml ^
camunda/camunda-bpm-platform:tomcat-7.14.0
```

### 3. korak

Oglejte si [code of the demo application](https://github.com/jit-open/incident-listener-tomcat-demo) v
imeniku `<directory-1>`.

V datoteki BPMN `incident-listener-tomcat-app/src/main/resources/sample-process.bpmn`, dejavnost `Set up incident listener`,
spremenite vrednosti izhodnih parametrov `incidentNotificationReceiver` in `incidentNotificationCc` na željeni
vrednosti.

![How to change incident listener parameters in the Apache Tomcat demo app][img04]

Nato zaženite `mvn install`.

### 4. korak

Vstavite vtičnik za poslušalca incidentov v `<directory-2>` z uporabo `mvn clean package`.

### 5. korak

Kopirajte datoteko [bpm-platform-template.xml](docs/bpm-platform-template.xml) v `<directory-3>` in jo preimenujte v
`bpm-platform.xml`. Odprite to datoteko v urejevalniku besedil in se pomaknite do razdelka, kjer je vtičnik za poslušalca incidentov
konfiguriran.

Nastavite `url` na `http://localhost:8080/camunda` in   
spremenite lastnosti

 * `intervalMs`,
 * `fallbackMailReceiver`,
 * `username`,
 * `password`,
 * `host`,
 * `port`, in
 * `mailSender`.
 
### 6. korak

Zaženite paketno datoteko `run.bat`.

### 7. korak

Ko se strežnik zažene, pojdite v brskalnik na naslov http://localhost:8080/camunda. Uporabite uporabniško ime in geslo: `demo`/`demo`
za prijavo.

### 8. korak

Na seznamu opravil zaženite proces `Test Incident Listener`. Prejeli boste e-poštno sporočilo po času, določenem v
`intervalMs` je prenesen.

## Kako lahko namestim vtičnik poslušalca incidentov s programom Spring Boot?

### 1. korak

Vtičnik poslušalca incidentov zgradite z `mvn clean install`.

### 2. korak

Dependency poslušalca incidentov dodajte v datoteko `pom.xml` vašega Spring Boot projekta.

```xml
<dependency>
    <groupId>at.jit</groupId>
    <artifactId>incident-listener</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 3. korak

V `src/main/resources/application.yaml` dodajte nastavitve, povezane s poslušalcem incidentov, da bo videti približno tako:

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

Oznake "TODO" zamenjajte z dejanskimi vrednostmi posameznih lastnosti.

### 4. korak

Ustvarite Java class z anotacijo `org.springframework.context.annotation.Configuration`. Inject vrednosti iz
`application.yaml` v ta class. Na koncu dodajte metodo, ki ustvari primerek vtičnika poslušalca incidenta
in ga inicializira s podatki iz `application.yaml`.

Na koncu bo class videti tako:

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

### 5. korak

Zaženite aplikacijo. Zdaj bi morali prejemati e-pošto v primeru incidentov.

## Kje najdem primer poslušalca incidentov, ki se izvaja v Spring Boot?

Uporabite lahko [incident-listener-spring-boot-demo](https://github.com/jit-open/incident-listener-spring-boot-demo/tags)
demo aplikacijo. Ima dve oznaki:

 * `Initial_Version` je različica aplikacije brez poslušalca incidentov
 * `Incident_Listener_Works` je različica z delno nastavljenim poslušalcem incidentov. Da bo delovalo, prosim, preimenujte
   datoteko `src/main/resources/application.yaml-template` v `application.yaml` in zamenjajte oznake `TODO` v
   tej datoteki z dejanskimi vrednostmi parametrov. Nastavite e-poštne naslove prejemnika v
   `src/main/resources/sample-process.bpmn` (aktivnost `Set up incident listener`, `Input/Output` zavihek, `Output` plošča). 
   Nato lahko zaženete aplikacijo Spring Boot in poslušalec incidentov bi moral delovati.

## Imam proces s številnimi klicnimi aktivnostmi. Kako lahko definiram prejemnike e-pošte (v procesu na najvišji ravni)?

Če želite zagotoviti, da se nastavitve poslušalca incidentov posredujejo podprocesom, dodajte njihove preslikave v `Variables`
sekcijo Camunda Modeler. Morali bi biti dve `In Mappings` preslikavi za `incidentNotificationReceiver` in
`incidentNotificationCc`. V obeh primerih morate uporabiti naslednje nastavitve:

 * `Type`: `Source`
 * `Source` in `Target` morata biti nastavljena na ime ustrezne spremenljivke (`incidentNotificationReceiver` in `incidentNotificationCc`).

![How to pass incident listener variables to subprocesses][subprocess]

Primer najdete v [subprocess_example_parent.bpmn](docs/subprocess_example_parent.bpmn). Lahko ga vidite tudi 
v primeru [Spring Boot example](https://github.com/jit-open/incident-listener-spring-boot-demo/). 
To napravite tako, da

1. odprite [subprocess_example_parent.bpmn](https://github.com/jit-open/incident-listener-spring-boot-demo/blob/master/src/main/resources/subprocess_example_parent.bpmn),
2. in v dejavnosti `Set up incident listener` spremenite spremenljivki `incidentNotificationReceiver` in `incidentNotificationCc` na dejanski e-poštni naslov,
3. zaženite aplikacijo in
4. na seznamu opravil zaženite postopek `Subprocess Example (Parent)`.

## Različice

  * `1.0.0`: začetna različica

## Avtorji

  * Dmitrij Pisarenko
  * Miguel De los Santos La Paz

## Sponzor

"J-IT" IT-Dienstleistungs GesmbH

Vorgartenstraße 206B

Nadstropje 5

A-1020 Dunaj

Avstrija

![JIT logo][logo]

[logo]: docs/jit_logo.png "Logotip JIT"
[sample-email]: docs/img01.png "Vzorec e-pošte o incidentu"
[process-variables]: docs/img02.png "Spremenljivke procesa"
[subprocess]: docs/img03.png "Kako posredovati spremenljivke poslušalca incidentov v podprocese"
[img04]: docs/img04.png  "Kako spremeniti parametre poslušalca incidentov v demo aplikaciji Apache Tomcat"
