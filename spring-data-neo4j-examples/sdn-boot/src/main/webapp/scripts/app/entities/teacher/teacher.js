'use strict';

angular.module('registrarApp')
    .config(function ($stateProvider) {
        $stateProvider
            .state('teacher', {
                parent: 'entity',
                url: '/teachers',
                data: {
                    roles: ['ROLE_USER']
                },
                views: {
                    'content@': {
                        templateUrl: 'scripts/app/entities/teacher/teachers.html',
                        controller: 'TeacherController'
                    }
                },
                resolve: {
                    translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                        $translatePartialLoader.addPart('teacher');
                        return $translate.refresh();
                    }]
                }
            })
            .state('teacherDetail', {
                parent: 'entity',
                url: '/teachers/:id',
                data: {
                    roles: ['ROLE_USER']
                },
                views: {
                    'content@': {
                        templateUrl: 'scripts/app/entities/teacher/teacher-detail.html',
                        controller: 'TeacherDetailController'
                    }
                },
                resolve: {
                    translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                        $translatePartialLoader.addPart('teacher');
                        return $translate.refresh();
                    }]
                }
            });
    });
