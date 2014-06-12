var developerWebsiteApp = angular.module('developerWebsiteApp', [
    'ngRoute',
    'developerWebsiteControllers',
    'developerWebsiteServices'
]);

developerWebsiteApp.config(['$routeProvider', '$locationProvider',
    function($routeProvider) {
        $routeProvider.
            when('/apps', {
                templateUrl: 'partners/partials/apps-list',
                controller: 'AppsListCtrl'
            }).
            when('/app-edit', {
                templateUrl: 'partners/partials/app-form',
                controller: 'AppEditFormCtrl'
            }).
            when('/app-create', {
                templateUrl: 'partners/partials/app-form',
                controller: 'AppCreateFormCtrl'
            }).
            when('/user-manual', {
                templateUrl: 'partials/user-manual'
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