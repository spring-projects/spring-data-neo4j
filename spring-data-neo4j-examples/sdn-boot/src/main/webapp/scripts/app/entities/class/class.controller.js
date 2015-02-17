'use strict';

angular.module('registrarApp')
    .controller('ClassController', function ($scope, $state, Class) {
        $scope.classes = [];
        $scope.loadAll = function() {
            Class.query(function(result) {
               $scope.classes = result;
            });
        };
        $scope.loadAll();

        $scope.objectifySubject = function() {
            console.log("objectified subject");
            $scope.class.subject = angular.fromJson($scope.class.subject);
        };

        $scope.objectifyTeacher = function() {
            console.log("objectified teacher");
            // maybe JSOG?
            $scope.class.teacher = angular.fromJson($scope.class.teacher);
        };


        $scope.create = function () {

            console.log("saving class");
            //$scope.truncate($scope.class, 2);
            console.log($scope.class);

            Class.save($scope.class,
                function () {
                    $('#saveClassModal').modal('hide');
                    $scope.loadAll();
                });
        };

        $scope.update = function (id) {
            $scope.class = Class.get({id: id});

            $('#saveClassModal').modal('show');
        };

        $scope.delete = function (id) {
            $scope.class = Class.get({id: id});

            $('#deleteClassConfirmation').modal('show');
        };

        $scope.confirmDelete = function (id) {
            Class.delete({id: id},
                function () {
                    var popup = $('#deleteClassConfirmation');
                    popup.on('hidden.bs.modal', function(e) {
                        $state.transitionTo('class');
                    });
                    $scope.clear();
                    popup.modal('hide');
                });
        };

        $scope.clear = function () {
            $scope.class = {};
        };
    });
