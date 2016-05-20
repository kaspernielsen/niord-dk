
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

            'FOOTER_COPYRIGHT': '&copy; 2016 Danish Maritime Authority',
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
            'MENU_FILTER_DOMAIN' : 'Domain',
            'MENU_FILTER_MESSAGESERIES' : 'Message Series',
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
            'MENU_ACTION_CLEAR_SELECTION' : 'Clear selection',
            'MENU_ACTION_TAGS' : 'Tags...',

            'FIELD_REFERENCE' : 'Reference',
            'FIELD_TIME' : 'Time',
            'FIELD_LOCATION' : 'Location',
            'FIELD_DETAILS' : 'Details',
            'FIELD_ATTACHMENTS' : 'Attachments',
            'FIELD_NOTE' : 'Note',
            'FIELD_CHARTS' : 'Charts',
            'FIELD_PUBLICATION' : 'Publication',
            'REF_REPETITION' : '(repetition)',
            'REF_CANCELLED' : '(cancelled)',
            'REF_UPDATED' : '(updated)',
            'SHOW_POS' : 'Show positions',
            'HIDE_POS' : 'Hide positions',
            'GENERAL_MSGS' : 'General Notifications'

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

            'FOOTER_COPYRIGHT': '&copy; 2016 Søfartsstyrelsen',
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
            'MENU_FILTER_DOMAIN' : 'Domæne',
            'MENU_FILTER_MESSAGESERIES' : 'Besked-serier',
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
            'MENU_ACTION_CLEAR_SELECTION' : 'Vælg ingen',
            'MENU_ACTION_TAGS' : 'Mærkater...',

            'FIELD_REFERENCE' : 'Reference',
            'FIELD_TIME' : 'Tid',
            'FIELD_LOCATION' : 'Placering',
            'FIELD_DETAILS' : 'Detaljer',
            'FIELD_ATTACHMENTS' : 'Vedhæftninger',
            'FIELD_NOTE' : 'Note',
            'FIELD_CHARTS' : 'Søkort',
            'FIELD_PUBLICATION' : 'Publikation',
            'REF_REPETITION' : '(gentagelse)',
            'REF_CANCELLED' : '(udgår)',
            'REF_UPDATED' : '(ajourført)',
            'SHOW_POS' : 'Vis positioner',
            'HIDE_POS' : 'Skjul positioner',
            'GENERAL_MSGS' : 'Generelle notifikationer'

        });

        $translateProvider.preferredLanguage('da');

    }]);

