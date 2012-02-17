Read me First
-------------

Fluxtream is based on Spring(-MVC) 3.1, Spring Security 3.1 and JPA for the persistence. We use Jersey (Rest-WS) for exposing the REST API

This is a maven project with 5 sub-modules.

* core: mainly domain classes, persistence, services and support classes for the other modules (e.g. connectors)
* core-webapp: the web API (with Rest-WS annotations), base oauth classes and helper classes
* connectors: lots of them - but for all but the 10 connectors that are actually enabled, there is little more than basic authentication (sometimes even nothing)
* fluxtream-webapp: the web application you can see on fluxtream.com - in maintenance mode
* fluxtream-web: a new web application that we are starting to work on, based on twitter bootstrap

Build requirements
------------------

* you will need maven 3.x.x

Build instructions
------------------

* start by running the install script in fluxtream-app's maven directory, which will install missing dependencies in your local maven repository
* run `mvn install` in fluxtream-app

Tips
----

* We use JRebel during development, which works perfectly well for this application. Since we are open source, you should be able to use jrebel "social" thus without having to pay a license

Running requirements
--------------------

* Tomcat 7 or Jetty 7
* Mysql

Running the application (fluxtream-webapp)
------------------------------------------
* replace `xxx` in property files under `src/main/resources` with appropriate values (you need only provide oauth keys for the connectors you want to use, obviously)
* You need to set a global environment variable at the OS level of your system, named `TARGET_ENV`; use that value for the name of your environment-specific properties (it's called `local.properties` by default)
* inside `TOMCAT_ROOT` / `JETTY_ROOT` there is a `webapps/` directory; either drop the `fluxtream-webapp/target/ROOT.war` there or, or create a symbolic version there pointing to `fluxtream-webapp/target/ROOT`
* run tomcat: under `TOMCAT_ROOT/bin`, do `./startup.sh` or `./catalina.sh start`
* during development, and if you want to use JRebel (you should), create a symbolic link to the `target/ROOT` directory of the webapp module in Tomcat's or Jetty's `webapps/` directory

Copyright and license
---------------------

Copyright 2011-2012 Candide Kemmler

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.