'use strict';

angular.module('registrarApp')
    .controller('SchoolController', function ($scope, School) {
        $scope.schools = [];
        $scope.loadAll = function() {
            School.query(function(result) {
               $scope.schools = result;
            });
        };
        $scope.loadAll();

        $scope.create = function () {

            console.log("saving school");
            //$scope.truncate($scope.school);
            console.log($scope.school);

            School.save($scope.school,
                function () {
                    $('#saveSchoolModal').modal('hide');
                    $scope.loadAll();
                });
        };

        $scope.update = function (id) {
            $scope.school = School.get({id: id});
            $('#saveSchoolModal').modal('show');
        };

        $scope.delete = function (id) {
            $scope.school = School.get({id: id});
            $('#deleteSchoolConfirmation').modal('show');
        };

        $scope.confirmDelete = function (id) {
            School.delete({id: id},
                function () {
                    $scope.loadAll();
                    $('#deleteSchoolConfirmation').modal('hide');
                    $scope.clear();
                });
        };

        $scope.clear = function () {
            $scope.school = {};
        };
    });
