'use strict';

angular.module('registrarApp')
    .controller('StudentDetailController', function ($scope, $stateParams, Student) {
        $scope.student = {};
        $scope.load = function (id) {
            Student.get({id: id}, function(result) {
              $scope.student = result;
            });
        };
        $scope.load($stateParams.id);
    });
