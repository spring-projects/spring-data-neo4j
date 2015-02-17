'use strict';

angular.module( 'registrarApp' )
    .controller( 'StudyBuddyController', function ( $scope, $state, StudyBuddy )
    {
        $scope.studyBuddies = [];

        $scope.loadAll = function ()
        {
            StudyBuddy.query( function ( result )
            {
                $scope.studyBuddies = result;
            } );
        };
        $scope.loadAll();

        $scope.create = function ()
        {

            console.log( "saving StudyBuddy" );
            //$scope.truncate($scope.StudyBuddy);
            console.log( $scope.studyBuddy );

            StudyBuddy.save( $scope.studyBuddy,
                function ()
                {
                    $( '#saveStudyBuddyModal' ).modal( 'hide' );
                    $scope.loadAll();
                } );
        };

        $scope.update = function ( id )
        {
            $scope.studyBuddy = StudyBuddy.get( {id: id} );
            $( '#saveStudyBuddyModal' ).modal( 'show' );
        };

        $scope.delete = function ( id )
        {
            $scope.studyBuddy = StudyBuddy.get( {id: id} );
            $( '#deleteStudyBuddyConfirmation' ).modal( 'show' );
        };

        $scope.confirmDelete = function ( id )
        {
            StudyBuddy.delete( {id: id},
                function ()
                {
                    var popup = $( '#deleteStudyBuddyConfirmation' );
                    popup.on( 'hidden.bs.modal', function ( e )
                    {
                        $scope.loadAll();
                        $state.transitionTo( 'StudyBuddy' );
                    } );
                    $scope.clear();
                    popup.modal( 'hide' );
                } );
        };

        $scope.clear = function ()
        {
            $scope.studyBuddy = {};
        };

        $scope.objectifyBuddyOne = function ()
        {
            console.log( "objectified buddy one" );
            $scope.studyBuddy.buddyOne = angular.fromJson( $scope.studyBuddy.buddyOne );
        };

        $scope.objectifyBuddyTwo = function ()
        {
            console.log( "objectified buddy two" );
            $scope.studyBuddy.buddyTwo = angular.fromJson( $scope.studyBuddy.buddyTwo );
        };

        $scope.objectifyCourse = function ()
        {
            console.log( "objectified course" );
            $scope.studyBuddy.course = angular.fromJson( $scope.studyBuddy.course );
        };
    } );
