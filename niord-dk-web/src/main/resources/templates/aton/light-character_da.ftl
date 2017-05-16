
<#macro formatLightCharacterPhase phase multiple=false>
    <#switch phase>
        <#case "F">fast lys<#break>
        <#case "Fl">blink<#break>
        <#case "FFl">fast lys med blink<#break>
        <#case "LFl">${multiple?then('lange', 'langt')} blink<#break>
        <#case "Q">hurtig-blink<#break>
        <#case "VQ">meget ${multiple?then('hurtige blink', 'hurtig-blink')}<#break>
        <#case "IQ">${multiple?then('afbrudte', 'afbrudt')} hurtig-blink<#break>
        <#case "IVQ">${multiple?then('afbrudte meget hurtige blink', 'afbrudt meget hurtigt blink')}<#break>
        <#case "UQ">${multiple?then('ultra-hurtige blink', 'ultra-hurtigt blink')}<#break>
        <#case "IUQ">${multiple?then('afbrudte ultra-hurtige blink', 'afbrudt ultra-hurtigt blink')}<#break>
        <#case "Iso">isofase lys<#break>
        <#case "Oc">formørkelser<#break>
        <#case "Al">vekslende<#break>
        <#case "Mo">morsekode<#break>
    </#switch>
</#macro>

<#macro formatLightCharacterColor col multiple=false>
    <#switch col>
        <#case "W">${multiple?then('hvide', 'hvidt')}<#break>
        <#case "G">${multiple?then('grønne', 'grønt')}<#break>
        <#case "R">${multiple?then('røde', 'rødt')}<#break>
        <#case "Y">${multiple?then('gule', 'gult')}<#break>
        <#case "B">${multiple?then('blå', 'blåt')}<#break>
        <#case "Am">${multiple?then('ravgule', 'ravgult')}<#break>
    </#switch>
</#macro>


<#macro formatlightGroup lightGroup>
    <#assign multiple=lightGroup.grouped />

    <#if lightGroup.phase == 'Mo'>
        morsekode ${lightGroup.morseCode}

    <#elseif lightGroup.phase == 'Al'>
        vekslende
        <#if lightGroup.groupSpec?has_content>
            <#list lightGroup.groupSpec as blinks>
                ${blinks} <#if blinks_has_next> + </#if>
            </#list>
        </#if>

        <#if lightGroup.colors?has_content>
            <#list lightGroup.colors as col>
                <#if !col?is_first && col?is_last> og <#elseif !col?is_first>, </#if>
                <@formatLightCharacterColor col=col multiple=multiple/>
            </#list>
            blink
        </#if>

    <#elseif lightGroup.phase == 'Oc'>

        <#if lightGroup.colors?has_content>
            <#list lightGroup.colors as col>
                <#if !col?is_first && col?is_last> og <#elseif !col?is_first>, </#if>
                <@formatLightCharacterColor col=col multiple=false/>
            </#list> lys med
        </#if>

        <#if lightGroup.groupSpec?has_content>
            <#list lightGroup.groupSpec as blinks>
                ${blinks} <#if blinks_has_next> + </#if>
            </#list>
        </#if>
        formørkelser

    <#else>
        <#if lightGroup.groupSpec?has_content>
            <#list lightGroup.groupSpec as blinks>
                ${blinks} <#if blinks_has_next> + </#if>
            </#list>
        </#if>

        <#if lightGroup.colors?has_content>
            <#list lightGroup.colors as col>
                <#if !col?is_first && col?is_last> og <#elseif !col?is_first>, </#if>
                <@formatLightCharacterColor col=col multiple=multiple/>
            </#list>
        </#if>

        <@formatLightCharacterPhase phase=lightGroup.phase multiple=multiple />
    </#if>
</#macro>


<#macro formatlightCharacter lightModel>

    <#if lightModel.lightGroups??>
        <#list lightModel.lightGroups as lightGroup>
            <@formatlightGroup lightGroup=lightGroup /><#if lightGroup_has_next> efterfulgt af </#if>
        </#list>

        <#if lightModel.period??>
            hvert ${lightModel.period}. sekund
        </#if>

        <#if lightModel.elevation??>
            , ${lightModel.elevation} meter
        </#if>

        <#if lightModel.range??>
            , ${lightModel.range} sømil
        </#if>

    </#if>

</#macro>

<@formatlightCharacter lightModel=lightModel/>