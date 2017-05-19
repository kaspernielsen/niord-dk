
<#include "../messages/message-support.ftl"/>

<#setting time_zone="Europe/Copenhagen">

<html>
<head>
    <link rel="stylesheet" type="text/css" href="/css/templates/mail.css">
</head>
<body>

<h1>
    Danske farvandsefterretninger ${.now?string["EEEEEEE 'den' d. MMMMMMMMM yyyy"]}
</h1>

<h2>
    Her er Forsvarets Operationscenter med dagens farvandsefterretninger
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

<#if params['firingExercises']?? && params['firingExercisesDate']??>
<h2>
    Der afholdes i morgen
    ${params['firingExercisesDate']?string["EEEEEEE 'den' d. MMMMMMMMM yyyy"]}
    følgende skydeøvelser:
</h2>

    <#list params['firingExercises'] as fe>
    <p>
        Ved <@areaLineage area=fe.area />
        <#list fe.times as t>
            <#if !t?is_first>og igen</#if>
            fra kl ${t.fromDate?string["HHmm"]}
            til kl ${t.toDate?string["HHmm"]}<#if t?is_last>.</#if>
        </#list>
    </p>
    </#list>
</#if>

<p>
    Det var farvandsefterretningerne.
</p>

</body>
</html>
