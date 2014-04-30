var developerWebsiteServices = angular.module('developerWebsiteServices', ['ngResource']);

developerWebsiteServices.factory('App', ['$resource',
    function($resource){
        return $resource(
            '/dev/api/apps/:uid',
            {uid: '@uid'}
        );
    }
]);