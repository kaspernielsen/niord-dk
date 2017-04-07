<#include "common.ftl"/>

<@defaultSubjectFieldTemplates/>

<field-template field="part.getDesc('da').details" format="html">
    <#list params.positions as pos>
        <@renderAtonType atonParams=pos defaultName="Et dagsømærke" format="long" lang="da"/>
        er blevet etableret <@renderPositionList geomParam=pos lang="da"/>.<br>
    </#list>
</field-template>

<field-template field="part.getDesc('en').details" format="html">
    <#list params.positions as pos>
        <@renderAtonType atonParams=pos defaultName="A buoy" format="long" lang="en"/>
        has been established <@renderPositionList geomParam=pos lang="en"/>.<br>
    </#list>
</field-template>

<#if promulgate('audio')>
    <field-template field="message.promulgation('audio').text" update="append">
        <#list params.positions as pos>
            <@line>
                <@renderAtonType atonParams=pos defaultName="Et dagsømærke" format="long" lang="da"/>
                er blevet etableret <@renderPositionList geomParam=pos format="audio" lang="da"/>.
            </@line>
        </#list>
    </field-template>
</#if>

<#if promulgate('navtex')>
    <field-template field="message.promulgation('navtex').text" update="append">
        <#list params.positions as pos>
            <@line format="navtex">
                <@renderAtonType atonParams=pos defaultName="A buoy" format="short" lang="en"/>
                ESTABLISHED <@renderPositionList geomParam=pos format="navtex" lang="en"/>.
            </@line>
        </#list>
    </field-template>
</#if>
