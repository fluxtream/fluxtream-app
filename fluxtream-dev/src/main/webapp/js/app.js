var developerWebsiteApp = angular.module('developerWebsiteApp', [
    'ngRoute',
    'developerWebsiteControllers',
    'developerWebsiteServices'
]);

developerWebsiteApp.config(['$routeProvider', '$locationProvider',
    function($routeProvider) {
        $routeProvider.
            when('/apps', {
                templateUrl: 'partials/apps-list',
                controller: 'AppsListCtrl'
            }).
            when('/app-edit', {
                templateUrl: 'partials/app-form',
                controller: 'AppEditFormCtrl'
            }).
            when('/app-create', {
                templateUrl: 'partials/app-form',
                controller: 'AppCreateFormCtrl'
            }).
            when('/api-docs', {
                templateUrl: 'partials/api-docs',
                controller: 'ApiDocsCtrl'
            }).
            otherwise({
                redirectTo: '/apps'
            });
    }
]);