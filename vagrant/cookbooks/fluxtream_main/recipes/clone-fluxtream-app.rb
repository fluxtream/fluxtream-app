git "clone fluxtream-app repo" do
	repository "git://github.com/fluxtream/fluxtream-app"
	depth 3
	destination "/home/fluxtream/projects/fluxtream-app"
	user "fluxtream"
	group "users"
	action :sync
end

execute "checkout idempotent_updates" do
  command "git checkout idempotent_updates"
  cwd "/home/fluxtream/projects/fluxtream-app"
  user "fluxtream"
  action :run
end

execute "install maven dependencies" do
  command "/home/fluxtream/projects/fluxtream-app/maven/install.sh"
  cwd "/home/fluxtream/projects/fluxtream-app/maven"
  user "fluxtream"
end