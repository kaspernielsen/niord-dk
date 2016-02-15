
/**
 * Translations.
 * Danish added
 */
angular.module('niord.conf')

    .config(['$translateProvider', function ($translateProvider) {

        $translateProvider.useSanitizeValueStrategy('sanitize');

        $translateProvider.translations('en', {
            'LANG_EN' : 'English',
            'LANG_DA' : 'Danish',

            'MENU_BRAND': 'Niord',
            'MENU_MESSAGES': 'Messages',
            'MENU_ATONS': 'AtoNs',
            'MENU_EDITOR': 'Editor',
            'MENU_ADMIN': 'Admin',
            'MENU_LOGIN': 'Login',
            'MENU_LOGOUT': 'Logout',

            'FOOTER_COPYRIGHT': '&copy; 2015 Danish Maritime Authority',
            'FOOTER_LINK': 'http://www.soefartsstyrelsen.dk/',
            'FOOTER_DISCLAIMER': 'Disclaimer',
            'FOOTER_COOKIES': 'Cookies',


            'FRONT_PAGE_WELCOME': 'Welcome to the',
            'FRONT_PAGE_BRAND': 'Nautical Information Directory',

            'VIEW_MODE_GRID': 'Grid',
            'VIEW_MODE_DETAILS': 'Details',
            'VIEW_MODE_TABLE': 'Table',
            'VIEW_MODE_MAP': 'Map',
            'VIEW_MODE_SELECTION': 'Selection',
            'MENU_FILTER' : 'Filter',
            'MENU_FILTER_TEXT' : 'Text',
            'MENU_FILTER_TYPE' : 'Type',
            'MENU_FILTER_STATUS' : 'Status',
            'MENU_FILTER_CATEGORY' : 'Category',
            'MENU_FILTER_TAG' : 'Tag',
            'MENU_FILTER_ATON' : 'AtoN',
            'MENU_FILTER_CHART' : 'Chart',
            'MENU_FILTER_AREA' : 'Area',
            'MENU_FILTER_DATE' : 'Date',
            'MENU_FILTER_SAVE' : 'Save Filter...',
            'MENU_FILTER_CLEAR' : 'Clear Filter',
            'MENU_ACTION' : 'Action',
            'MENU_ACTION_PDF' : 'Generate PDF',
            'MENU_ACTION_SELECT_ALL' : 'Select all',
            'MENU_ACTION_CLEAR_SELECTION' : 'Clear selection'
        });

        $translateProvider.translations('da', {
            'LANG_EN' : 'Engelsk',
            'LANG_DA' : 'Dansk',

            'MENU_BRAND': 'Niord',
            'MENU_MESSAGES': 'Beskeder',
            'MENU_ATONS': 'Afmærkninger',
            'MENU_EDITOR': 'Editor',
            'MENU_ADMIN': 'Admin',
            'MENU_LOGIN': 'Log ind',
            'MENU_LOGOUT': 'Log ud',

            'FOOTER_COPYRIGHT': '&copy; 2015 Søfartsstyrelsen',
            'FOOTER_LINK': 'http://www.soefartsstyrelsen.dk/',
            'FOOTER_DISCLAIMER': 'Disclaimer',
            'FOOTER_COOKIES': 'Cookies',


            'FRONT_PAGE_WELCOME': 'Velkommen til',
            'FRONT_PAGE_BRAND': 'Nautical Information Directory',

            'VIEW_MODE_GRID': 'Gitter',
            'VIEW_MODE_DETAILS': 'Detaljer',
            'VIEW_MODE_TABLE': 'Tabel',
            'VIEW_MODE_MAP': 'Kort',
            'VIEW_MODE_SELECTION': 'Valgte',
            'MENU_FILTER' : 'Filter',
            'MENU_FILTER_TEXT' : 'Tekst',
            'MENU_FILTER_TYPE' : 'Type',
            'MENU_FILTER_STATUS' : 'Status',
            'MENU_FILTER_CATEGORY' : 'Kategori',
            'MENU_FILTER_TAG' : 'Mærkat',
            'MENU_FILTER_ATON' : 'Afmærkning',
            'MENU_FILTER_CHART' : 'Søkort',
            'MENU_FILTER_AREA' : 'Område',
            'MENU_FILTER_DATE' : 'Dato',
            'MENU_FILTER_SAVE' : 'Gem Filter...',
            'MENU_FILTER_CLEAR' : 'Nulstil Filter',
            'MENU_ACTION' : 'Handling',
            'MENU_ACTION_PDF' : 'Generer PDF',
            'MENU_ACTION_SELECT_ALL' : 'Vælg alle',
            'MENU_ACTION_CLEAR_SELECTION' : 'Vælg ingen'
        });

        $translateProvider.preferredLanguage('da');

    }]);

