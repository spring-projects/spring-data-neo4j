'use strict';

angular.module('registrarApp')
    .controller('TeacherDetailController', function ($scope, $stateParams, Teacher) {
        $scope.teacher = {};
        $scope.load = function (id) {
            Teacher.get({id: id}, function(result) {
                console.log(result);
              $scope.teacher = result;
            });
        };
        $scope.load($stateParams.id);
    });
