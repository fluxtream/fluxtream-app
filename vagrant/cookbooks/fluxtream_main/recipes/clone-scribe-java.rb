git "clone scribe-java repo" do
	repository "git://github.com/fluxtream/scribe-java.git"
	destination "/home/fluxtream/projects/scribe-java"
	user "fluxtream"
	group "users"
	action :sync
end
