'use strict';

angular.module('registrarApp')
    .config(function ($stateProvider) {
        $stateProvider
            .state('school', {
                parent: 'entity',
                url: '/school',
                data: {
                    roles: ['ROLE_USER']
                },
                views: {
                    'content@': {
                        templateUrl: 'scripts/app/entities/school/schools.html',
                        controller: 'SchoolController'
                    }
                },
                resolve: {
                    translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                        $translatePartialLoader.addPart('school');
                        return $translate.refresh();
                    }]
                }
            })
            .state('schoolDetail', {
                parent: 'entity',
                url: '/school/:id',
                data: {
                    roles: ['ROLE_USER']
                },
                views: {
                    'content@': {
                        templateUrl: 'scripts/app/entities/school/school-detail.html',
                        controller: 'SchoolDetailController'
                    }
                },
                resolve: {
                    translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                        $translatePartialLoader.addPart('school');
                        return $translate.refresh();
                    }]
                }
            });
    });
