'use strict';

angular.module('registrarApp')
    .controller('NavbarController', function ($scope, $location, $state, Auth, Principal) {
        $scope.isAuthenticated = true;//Principal.isAuthenticated;
        $scope.isInRole = true; //Principal.isInRole;
        $scope.$state = $state;

        $scope.logout = function () {
            Auth.logout();
            $state.go('home');
        };
    });
