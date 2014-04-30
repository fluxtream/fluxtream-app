var developerWebsiteApp = angular.module('developerWebsiteApp', [
    'ngRoute',
    'developerWebsiteControllers',
    'developerWebsiteServices'
]);

developerWebsiteApp.config(['$routeProvider', '$locationProvider',
    function($routeProvider) {
        $routeProvider.
            when('/sign-in', {
                templateUrl: 'partials/sign-in',
                controller: 'SignInCtrl'
            }).
            when('/api-docs', {
                templateUrl: 'partials/api-docs',
                controller: 'ApiDocsCtrl'
            }).
            otherwise({
                redirectTo: '/api-docs'
            });
    }
]);