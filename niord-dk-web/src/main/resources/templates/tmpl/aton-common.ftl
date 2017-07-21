<#include "common.ftl"/>

<#macro aton defaultSubjectFields=true daDefaultName="" daDetails="" enDefaultName="" enDetails="" enNavtex="">

    <#if defaultSubjectFields>
        <@defaultSubjectFieldTemplates/>
    </#if>

    <field-template field="part.getDesc('da').details" format="html">
        <#list params.positions![] as pos>
            <@renderAtonType atonParams=pos defaultName="${daDefaultName}" format="long" lang="da"/>
            <@renderPositionList geomParam=pos lang="da"/> ${daDetails}.<br>
        </#list>
    </field-template>

    <field-template field="part.getDesc('en').details" format="html">
        <#list params.positions![] as pos>
            <@renderAtonType atonParams=pos defaultName="${enDefaultName}" format="long" lang="en"/>
            <@renderPositionList geomParam=pos lang="en"/> ${enDetails}.<br>
        </#list>
    </field-template>

    <#if promulgate('audio')>
        <field-template field="message.promulgation('audio').text" update="append">
            <#list params.positions![] as pos>
                <@line>
                    <@renderAtonType atonParams=pos defaultName="${daDefaultName}" format="long" lang="da"/>
                    <@renderPositionList geomParam=pos format="audio" lang="da"/> ${daDetails}.
                </@line>
            </#list>
        </field-template>
    </#if>

    <#if promulgate('navtex')>
        <field-template field="message.promulgation('navtex').text" update="append">
            <#list params.positions![] as pos>
                <@line format="navtex">
                    <@renderAtonType atonParams=pos defaultName="${enDefaultName}" format="short" lang="en"/>
                    <@renderPositionList geomParam=pos format="navtex" lang="en"/> ${enNavtex}.
                </@line>
            </#list>
        </field-template>
    </#if>

</#macro>
