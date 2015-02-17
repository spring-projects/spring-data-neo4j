'use strict';

angular.module('registrarApp')
    .config(function ($stateProvider) {
        $stateProvider
            .state('class', {
                parent: 'entity',
                url: '/classes',
                data: {
                    roles: ['ROLE_USER']
                },
                views: {
                    'content@': {
                        templateUrl: 'scripts/app/entities/class/classes.html',
                        controller: 'ClassController'
                    }
                },
                resolve: {
                    translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                        $translatePartialLoader.addPart('class');
                        return $translate.refresh();
                    }]
                }
            })
            .state('classDetail', {
                parent: 'entity',
                url: '/classes/:id',
                data: {
                    roles: ['ROLE_USER']
                },
                views: {
                    'content@': {
                        templateUrl: 'scripts/app/entities/class/class-detail.html',
                        controller: 'ClassDetailController'
                    }
                },
                resolve: {
                    translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                        $translatePartialLoader.addPart('class');
                        return $translate.refresh();
                    }]
                }
            });
    });
