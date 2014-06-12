var developerWebsiteControllers = angular.module('developerWebsiteControllers', []);

developerWebsiteControllers.controller('SignUpCtrl', ['$scope', '$http', '$parse', '$location',
    function($scope, $http, $parse, $location) {
        $scope.createAccount = function(developer) {
            $http({
                url: "createAccount",
                method: "POST",
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                data: $.param(developer)
            }).success(function(data) {
                if (data.status==="invalid") {
                    var fields = data.fields;
                    console.log(fields);
                    for(var fieldName in fields) {
                        var serverMessage = $parse('createAccountForm.' + fieldName + '.$error.serverMessage');
                        if (fields[fieldName]==="valid") {
                            $scope.createAccountForm.$setValidity(fieldName, true, $scope.createAccountForm);
                            serverMessage.assign($scope, null);
                        } else {
                            $scope.createAccountForm.$setValidity(fieldName, false, $scope.createAccountForm);
                            serverMessage.assign($scope, fields[fieldName]);
                        }
                    }
                } else {
                    $location.path("/sign-in");
                }
            }).error(function(data, status) {
                $scope.status = status;
            });
        }
        $scope.developer = {};
   }
]);


developerWebsiteControllers.controller('SignInCtrl', ['$scope', '$http', '$parse',
    function($scope, $http, $parse) {
        $scope.signIn = function(credentials) {
            $http({
                url: "signIn",
                method: "POST",
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                data: $.param(credentials)
            }).success(function(data) {
                if (data.authd) {
                    window.location = "/dev/home";
                } else {
                    var serverMessage = $parse('signInForm.f_password.$error.serverMessage');
                    $scope.signInForm.$setValidity("f_password", false, $scope.signInForm);
                    $("#password-form-group").addClass("has-error");
                    $("#username-form-group").addClass("has-error");
                    serverMessage.assign($scope, data.message);
                }
            }).error(function(data, status) {
                $scope.status = status;
            });
        }
        $scope.credentials = {};
    }
]);
