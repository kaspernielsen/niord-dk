
/**
 * The site admin functionality.
 */
angular.module('niord.admin')

    /**
     * Legacy NW import Controller
     */
    .controller('NwIntegrationCtrl', ['$scope', '$http', 'growl',
        function ($scope, $http, growl) {
            'use strict';

            $scope.nwTestDatabaseResult = '';


            /** Displays the error message */
            $scope.displayError = function (err) {
                growl.error("Error");
                $scope.nwTestDatabaseResult = 'Error:\n' + err;
            };


            /** Tests the legacy NW database connection */
            $scope.testConnection = function() {
                $scope.nwTestDatabaseResult = 'Trying to connect...';
                $http.get('/rest/import/nw/test-connection')
                    .success(function (result) {
                        $scope.nwTestDatabaseResult = 'Connection status: ' + result;
                    })
                    .error($scope.displayError);

            };

        }])


    /**
     * Aton Import Controller
     */
    .controller('AtonIntegrationCtrl', ['$scope',
        function ($scope) {
            'use strict';

            $scope.atonUploadUrl = '/rest/import/atons/upload-xls';
            $scope.importResult = '';

            $scope.xlsFileUploaded = function(result) {
                $scope.importResult = result;
                $scope.$$phase || $scope.$apply();
            };

            $scope.xlsFileUploadError = function(status, statusText) {
                $scope.importResult = "Error importing AtoNs (error " + status + ")";
                $scope.$$phase || $scope.$apply();
            };

        }]);
