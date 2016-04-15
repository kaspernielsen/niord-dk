
/**
 * The site admin functionality.
 */
angular.module('niord.admin')

    /**
     * Legacy NW import Controller
     */
    .controller('NwIntegrationCtrl', ['$scope', '$rootScope', '$http', 'growl',
        function ($scope, $rootScope, $http, growl) {
            'use strict';

            $scope.legacyNwResult = '';

            /** Displays the error message */
            $scope.displayError = function (err) {
                growl.error("Error");
                $scope.legacyNwResult = 'Error:\n' + err;
            };


            /** Tests the legacy NW database connection */
            $scope.testConnection = function() {
                $scope.legacyNwResult = 'Trying to connect...';
                $http.get('/rest/import/nw/test-connection')
                    .success(function (result) {
                        $scope.legacyNwResult = 'Connection status: ' + result;
                    })
                    .error($scope.displayError);

            };

            // Determine the message series for the current domain
            $scope.messageSeriesIds = [];
            if ($rootScope.domain && $rootScope.domain.messageSeries) {
                angular.forEach($rootScope.domain.messageSeries, function (series) {
                    if (series.mainType == 'NW') {
                        $scope.messageSeriesIds.push(series.seriesId);
                    }
                });
            }

            $scope.data = {
                seriesId: $scope.messageSeriesIds.length == 1 ? $scope.messageSeriesIds[0] : undefined,
                tagName: ''
            };


            /** Opens the tags dialog */
            $scope.openTagsDialog = function () {
                $rootScope.$broadcast('messageTags', {});
            };


            /** Flags that a given tag has been selected in the tags dialog */
            $scope.$on('messageTagSelected', function(event, data) {
                $scope.data.tagName = data.tag.tagId;
            });


            /** Imports the active legacy NW messages */
            $scope.importActiveLegacyNw = function () {
                $scope.legacyNwResult = 'Start import of active legacy MW messages';

                $http.post('/rest/import/nw/import-active-nw', $scope.data)
                    .success(function (result) {
                        $scope.legacyNwResult = result;
                    })
                    .error($scope.displayError);
            }

        }])


    /**
     * Legacy NM import Controller
     */
    .controller('NmIntegrationCtrl', ['$scope', '$rootScope', '$http', 'growl',
        function ($scope, $rootScope, $http, growl) {
            'use strict';

            $scope.nmImportUrl = '/rest/import/nm/import-nm';
            $scope.legacyNmResult = '';

            /** Displays the error message */
            $scope.displayError = function (err) {
                growl.error("Error");
                $scope.legacyNmResult = 'Error:\n' + err;
            };


            // Determine the message series for the current domain
            $scope.messageSeriesIds = [];
            if ($rootScope.domain && $rootScope.domain.messageSeries) {
                angular.forEach($rootScope.domain.messageSeries, function (series) {
                    if (series.mainType == 'NM') {
                        $scope.messageSeriesIds.push(series.seriesId);
                    }
                });
            }

            $scope.data = {
                seriesId: $scope.messageSeriesIds.length == 1 ? $scope.messageSeriesIds[0] : undefined,
                tagName: ''
            };


            /** Opens the tags dialog */
            $scope.openTagsDialog = function () {
                $rootScope.$broadcast('messageTags', {});
            };


            /** Flags that a given tag has been selected in the tags dialog */
            $scope.$on('messageTagSelected', function(event, data) {
                $scope.data.tagName = data.tag.tagId;
            });


            /** Called when the NM html file has been imported */
            $scope.nmFileUploaded = function(result) {
                $scope.legacyNmResult = result;
                $scope.$$phase || $scope.$apply();
            };

            /** Called when the NM html import has failed */
            $scope.nmFileUploadError = function(status, statusText) {
                $scope.legacyNmResult = "Error importing NMs (error " + status + ")";
                $scope.$$phase || $scope.$apply();
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
