'use strict';

angular.module('registrarApp')
    .controller('SubjectDetailController', function ($scope, $stateParams, Subject) {
        $scope.subject = {};
        $scope.load = function (id) {
            Subject.get({id: id}, function(result) {
              $scope.subject = result;
            });
        };
        $scope.load($stateParams.id);
    });
