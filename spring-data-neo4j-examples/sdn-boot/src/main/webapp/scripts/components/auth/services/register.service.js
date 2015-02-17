'use strict';

angular.module('registrarApp')
    .factory('Register', function ($resource) {
        return $resource('api/register', {}, {
        });
    });


