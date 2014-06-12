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
            when('/user-manual', {
                templateUrl: 'partials/user-manual'
            }).
            when('/api-docs', {
                templateUrl: 'partials/api-docs'
            }).
            otherwise({
                redirectTo: '/api-docs'
            });
    }
]);