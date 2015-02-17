'use strict';

angular.module('registrarApp')
    .config(function ($stateProvider) {
        $stateProvider
            .state('department', {
                parent: 'entity',
                url: '/departments',
                data: {
                    roles: ['ROLE_USER']
                },
                views: {
                    'content@': {
                        templateUrl: 'scripts/app/entities/department/departments.html',
                        controller: 'DepartmentController'
                    }
                },
                resolve: {
                    translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                        $translatePartialLoader.addPart('department');
                        return $translate.refresh();
                    }]
                }
            })
            .state('departmentDetail', {
                parent: 'entity',
                url: '/departments/:id',
                data: {
                    roles: ['ROLE_USER']
                },
                views: {
                    'content@': {
                        templateUrl: 'scripts/app/entities/department/department-detail.html',
                        controller: 'DepartmentDetailController'
                    }
                },
                resolve: {
                    translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                        $translatePartialLoader.addPart('department');
                        return $translate.refresh();
                    }]
                }
            });
    });
