'use strict';

angular.module('registrarApp')
    .config(function ($stateProvider) {
        $stateProvider
            .state('statistics', {
                parent: 'entity',
                url: '/popularStudyBuddies',
                data: {
                    roles: ['ROLE_USER']
                },
                views: {
                    'content@': {
                        templateUrl: 'scripts/app/entities/statistics/statistics.html',
                        controller: 'StatisticsController'
                    }
                },
                resolve: {
                    translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                        $translatePartialLoader.addPart('statistics');
                        return $translate.refresh();
                    }]
                }
            })
    });
