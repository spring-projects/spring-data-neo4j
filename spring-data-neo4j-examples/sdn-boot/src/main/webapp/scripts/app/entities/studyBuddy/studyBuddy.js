'use strict';

angular.module('registrarApp')
    .config(function ($stateProvider) {
        $stateProvider
            .state('studyBuddy', {
                parent: 'entity',
                url: '/studyBuddies',
                data: {
                    roles: ['ROLE_USER']
                },
                views: {
                    'content@': {
                        templateUrl: 'scripts/app/entities/studyBuddy/studyBuddies.html',
                        controller: 'StudyBuddyController'
                    }
                },
                resolve: {
                    translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                        $translatePartialLoader.addPart('studyBuddy');
                        return $translate.refresh();
                    }]
                }
            })
            .state('studyBuddyDetail', {
                parent: 'entity',
                url: '/studyBuddies/:id',
                data: {
                    roles: ['ROLE_USER']
                },
                views: {
                    'content@': {
                        templateUrl: 'scripts/app/entities/studyBuddy/studyBuddy-detail.html',
                        controller: 'StudyBuddyDetailController'
                    }
                },
                resolve: {
                    translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                        $translatePartialLoader.addPart('studyBuddy');
                        return $translate.refresh();
                    }]
                }
            });
    });
