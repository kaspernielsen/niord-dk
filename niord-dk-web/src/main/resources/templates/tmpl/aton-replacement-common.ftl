<#include "aton-common.ftl"/>


<#macro renderAtonReplacementType atonParams defaultName format='long' lang='en'>
    <#if atonParams?has_content && atonParams.aton_replacement?has_content>
        <#assign desc=descForLang(atonParams.aton_replacement, lang)!>
        <#if desc?? && format == 'long'>
            ${desc.longValue}
        <#elseif desc??>
            ${desc.value}
        <#else>
            ${defaultName}
        </#if>
    <#else>
        ${defaultName}
    </#if>
</#macro>


<#macro atonReplaced daDefaultName="" daDefaultReplacementName="" enDefaultName="" enDefaultReplacementName="">

    <@defaultSubjectFieldTemplates/>

    <field-template field="part.getDesc('da').details" format="html">
        <#list params.positions![] as pos>
            <@renderAtonType atonParams=pos defaultName="${daDefaultName}" format="long" lang="da"/>
            <@renderPositionList geomParam=pos lang="da"/> er erstattet af
            <@renderAtonReplacementType atonParams=pos defaultName="${daDefaultReplacementName}" format="long" lang="da"/>.<br>
        </#list>
    </field-template>

    <field-template field="part.getDesc('en').details" format="html">
        <#list params.positions![] as pos>
            <@renderAtonType atonParams=pos defaultName="${enDefaultName}" format="long" lang="en"/>
            <@renderPositionList geomParam=pos lang="en"/> has been replaced by
            <@renderAtonReplacementType atonParams=pos defaultName="${enDefaultReplacementName}" format="long" lang="en"/>.<br>
        </#list>
    </field-template>

    <#if promulgate('audio')>
        <field-template field="message.promulgation('audio').text" update="append">
            <#list params.positions![] as pos>
                <@line>
                    <@renderAtonType atonParams=pos defaultName="${daDefaultName}" format="long" lang="da"/>
                    <@renderPositionList geomParam=pos format="audio" lang="da"/> er erstattet af
                    <@renderAtonReplacementType atonParams=pos defaultName="${daDefaultReplacementName}" format="long" lang="da"/>.
                </@line>
            </#list>
        </field-template>
    </#if>

    <#if promulgate('navtex')>
        <field-template field="message.promulgation('navtex').text" update="append">
            <#list params.positions![] as pos>
                <@line format="navtex">
                    <@renderAtonType atonParams=pos defaultName="${enDefaultName}" format="short" lang="en"/>
                    <@renderPositionList geomParam=pos format="navtex" lang="en"/> REPLACED BY
                    <@renderAtonReplacementType atonParams=pos defaultName="${enDefaultReplacementName}" format="navtex" lang="en"/>.
                </@line>
            </#list>
        </field-template>
    </#if>

</#macro>
