'use strict';

angular.module('registrarApp')
    .controller('DepartmentDetailController', function ($scope, $stateParams, Department) {
        $scope.department = {};
        $scope.load = function (id) {
            Department.get({id: id}, function(result) {
                $scope.department = result;
            });
        };
        $scope.load($stateParams.id);
    });
