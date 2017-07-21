<#include "common.ftl"/>

<@defaultSubjectFieldTemplates/>

<field-template field="part.getDesc('da').details" format="html">
    <#list params.positions as pos>
        <@renderAtonType atonParams=pos defaultName="En lystønde" format="long" lang="da"/>
        <#if pos.aton_light?has_content>
            visende
            <@lightCharacterFormat light=pos.aton_light format="verbose"  lang="da"/>
        </#if>
        er blevet etableret <@renderPositionList geomParam=pos lang="da"/>.<br>
    </#list>
</field-template>

<field-template field="part.getDesc('en').details" format="html">
    <#list params.positions as pos>
        <@renderAtonType atonParams=pos defaultName="A light buoy" format="long" lang="en"/>
        <#if pos.aton_light?has_content>
            showing
            <@lightCharacterFormat light=pos.aton_light format="verbose"  lang="en"/>
        </#if>
        has been established <@renderPositionList geomParam=pos lang="en"/>.<br>
    </#list>
</field-template>

<#if promulgate('audio')>
    <field-template field="message.promulgation('audio').text" update="append">
        <#list params.positions as pos>
            <@line>
                <@renderAtonType atonParams=pos defaultName="En lystønde" format="long" lang="da"/>
                <#if pos.aton_light?has_content>
                    visende
                    <@lightCharacterFormat light=pos.aton_light format="verbose"  lang="da"/>
                </#if>
                er blevet etableret <@renderPositionList geomParam=pos format="audio" lang="da"/>.
            </@line>
        </#list>
    </field-template>
</#if>

<#if promulgate('navtex')>
    <field-template field="message.promulgation('navtex').text" update="append">
        <#list params.positions as pos>
            <@line format="navtex">
                <@renderAtonType atonParams=pos defaultName="A light buoy" format="short" lang="en"/>
                <#if pos.aton_light?has_content>
                    SHOWING
                    <@lightCharacterFormat light=pos.aton_light format="normal"  lang="en"/>
                </#if>
                ESTABLISHED <@renderPositionList geomParam=pos format="navtex" lang="en"/>.
            </@line>
        </#list>
    </field-template>
</#if>
