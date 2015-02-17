'use strict';

angular.module('registrarApp')
    .controller('StudyBuddyDetailController', function ($scope, $stateParams, StudyBuddy) {
        $scope.studyBuddy = {};
        $scope.load = function (id) {
            StudyBuddy.get({id: id}, function(result) {
              $scope.studyBuddy = result;
            });
        };
        $scope.load($stateParams.id);
    });
