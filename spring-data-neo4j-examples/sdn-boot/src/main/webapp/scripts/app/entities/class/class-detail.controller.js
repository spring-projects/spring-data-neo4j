'use strict';

angular.module('registrarApp')
    .controller('ClassDetailController', function ($scope, $stateParams, Class) {
        $scope.class = {};
        $scope.load = function (id) {
            Class.get({id: id}, function(result) {
              $scope.class = result;
            });
        };
        $scope.load($stateParams.id);
    });
