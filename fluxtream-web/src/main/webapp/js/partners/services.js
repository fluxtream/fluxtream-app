var developerWebsiteServices = angular.module('developerWebsiteServices', ['ngResource']);

developerWebsiteServices.factory('App', ['$resource',
    function($resource){
        return $resource(
            '/api/v1/apps/:uid',
            {uid: '@uid'}
        );
    }
]);