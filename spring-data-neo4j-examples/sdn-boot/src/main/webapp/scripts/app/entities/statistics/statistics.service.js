'use strict';

angular.module('registrarApp')
    .factory('Statistics', function ($resource, $cacheFactory) {

        var url = 'api/popularStudyBuddies';
        var cache = $cacheFactory.get('$http');

        return $resource(url, {}, {
            'query'  : { method: 'GET', isArray: true, cache: cache,
                transformResponse: function (data) {
                    var obj = JSOG.parse(data);
                    console.log(obj);
                    return obj;
                }
            }
        });
    });
