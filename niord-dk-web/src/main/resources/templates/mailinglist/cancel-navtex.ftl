
<#include "../messages/message-support.ftl"/>

<#if message??>
    CANCEL DANISH NAV WARN
    <@renderNumberYearId message=message defaultValue=message.shortId!''></@renderNumberYearId>
</#if>
