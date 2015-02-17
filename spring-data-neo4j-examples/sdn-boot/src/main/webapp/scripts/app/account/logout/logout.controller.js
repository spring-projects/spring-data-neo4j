'use strict';

angular.module('registrarApp')
    .controller('LogoutController', function (Auth) {
        Auth.logout();
    });
