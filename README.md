Read me First
-------------

Fluxtream is based on Spring(-MVC) 3.1, Spring Security 3.1 and JPA for the persistence. We use [Jersey (JAX-WS)](https://jersey.java.net/) for exposing our [REST API](https://fluxtream.org/dev/#/api-docs)

This is a maven project with 3 sub-modules.

* fluxtream-core: persistence, services and support classes for the other modules (e.g. connectors); [public API](https://fluxtream.org/dev/#/api-docs)
* fluxtream-connectors: data import and update modules for various devices (Fitbit, BodyMedia et al.) and services (Google Calendar, LastFM, etc.)
* fluxtream-web: the main web application, that is both the middleware (Spring MVC) and the frontend (jQuery, [backbone](http://backbonejs.org/),... and [Twitter Bootstrap 2.3.2](http://getbootstrap.com/2.3.2/)); also contains the API documentation (using [swagger](https://helloreverb.com/developers/swagger))

Build instructions
------------------

[Build instructions are available at the Fluxtream wiki](https://github.com/fluxtream/fluxtream-app/wiki).

Copyright and license
---------------------

Copyright 2011-2015 The BodyTrack Team @ CMU CREATE Lab & Candide Kemmler

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
