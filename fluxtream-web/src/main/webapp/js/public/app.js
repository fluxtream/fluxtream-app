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
            when('/sign-up', {
                templateUrl: 'partials/sign-up',
                controller: 'SignUpCtrl'
            }).
            when('/general', {
                templateUrl: 'partials/manuals/general'
            }).
            when('/authorization', {
                templateUrl: 'partials/manuals/authorization'
            }).
            when('/api-docs', {
                templateUrl: 'partials/api-docs'
            }).
            when('/widgets-guide', {
                templateUrl: 'partials/widgets-guide'
            }).
            otherwise({
                redirectTo: '/api-docs'
            });
    }
]);