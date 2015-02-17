'use strict';

angular.module('registrarApp')
    .factory('Teacher', function ($resource, $cacheFactory) {

        var url = 'api/teachers/:id';
        var cache = $cacheFactory.get('$http');

        var interceptor = {
            response: function (response) {
                cache.remove(response.config.url);
                console.log('cache removed', response.config.url);
                return response;
            }
        };

        // should be done on server side
        var truncate = function(obj, depth) {
            for (var property in obj) {
                if (property == 'id' || property == 'name') {
                    continue;
                }
                if (depth == 0) {
                    var p = obj[property];
                    if (p && p.constructor === Array) {
                        obj[property] = [];
                    } else {
                        obj[property] = null;
                    }
                } else {
                    truncate(obj[property], depth -1);
                }
            }
        };

        return $resource(url, {}, {
            'query'  : { method: 'GET', isArray: true, cache: cache,
                transformResponse: function (data) {
                    var obj = JSOG.parse(data);
                    for (var i = 0; i < obj.length; i++) {
                        truncate(obj[i], 0);
                    }
                    console.log(obj);
                    return obj;
                }},
            'remove' : { method: 'DELETE', cache: interceptor },
            'delete' : { method: 'DELETE', cache: interceptor },
            'post'   : { method: 'POST', cache: interceptor },
            'get'    : { method: 'GET', cache: cache,
                transformResponse: function (data) {
                    var obj = JSOG.parse(data);
                    truncate(obj, 2);
                    console.log(obj);
                    return obj;
                }
            }
        });
    });

