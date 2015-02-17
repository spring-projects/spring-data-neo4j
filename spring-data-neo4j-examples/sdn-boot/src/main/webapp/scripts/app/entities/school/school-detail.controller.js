'use strict';

angular.module('registrarApp')
    .controller('SchoolDetailController', function ($scope, $stateParams, School) {
        $scope.school = {};
        $scope.load = function (id) {
            School.get({id: id}, function(result) {
              $scope.school = result;
            });
        };
        $scope.load($stateParams.id);
    });
