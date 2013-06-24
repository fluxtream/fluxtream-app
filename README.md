Read me First
-------------

Fluxtream is based on Spring(-MVC) 3.1, Spring Security 3.1 and JPA for the persistence. We use Jersey (JAX-WS) for exposing the REST API

This is a maven project with 5 sub-modules.

* fluxtream-core: mainly domain classes, persistence, services and support classes for the other modules (e.g. connectors)
* core-webapp: the web API (with Rest-WS annotations), base oauth classes and helper classes
* fluxtream-connectors: lots of them - but for all but the 10 connectors that are actually enabled, there is little more than basic authentication (sometimes even nothing)
* fluxtream-web: a new web application that we are starting to work on, based on twitter bootstrap

Build requirements
------------------

* you will need maven 3.x.x (`sudo apt-get install maven`)
* for interactive development, we use Eclipse Indigo with EE support (/WTP)

Build instructions
------------------

[Build instructions are available elsewhere](https://github.com/fluxtream/fluxtream-app/wiki).

Copyright and license
---------------------

Copyright 2011-2013 The BodyTrack Team @ CMU CREATE Lab & Candide Kemmler

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
