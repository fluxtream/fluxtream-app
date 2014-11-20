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
            when('/general', {
                templateUrl: 'partials/manuals/general'
            }).
            when('/authorization', {
                templateUrl: 'partials/manuals/authorization'
            }).
            when('/api-docs', {
                templateUrl: 'partials/api-docs',
                controller: 'ApiDocsCtrl'
            }).
            when('/widgets-guide', {
                templateUrl: 'partials/widgets-guide'
            }).
            otherwise({
                redirectTo: '/apps'
            });
    }
]);