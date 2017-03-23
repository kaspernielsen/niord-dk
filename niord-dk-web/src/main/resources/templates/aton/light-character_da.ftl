
<#macro formatLightCharacterPhase phase>
    <#switch phase>
        <#case "F">fast lys<#break>
        <#case "Fl">blink<#break>
        <#case "FFl">fast lys med blink<#break>
        <#case "LFl">lange blink<#break>
        <#case "Q">hurtigblink<#break>
        <#case "VQ">meget hurtige blink<#break>
        <#case "IQ">afbrudte hurtigblink<#break>
        <#case "IVQ">afbrudte meget hurtige blink<#break>
        <#case "UQ">ultra-hurtige blink<#break>
        <#case "IUQ">afbrudte ultra-hurtige blink<#break>
        <#case "Iso">isofase blink<#break>
        <#case "Oc">formørkelser<#break>
        <#case "Alt">alternerende lys<#break>
        <#case "Mo">blink i morsekode<#break>
    </#switch>
</#macro>

<#macro formatLightCharacterColor col>
    <#switch col>
        <#case "W">hvid<#break>
        <#case "G">grøn<#break>
        <#case "R">rød<#break>
        <#case "Y">gul<#break>
        <#case "B">blå<#break>
        <#case "Am">ravgul<#break>
    </#switch>
</#macro>


<#macro formatlightGroup lightGroup>
    <#if lightGroup.composite!false>
        sammensatte grupper af
    <#elseif lightGroup.grouped!false>
        grupper af
    </#if>

    <#if lightGroup.groupSpec?has_content>
        <#list lightGroup.groupSpec as blinks>
            ${blinks} <#if blinks_has_next> + </#if>
        </#list>
    </#if>

    <@formatLightCharacterPhase phase=lightGroup.phase />

    <#if lightGroup.colors?has_content>
        i
        <#list lightGroup.colors as col>
            <@formatLightCharacterColor col=col /><#if col_has_next>, </#if>
        </#list>
    </#if>

    <#if lightGroup.phase == "Mo">
        ${lightGroup.morseCode}
    </#if>

</#macro>

<#macro formatlightCharacter lightModel>

    <#if lightModel.lightGroups??>
        <#list lightModel.lightGroups as lightGroup>
            <@formatlightGroup lightGroup=lightGroup /><#if lightGroup_has_next> efterfulgt af </#if>
        </#list>

        <#if lightModel.period??>
            , som gentages hver ${lightModel.period}. sekund
        </#if>

        <#if lightModel.elevation??>
            , lyset er ${lightModel.elevation} meter over kort-datum
        </#if>

        <#if lightModel.range??>
            og er synlig over ${lightModel.range} nautiske mil
        </#if>

    </#if>

</#macro>

<@formatlightCharacter lightModel=lightModel/>