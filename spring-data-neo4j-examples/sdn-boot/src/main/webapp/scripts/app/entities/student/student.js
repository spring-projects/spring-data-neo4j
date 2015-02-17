'use strict';

angular.module('registrarApp')
    .config(function ($stateProvider) {
        $stateProvider
            .state('student', {
                parent: 'entity',
                url: '/students',
                data: {
                    roles: ['ROLE_USER']
                },
                views: {
                    'content@': {
                        templateUrl: 'scripts/app/entities/student/students.html',
                        controller: 'StudentController'
                    }
                },
                resolve: {
                    translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                        $translatePartialLoader.addPart('student');
                        return $translate.refresh();
                    }]
                }
            })
            .state('studentDetail', {
                parent: 'entity',
                url: '/students/:id',
                data: {
                    roles: ['ROLE_USER']
                },
                views: {
                    'content@': {
                        templateUrl: 'scripts/app/entities/student/student-detail.html',
                        controller: 'StudentDetailController'
                    }
                },
                resolve: {
                    translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                        $translatePartialLoader.addPart('student');
                        return $translate.refresh();
                    }]
                }
            });
    });
