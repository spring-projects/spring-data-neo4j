'use strict';

angular.module('registrarApp')
    .controller('StatisticsController', function ($scope, $state, Statistics) {
        $scope.results = [];
        $scope.loadAll = function() {
            Statistics.query(function(result) {
               $scope.results = result;
            });
        };
        $scope.loadAll();

    });
