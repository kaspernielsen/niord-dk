<#include "common.ftl"/>

<@defaultSubjectFieldTemplates/>

<field-template field="part.getDesc('da').details" format="html">
    <#setting locale="da">
    Der er observeret vanddybder ned til ${params.water_depth!0?c} m
    <@renderPositionList geomParam=part lang="da"/>
    <#if params.locality?has_content>i indsejlingen til ${params.locality}</#if>.<br>
    Det tilrådes skibsfarten at holde godt klar.
</field-template>

<field-template field="part.getDesc('en').details" format="html">
    <#setting locale="en">
    Water depths down to ${params.water_depth!0?c}m
    have been observed
    <@renderPositionList geomParam=part lang="en"/>
    <#if params.locality?has_content>in the entrance to ${params.locality}</#if>.<br>
    Mariners are advised to keep well clear.
</field-template>

<#if promulgate('audio')>
    <field-template field="message.promulgation('audio').text" update="append">
        <#setting locale="da">
        <@line>
            Der er observeret vanddybder ned til ${params.water_depth!0?c} m
            <@renderPositionList geomParam=part format="audio" lang="da"/>
            <#if params.locality?has_content>i indsejlingen til ${params.locality}</#if>.
        </@line>
        <@line>
            Det tilrådes skibsfarten at holde godt klar.
        </@line>
    </field-template>
</#if>

<#if promulgate('navtex')>
    <field-template field="message.promulgation('navtex').text" update="append">
        <#setting locale="en">
        <@line format="navtex">
            Water depths down to ${params.water_depth!0?c}M OBSERVED
            <@renderPositionList geomParam=part format="navtex" lang="en"/>
            <#if params.locality?has_content>IN ENTRANCE TO ${params.locality}</#if>.
        </@line>
        <@line format="navtex">
            MARINERS ADVISED TO KEEP CLEAR
        </@line>
    </field-template>
</#if>
