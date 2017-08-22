<#include "common.ftl"/>

<@defaultSubjectFieldTemplates/>

<field-template field="part.getDesc('da').details" format="html">
    Broen
    <#if params.bridge_name?has_content>
        <@quote text=params.bridge_name format="angular"/>
    </#if>
    <@renderPositionList geomParam=part lang="da"/>
    er lukket for gennemsejling der kræver broåbning.
</field-template>

<field-template field="part.getDesc('en').details" format="html">
    The bridge
    <#if params.bridge_name?has_content>
        <@quote text=params.bridge_name format="angular"/>
    </#if>
    <@renderPositionList geomParam=part lang="en"/>
    is closed for passage that requires opening of the bridge.
</field-template>

<#if promulgate('audio')>
    <field-template field="message.promulgation('audio').text" update="append">
        <@line>
            Broen
            <#if params.bridge_name?has_content>
                <@quote text=params.bridge_name/>
            </#if>
            <@renderPositionList geomParam=part lang="da"  format="audio"/>
            er lukket for gennemsejling der kræver broåbning.
        </@line>
    </field-template>
</#if>

<#if promulgate('navtex')>
    <field-template field="message.promulgation('navtex').text" update="append">
        <@line format="navtex">
            BRIDGE
            <#if params.bridge_name?has_content>
                ${params.bridge_name}
            </#if>
            <@renderPositionList geomParam=part format="navtex" lang="en"/>
            CLOSED FOR PASSAGE THAT REQUIRES OPENING OF BRIDGE.
        </@line>
    </field-template>
</#if>
