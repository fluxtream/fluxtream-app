var developerWebsiteControllers = angular.module('developerWebsiteControllers', []);

developerWebsiteControllers.controller('AppsListCtrl', ['$scope', 'App',
    function ($scope, App) {
        $scope.delete = function(app) {
            app.$delete(function(status){
                $scope.refresh();
            });
        };
        $scope.refresh = function() {
            $scope.apps = App.query();
        }
        $scope.refresh();
    }
]);

developerWebsiteControllers.controller('ApiDocsCtrl', ['$scope',
    function($scope) {

    }
]);

developerWebsiteControllers.controller('AppEditFormCtrl', ['$scope', 'App', '$routeParams', '$location',
    function($scope, App, $routeParams, $location) {
        $scope.createOrUpdate = function(app) {
            app.$save(function(status){
                $location.path("#/apps");
            });
        };
        App.get({uid:$routeParams.uid}, function(app){
            $scope.app = app;
        });
    }
]);

developerWebsiteControllers.controller('AppCreateFormCtrl', ['$scope', 'App', '$location',
    function($scope, App, $location) {
        $scope.createOrUpdate = function(app) {
            var newApp = new App(app);
            newApp.$save(function(status){
                $location.path("#/apps");
            });
        };
    }
]);
