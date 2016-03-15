
/**
 * Common settings.
 * The settings are either defined as constants or set on the root scope
 */
angular.module('niord.conf')

    .run(['$rootScope', '$window', '$translate', 'LangService', 'AppSpaceService',
        function ($rootScope, $window, $translate, LangService, AppSpaceService) {

        $rootScope.adminIntegrationPageEnabled = true;

        // Map settings
        $rootScope.mapDefaultZoomLevel = 6;
        $rootScope.mapDefaultLatitude  = 56;
        $rootScope.mapDefaultLongitude = 11;

        // Layer settings
        $rootScope.wmsLayerEnabled = true;

        // Set current language
        $rootScope.modelLanguages = [ 'da', 'en' ];
        $rootScope.editorLanguages = [ 'da', 'en', 'gl' ];
        $rootScope.siteLanguages = [ 'da', 'en' ];
        // Define which numeral language to use. If language is not supported, create the definition, as per http://numeraljs.com
        $rootScope.numeralLauguages = { 'da': 'da-dk', 'en': 'en' };
        LangService.initLanguage();

        // Update the application spaces
        $rootScope.spaces = [
            {
                id: "1",
                name: "All"
            },
            {
                id: "2",
                name: "NW"
            },
            {
                id: "3",
                name: "NM"
            },
            {
                id: "4",
                name: "NW (Local)"
            },
            {
                id: "5",
                name: "NM (Firing Areas)"
            },
            {
                id: "6",
                name: "NM (Annex)"
            }
        ];
        AppSpaceService.initAppSpace();

    }]);
