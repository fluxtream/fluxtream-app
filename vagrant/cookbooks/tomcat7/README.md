Description
===========

Installs and configures Tomcat 7.

Requirements
============

Platform:

* Debian, Ubuntu (OpenJDK, Oracle)
* CentOS 6+, Red Hat 6+, Fedora (OpenJDK, Oracle)

The following Opscode cookbooks are dependencies:

* java

Attributes
==========

* `node["tomcat7"]["port"]` - The network port used by Tomcat's HTTP connector, default `8080`.
* `node["tomcat7"]["ssl_port"]` - The network port used by Tomcat's SSL HTTP connector, default `8443`.
* `node["tomcat7"]["ajp_port"]` - The network port used by Tomcat's AJP connector, default `8009`.
* `node["tomcat7"]["java_options"]` - Extra options to pass to the JVM, default `-Xmx128M -Djava.awt.headless=true`.
* `node["tomcat7"]["use_security_manager"]` - Run Tomcat under the Java Security Manager, default `false`.
* `node["tomcat7"]["version"]` - The Tomcat version, default `7.0.27`.
* `node["tomcat7"]["user"]` - Tomcat user, default `tomcat`.
* `node["tomcat7"]["group"]` - Tomcat group, default `tomcat`.
* `node["tomcat7"]["target"]` - The target folder where tomcat is installed, default `/usr/share`.

Usage
=====

Simply include the recipe where you want Tomcat installed.

Due to the ways that some system init scripts call the configuration,
you may wish to set the java options to include `JAVA_OPTS`. As an
example for a java app server role:

    name "java-app-server"
    run_list("recipe[tomcat7]")
    override_attributes(
      'tomcat' => {
        'java_options' => "${JAVA_OPTS} -Xmx128M -Djava.awt.headless=true"
      }
    )

License and Author
==================

Author:: gbloquel

Copyright:: 2012

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.