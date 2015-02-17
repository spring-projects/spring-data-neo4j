'use strict';

angular.module('registrarApp')
    .controller('StudentController', function ($scope, $state, Student) {
        $scope.students = [];

        $scope.loadAll = function() {
            Student.query(function(result) {
               $scope.students = result;
            });
        };
        $scope.loadAll();

        $scope.create = function () {

            console.log("saving student");
            //$scope.truncate($scope.student,2);
            console.log($scope.student);

            Student.save($scope.student,
                function () {
                    $('#saveStudentModal').modal('hide');
                    $scope.loadAll();
                });
        };

        $scope.update = function (id) {
            $scope.student = Student.get({id: id});
            $('#saveStudentModal').modal('show');
        };

        $scope.delete = function (id) {
            $scope.student = Student.get({id: id});
            $('#deleteStudentConfirmation').modal('show');
        };

        $scope.confirmDelete = function (id) {
            Student.delete({id: id},
                function () {
                    var popup = $('#deleteStudentConfirmation');
                    popup.on('hidden.bs.modal', function(e) {
                        $scope.loadAll();
                        $state.transitionTo('student');
                    });
                    $scope.clear();
                    popup.modal('hide');
                });
        };

        $scope.clear = function () {
            $scope.student = {};
        };
    });
