'use strict';

angular.module('registrarApp')
    .controller('TeacherController', function ($scope, $state, Teacher) {
        $scope.teachers = [];
        $scope.loadAll = function() {
            Teacher.query(function(result) {
               $scope.teachers = result;
            });
        };
        $scope.loadAll();

        $scope.create = function () {
            console.log("saving teacher");
            //$scope.truncate($scope.teacher, 2);
            console.log($scope.teacher);
            Teacher.save($scope.teacher,
                function () {
                    $('#saveTeacherModal').modal('hide');
                    $scope.loadAll();
                });
        };

        $scope.update = function (id) {
            $scope.teacher = Teacher.get({id: id});
            $('#saveTeacherModal').modal('show');
        };

        $scope.delete = function (id) {
            $scope.teacher = Teacher.get({id: id});
            $('#deleteTeacherConfirmation').modal('show');
        };

        $scope.confirmDelete = function (id) {
            Teacher.delete({id: id},
                function () {
                    var popup = $('#deleteTeacherConfirmation');
                    popup.on('hidden.bs.modal', function(e) {
                        $scope.loadAll();
                        $state.transitionTo('teacher');
                    });
                    $scope.clear();
                    popup.modal('hide');
                });
        };

        $scope.clear = function () {
            $scope.teacher = {};
        };

//        $scope.tableParams = new ngTableParams({
//            page: 1,            // show first page
//            count: 10,          // count per page
//            sorting: {
//                name: 'asc'     // initial sorting
//            }
//        }, {
//            total: $scope.teachers.length, // length of data
//            getData: function($defer, params) {
//                // use build-in angular filter
//                var orderedData = params.sorting() ?
//                    $filter('orderBy')(data, params.orderBy()) :
//                    data;
//
//                $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
//            }
//        });



    });
