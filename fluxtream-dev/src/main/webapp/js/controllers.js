var developerWebsiteControllers = angular.module('developerWebsiteControllers', []);

developerWebsiteControllers.controller('AppsListCtrl', ['$scope', 'App',
    function ($scope, App) {
        $scope.apps = App.query();
    }
]);

developerWebsiteControllers.controller('ApiDocsCtrl', ['$scope', '$routeParams',
    function($scope, $routeParams) {

    }
]);

developerWebsiteControllers.controller('AppCreateFormCtrl', ['$scope', 'App',
    function($scope, App) {
        $scope.create = function(app) {
            var newApp = new App(app);
            newApp.$save();
        };
    }
]);

developerWebsiteControllers.controller('AppEditFormCtrl', ['$scope', '$routeParams',
    function($scope, $routeParams) {

    }
]);