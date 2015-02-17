'use strict';

angular.module('registrarApp')
    .controller('SubjectController', function ($scope, $state, Subject) {
        $scope.subjects = [];

        $scope.loadAll = function() {
            Subject.query(function(result) {
                $scope.subjects = result;
            });
        };
        $scope.loadAll();

        $scope.create = function () {

            console.log("saving subject");
            //$scope.truncate($scope.subject);
            console.log($scope.subject);

            Subject.save($scope.subject,
                function () {
                    $('#saveSubjectModal').modal('hide');
                    $scope.loadAll();
                });
        };

        $scope.update = function (id) {
            $scope.subject = Subject.get({id: id});
            $('#saveSubjectModal').modal('show');
        };

        $scope.delete = function (id) {
            $scope.subject = Subject.get({id: id});
            $('#deleteSubjectConfirmation').modal('show');
        };

        $scope.confirmDelete = function (id) {
            Subject.delete({id: id},
                function () {
                    var popup = $('#deleteSubjectConfirmation');
                    popup.on('hidden.bs.modal', function(e) {
                        $scope.loadAll();
                        $state.transitionTo('subject');
                    });
                    $scope.clear();
                    popup.modal('hide');
                });
        };

        $scope.clear = function () {
            $scope.subject = {};
        };
    });
