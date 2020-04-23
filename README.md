
Cocop.AmqpRequestResponseHelper (Java) v.2.0.0
==============================================

---

<img src="logos.png" alt="COCOP and EU" style="display:block;margin-right:auto" />

COCOP - Coordinating Optimisation of Complex Industrial Processes  
https://cocop-spire.eu/

This project has received funding from the European Union's Horizon 2020
research and innovation programme under grant agreement No 723661. This piece
of software reflects only the authors' views, and the Commission is not
responsible for any use that may be made of the information contained therein.

---


Author
------

Petri Kannisto, Tampere University, Finland  
https://github.com/kannisto  
http://kannisto.org

**Please make sure to read and understand [LICENSE.txt](./LICENSE.txt)!**


COCOP Toolkit
-------------

This application is a part of COCOP Toolkit, which was developed to enable a
decoupled and well-scalable architecture in industrial systems. Please see
https://kannisto.github.io/Cocop-Toolkit/


Introduction
------------

This application is a software library to facilitate synchronous
request-response communication over the AMQP protocol (in particular,
RabbitMQ), because the native AMQP communication pattern is publish-subscribe
instead. This library provides you both a client and server.

This repository contains the following applications:

* Cocop.AmqpRequestResponseHelper API (JAR)
    * ClientTest class: application to run a client in tests
    * ServerTest class: application to run a server in tests

See also:

* Github repo: https://github.com/kannisto/Cocop.AmqpRequestResponseHelper_java
* API documentation: https://kannisto.github.io/Cocop.AmqpRequestResponseHelper_java


Environment and Libraries
-------------------------

The development environment was _Eclipse Photon Release (4.8.0), Build id:
20180619-1200_. The runtime was JDK8.

The following libraries were utilised in development:

* amqp-client-4.2.2.jar
    * see https://www.rabbitmq.com/download.html
* amqp-client-4.2.2-javadoc.jar
* commons-logging-1.2.jar
* slf4j-api-1.7.25.jar
* slf4j-nop-1.7.25.jar    
