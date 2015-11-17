# Sample for Sentilo Java client library

This is a sample web application that uses the Sentilo Java platform client

# Usage

### Configure

Before run this sample, you must edit the properties files that informs some connection values.

**/src/resources/properties/application.properties**

* **rest.client.host=YOUR\_SENTILO\_PLATFORM\_CLIENT\_ADDRESS** : put here your Sentilo instance host ip address
* **rest.client.identityKey=YOUR\_IDENTITY\_KEY** : put here your credential identity key

If you would change the sensor and component names, for example, you can do it in this same file.

### Package

After configure the application, you can package it with Maven:

<pre>
$ maven clean package
</pre>

After that, you get the generated file **/target/sentilo-client-java-sample.war** and publish it to your webapp container (such Tomcat 7)

### Execute the sample

Start your webapp server and acces to the sample from a web browser (we assume you're publiching from localhost and 8080 port):

<pre>http://localhost:8080/sentilo-client-java-sample/</pre>

Every time you access this url the system will send an observation to the Sentilo instance. If an error occurs, you'll see in the result web page.


Enjoy it. 