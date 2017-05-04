
<#include "../messages/message-support.ftl"/>

<html>
    <head>
        <link rel="stylesheet" type="text/css" href="/css/templates/mail.css">
    </head>
<body>

<h1>
    Farvandsefterretningerne
    ${.now?string["EEEEEEE 'den' d. MMMMMMMMM yyyy"]}
</h1>

<h2>
    Her er Søværnets Operative Kommando med dagens farvandsefterretninger
</h2>

<#list messages as msg>
    <#assign audio=promulgation(msg, 'audio')!>
    <#if audio?? && audio.text?has_content>
    <p>
        <#if msg.areas?has_content>
            <u>
                <@areaLineage area=msg.areas[0] />
                <#assign msgDesc=descForLang(msg, language)!>
                <#if msgDesc?? && msgDesc.vicinity?has_content>
                    - ${msgDesc.vicinity}
                </#if>
            </u>
            <br>
        </#if>
        <@txtToHtml text=audio.text/>
    </p>
    </#if>
</#list>

</body>
</html>
