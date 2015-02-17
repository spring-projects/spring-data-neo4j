'use strict';

angular.module('registrarApp')
    .config(function ($stateProvider) {
        $stateProvider
            .state('subject', {
                parent: 'entity',
                url: '/subjects',
                data: {
                    roles: ['ROLE_USER']
                },
                views: {
                    'content@': {
                        templateUrl: 'scripts/app/entities/subject/subjects.html',
                        controller: 'SubjectController'
                    }
                },
                resolve: {
                    translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                        $translatePartialLoader.addPart('subject');
                        return $translate.refresh();
                    }]
                }
            })
            .state('subjectDetail', {
                parent: 'entity',
                url: '/subjects/:id',
                data: {
                    roles: ['ROLE_USER']
                },
                views: {
                    'content@': {
                        templateUrl: 'scripts/app/entities/subject/subject-detail.html',
                        controller: 'SubjectDetailController'
                    }
                },
                resolve: {
                    translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                        $translatePartialLoader.addPart('subject');
                        return $translate.refresh();
                    }]
                }
            });
    });
