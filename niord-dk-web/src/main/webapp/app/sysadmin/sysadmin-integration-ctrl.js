
/*
 * Copyright 2016 Danish Maritime Authority.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

            $scope.data = {
                seriesId: undefined,
                tagId: ''
            };
            $scope.tagData = { tag: undefined };
            $scope.initTagIds = [];


            // Load the default parameters
            $http.get('/rest/import/nw/params')
                .success(function (result) {
                    $scope.data = result;

                    $scope.tagData.tag = undefined;
                    if (result && result.tagId) {
                        $scope.initTagIds.push(result.tagId);
                    }
                });


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
                    if (series.mainType === 'NW') {
                        $scope.messageSeriesIds.push(series.seriesId);
                    }
                });
            }


            // Sync the selected tag with the data.tagId
            $scope.$watch("tagData", function () {
                $scope.data.tagId = $scope.tagData.tag !== undefined ? $scope.tagData.tag.tagId : undefined;
            }, true);


            /** Imports the legacy NW messages */
            $scope.importLegacyNw = function () {
                $scope.legacyNwResult = 'Start import of legacy MW messages';

                $http.post('/rest/import/nw/import-nw', $scope.data)
                    .success(function (result) {
                        $scope.legacyNwResult = result;
                    })
                    .error($scope.displayError);
            }

        }])


    /**
     * Legacy Firing Area import Controller
     */
    .controller('FaIntegrationCtrl', ['$scope', '$rootScope', '$http', 'growl',
        function ($scope, $rootScope, $http, growl) {
            'use strict';

            $scope.legacyFaResult = '';
            $scope.tagData = { tag: undefined };

            // Determine the message series for the current domain
            $scope.messageSeriesIds = [];
            if ($rootScope.domain && $rootScope.domain.messageSeries) {
                angular.forEach($rootScope.domain.messageSeries, function (series) {
                    if (series.mainType === 'NM') {
                        $scope.messageSeriesIds.push(series.seriesId);
                    }
                });
            }
            $scope.data = {
                seriesId: $scope.messageSeriesIds.length === 1 ? $scope.messageSeriesIds[0] : undefined,
                tagId: ''
            };

            /** Displays the error message */
            $scope.displayError = function (data, status) {
                var error = "Error (code " + status + ")";
                growl.error(error);
                $scope.legacyFaResult = error;
            };


            // Sync the selected tag with the data.tagId
            $scope.$watch("tagData", function () {
                $scope.data.tagId = $scope.tagData.tag !== undefined ? $scope.tagData.tag.tagId : undefined;
            }, true);


            /** Tests the legacy NW database connection - also used for firing area imports */
            $scope.testConnection = function() {
                $scope.legacyFaResult = 'Trying to connect...';
                $http.get('/rest/import/nw/test-connection')
                    .success(function (result) {
                        $scope.legacyFaResult = 'Connection status: ' + result;
                    })
                    .error($scope.displayError);

            };

            /** Imports the legacy firing areas */
            $scope.importLegacyFa = function () {
                $scope.legacyFaResult = 'Start import of legacy firing areas';

                $http.post('/rest/import/fa/import-fa')
                    .success(function (result) {
                        $scope.legacyFaResult = result;
                    })
                    .error($scope.displayError);
            };


            /** Imports the legacy firing area schedule */
            $scope.importLegacyFaSchedule = function () {
                $scope.legacyFaResult = 'Start import of legacy firing areas';

                $http.post('/rest/import/fa/import-fa-schedule')
                    .success(function (result) {
                        $scope.legacyFaResult = result;
                    })
                    .error($scope.displayError);
            };


            // Load whether or not to auto import firing exercise schedules
            $scope.autoImportFaSchedule = false;
            $http.get('/rest/import/fa/auto-import-fa-schedule')
                .success(function (result) {
                    $scope.autoImportFaSchedule = result;
                });


            /** Called to save the schedule auto-import setting **/
            $scope.updateAutoImportFaSchedule = function () {
                $http.post('/rest/import/fa/auto-import-fa-schedule', $scope.autoImportFaSchedule)
                    .success(function (result) {
                        $scope.legacyFaResult = 'Updated auto-import setting: ' + result;
                    })
                    .error($scope.displayError);
            };


            /** Generates message templates for all firing areas */
            $scope.generateFaTemplates = function () {
                $scope.legacyFaResult = 'Start generating firing area template messages';

                $http.post('/rest/import/fa/generate-fa-messages', $scope.data)
                    .success(function (result) {
                        $scope.legacyFaResult = result;
                    })
                    .error($scope.displayError);
            }

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

            $scope.xlsFileUploadError = function(status) {
                $scope.importResult = "Error importing AtoNs (error " + status + ")";
                $scope.$$phase || $scope.$apply();
            };

        }]);
