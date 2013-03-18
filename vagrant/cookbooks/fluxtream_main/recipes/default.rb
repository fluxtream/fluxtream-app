user "fluxtream" do
  comment "Fluxtream user"
  gid "users"
  home "/home/fluxtream"
  shell "/bin/bash"
  password "fluxtream"
end

mysql_database 'flx' do
  connection ({:host => "localhost", :username => 'root', :password => node['mysql']['server_root_password']})
  action :create
end