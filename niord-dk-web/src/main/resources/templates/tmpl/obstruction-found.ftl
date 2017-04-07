<#include "common.ftl"/>

<#macro obstructionFound daDetails="" enDetails="" enNavtex="">

    <#assign daObj=daDetails?split("|")/>
    <#assign enObj=enDetails?split("|")/>
    <#assign navtexObj=enNavtex?split("|")/>
    <#assign offset=multiplePositions(part)?then(2,0)/>

    <@defaultSubjectFieldTemplates/>

    <field-template field="part.getDesc('da').details" format="html">
        ${daObj[0+offset]?cap_first} er fundet
        <@renderPositionList geomParam=part lang="da" plural=true/>.
        ${daObj[1+offset]?cap_first} kan være farlig.<br>
        Skibsfarten tilrådes at holde godt klar.
    </field-template>

    <field-template field="part.getDesc('en').details" format="html">
        ${enObj[0+offset]?cap_first} has been reported
        <@renderPositionList geomParam=part lang="en" plural=true/>.
        ${enObj[1+offset]?cap_first} may be dangerous.<br>
        Mariners are advised to keep well clear.
    </field-template>

    <#if promulgate('audio')>
        <field-template field="message.promulgation('audio').text" update="append">
            <@line>
                ${daObj[0+offset]?cap_first} er fundet
                <@renderPositionList geomParam=part lang="da" format="audio" plural=true/>.
                ${daObj[1+offset]?cap_first} kan være farlig.
            </@line>
            <@line>
                Skibsfarten tilrådes at holde godt klar.
            </@line>
        </field-template>
    </#if>

    <#if promulgate('navtex')>
        <field-template field="message.promulgation('navtex').text" update="append">
            <@line format="navtex">
                ${navtexObj[0+offset]} REPORTED FOUND
                <@renderPositionList geomParam=part lang="en" format="navtex" plural=true/>.
                ${navtexObj[1+offset]} MAY BE DANGEROUS.
            </@line>
            <@line format="navtex">
                MARINERS ADVISED TO KEEP CLEAR.
            </@line>
        </field-template>
    </#if>

</#macro>
