#
# Cookbook Name:: tomcat7
# Attributes:: default
#
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

default[:tomcat7][:version] = "7.0.27"
default[:tomcat7][:user] = "tomcat"
default[:tomcat7][:group] = "tomcat"
default[:tomcat7][:target] = "/usr/share"
default[:tomcat7][:port] = 8080
default[:tomcat7][:ssl_port] = 8443
default[:tomcat7][:ajp_port] = 8009
default[:tomcat7][:java_options] = " -Xmx128M -Dajva.awt.headless=true"
default[:tomcat7][:use_security_manager] = "no"

##
set[:tomcat7][:home] = "#{tomcat7['target']}/tomcat"
set[:tomcat7][:base] = "#{tomcat7['target']}/tomcat"
set[:tomcat7][:config_dir] = "#{tomcat7['target']}/tomcat/conf"
set[:tomcat7][:log_dir] = "#{tomcat7['target']}/tomcat/logs"
