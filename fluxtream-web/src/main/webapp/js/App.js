define(
		[ "core/FlxState", "libs/jquery.form", "libs/jquery.qtip.min" ],
		function(FlxState) {

			var App = {};
			var toLoad = 0, loaded = 0;
			var apps = {};

			function initialize() {
				_.bindAll(this);
				// start loading all applications
				loadApps();
			}

			/**
			 * Preload all applications dynamically; we do this because a) the
			 * applications list can/will be user-dependent b) we need to
			 * preload in order to setup the (global) router object and (in the
			 * future) to let every application know of the existence of others
			 */
			function loadApps() {
				toLoad = FlxState.apps.length;
				for ( var i = 0; i < FlxState.apps.length; i++) {
					require([ "applications/" + FlxState.apps[i] + "/App" ],
							function(app) {
								apps[app.name] = app;
								app.initialize();
								appLoaded(app.name);
							});
				}
			}

			/**
			 * Add the buttons to the top apps menu
			 */
			function createAppsMenu(appName, appIcon) {
				for ( var i = 0; i < FlxState.apps.length; i++) {
					var app = apps[FlxState.apps[i]];
					$("#apps-menu")
							.append(
									"<button id=\""
											+ app.name
											+ "MenuButton\" class=\"btn\" "
											+ "onclick=\"javascript:App.renderApp('"
											+ app.name + "')\">"
											+ "<i class=\"" + app.icon
											+ "  icon-large\"></i></button>");
				}
			}

			/**
			 * Application-is-loaded callback
			 */
			function appLoaded(appName) {
				// we keep track of how many apps have been loaded
				loaded++;
				// when all apps are loaded...
				if (loaded === toLoad) {
					App.apps = apps;
					// we create the top apps menu
					createAppsMenu();
					// we start the history
					Backbone.history.start({
						pushState : true
					});
					// finally we render the default - or url-specified - app
					renderMainApp();
				}
			}

			/**
			 * Render main app or the one that's specified in the location bar's
			 * contents
			 */
			function renderMainApp() {
				var parse_url = /^(?:([A-Za-z]+):)?(\/{0,3})([0-9.\-A-Za-z]+)(?::(\d+))?(?:\/([^?#]*))?(?:\?([^#]*))?(?:#(.*))?$/;
				var result = parse_url.exec(window.location.href);
				var names = [ 'url', 'scheme', 'slash', 'host', 'port', 'path',
						'query', 'hash' ];
				var blanks = ' ';
				var i;
				var parts = {};
				for (i = 0; i < names.length; i += 1)
					parts[names[i]] = result[i];
				var splits = parts.path.split("/");
				if (splits[0] === "app" && typeof (splits[1]) != "undefined") {
					var appState = parts.path.substring("app/".length
							+ splits[1].length + 1);
					var appName = splits[1];
					FlxState.saveState(appName, appState);
					App.activeApp = apps[appName];
				} else {
					App.activeApp = apps[FlxState.defaultApp];
					apps[FlxState.defaultApp].render("");
				}
			}
			
			function fullHeight() {
				if ($(".fullHeight").length>0) {
					tabsY = $("#tabs").position().top;
					windowHeight = $(window).height();
					footerHeight = $("#footer").height();
					fHeight = (windowHeight-tabsY-footerHeight);
					$(".fullHeight").height(fHeight);
				}
				$(window).resize(function() {
					App.fullHeight();
				});
			}

			function renderApp(appName) {
				App.activeApp.saveState();
				App.activeApp=App.apps[appName];
				App.apps[appName].render("last");
			}

			App.settings = function() {
				$.ajax({
					url : "/settings/main",
					success : function(html) {
						makeModal(html);
						$("#settingsForm").ajaxForm(function() {
							$("#modal").empty();
						});
					}
				});
			};
			
			function makeModal(html) {
				$("#modal").replaceWith(html);
				$("#modal").modal();
			}

			App.eraseEverything = function() {
				var confirmed = confirm("Are you sure?");
			};

			App.connectors = function() {
				$.ajax({
					url : "/connectors/main",
					success : function(html) {
						makeModal(html);
					}
				});
			};

			App.removeConnector = function(api) {
				var c = confirm("If you wrote comments on events related to this connector, "
						+ "you will loose them forever.\n"
						+ "Are your sure you want to continue?");
				if (c) {
					$.ajax({
						url : "/connectors/removeConnector?api=" + api,
						dataType : "json",
						success : function(data) {
							if (data.result == "ok") {
								$("#userConnectors").load(
										"/connectors/userConnectors");
								$("#availableConnectors").load(
										"/connectors/availableConnectors");
								App.showConnectorsPage(0);
							}
						}
					});
				}
			};

			function startsWith(s, prefix) {
				return s.substr(0, prefix.length) === prefix;
			};

			App.addConnector = function(url) {
				if (startsWith(url, "ajax:")) {
					var savedConnectorContent = $(".addConnectorsMain").html();
					$.ajax({
						url : url.substring(5),
						success : function(html) {
							$(".addConnectorsMain").html(html);
							$(".focushere").focus();
						}
					});
				} else {
					var loading = $("#loading").clone().show();
					$(".addConnectorsMain").empty();
					$(".addConnectorsMain").append(loading);
					setTimeout("window.location='" + url + "'", 500);
				}
			};

			App.showConnectorsPage = function(page) {
				$("#availableConnectors").load(
						"/connectors/availableConnectors?page=" + page);
			};

			App.discardNotifications = function() {
				var ids = $("#notificationIds").html();
				$.ajax({
					url : "/notifications/discard?ids=" + ids,
					success : function() {
						$("#notifications").alert("close");
					}
				});
			};
			
			App.showCarousel = function(photoId) {
				if ($("#photosCarousel").length==0) {
					$.ajax({
						url : "/tabs/photos/carousel",
						success: function(html) {
							makeModal(html);
							carousel(photoId);
						}
					});
				} else {
					carousel(photoId);
				}
			};
			
			App.search = function() {
				$(".application").load("/search/0?q=" + $(".search-query").val());
			};
			
			function carousel(photoId) {
				$(".carousel-inner div.item").removeClass("active");
				$(".carousel-inner #photo-"+photoId).addClass("active");
				$("#modal").modal("show");
			}

			App.initialize = initialize;
			App.renderApp = renderApp;
			App.state = FlxState;
			App.fullHeight = fullHeight;
			window.App = App;
			return App;

		});
