
<#include "publish-navwarn-common.ftl"/>

<html>
<head>
    <link rel="stylesheet" type="text/css" href="/css/templates/mail.css">

    <style>
        .message-details p {
            padding: 0;
            margin: 0;
        }
    </style>
</head>
<body>

    <div>
        Danish Navigational Warning <#if message.shortId?has_content>no. ${message.shortId}</#if>
    </div>
    <@renderNavWarnDetails msg=message lang='en'></@renderNavWarnDetails>
    <p>&nbsp;</p>
    <div>
        Dansk navigationsadvarsel <#if message.shortId?has_content>nr. ${message.shortId}</#if>
    </div>
    <@renderNavWarnDetails msg=message lang='da'></@renderNavWarnDetails>

</body>
</html>
