user "fluxtream" do
  comment "Fluxtream user"
  gid "users"
  home "/home/fluxtream"
  shell "/bin/bash"
  username "fluxtream"
  supports :manage_home => true
  action :create
end

directory "/home/fluxtream" do
  owner "fluxtream"
  group "users"
  mode 00755
  action :create
end

directory "/home/fluxtream/projects" do
  owner "fluxtream"
  group "users"
  mode 00755
  action :create
end
