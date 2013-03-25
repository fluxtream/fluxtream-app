maintainer       "Vladislav Mikhaylov"
maintainer_email "solarvm@gmail.com"
license          "AGPL"
description      "Installs/Configures tomcat7 binary distrib"
long_description IO.read(File.join(File.dirname(__FILE__), 'README.md'))
version          "0.0.3"

recipe "tomcat7::default", "Installs and configures Tomcat7"

%w{ debian ubuntu centos redhat fedora }.each do |os|
  supports os
end