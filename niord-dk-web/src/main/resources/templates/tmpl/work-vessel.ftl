
<!-- ***************************************  -->
<!-- Renders information about a work vessel  -->
<!-- ***************************************  -->

<#macro renderWorkVessel vessel lang="en" format="details">
    <#setting locale=lang>

    <#if vessel?has_content && (vessel.name?has_content || vessel.callSign?has_content)>

        <#assign csFormat=(format == 'audio')?then('verbose','normal')/>
        <#assign vesselName=(vessel.name?has_content)?then(vessel.name, '???')/>

        <#if lang == "da">

            <#if format == 'navtex'>
                Udføres af <@quote text=vesselName/>
            <#elseif format == 'audio'>
                Arbejdet udføres af <@quote text=vesselName/>
            <#else>
                Arbejdet udføres af <@quote text=vesselName format="angular"/>
            </#if>
            <#if vessel.callSign?has_content>kaldesignal <@callSignFormat callSign=vessel.callSign format=csFormat/></#if>.
            <#if vessel.guardVessels!false>
                Der er afviserfartøjer i området.
            </#if>
            <#if vessel.contact!false>
                <#if vessel.guardVessels!false>Skibene<#else>Skibet</#if>
                    kan kontaktes på VHF kanal 16
                <#if vessel.channel?has_content>og ${vessel.channel}</#if>.
            </#if>
        <#else>

            <#if format == 'navtex'>
                Work by <@quote text=vesselName/>
            <#elseif format == 'audio'>
                Work is carried out by <@quote text=vesselName/>
            <#else>
                Work is carried out by <@quote text=vesselName format="angular"/>
            </#if>
            <#if vessel.callSign?has_content>call-sign <@callSignFormat callSign=vessel.callSign format=csFormat/></#if>.
            <#if vessel.guardVessels!false>
                Guard vessels will be in the area.
            </#if>
            <#if vessel.contact!false>
                The <#if vessel.guardVessels!false>vessels are<#else>vessel is</#if>
                listening on VHF channel 16
                <#if vessel.channel?has_content>and ${vessel.channel}</#if>.
            </#if>
        </#if>
    </#if>

    <#if lang == "da">
        Skibsfarten anmodes om at vise hensyn ved passage
        <#if vessel.minDist?has_content>
            og holde en afstand på min. ${vessel.minDist} ${(vessel.minDistType == 'm')?then('m', 'sømil')}
        </#if>.
    <#elseif format == 'navtex'>
        MARINERS REQUESTED TO PASS WITH CAUTION
        <#if vessel.minDist?has_content>
            KEEPING MINIMUM DISTANCE ${vessel.minDist}${(vessel.minDistType!'m')?upper_case}
        </#if>.
    <#else>
        Mariners are requested to pass with caution
        <#if vessel.minDist?has_content>
            and keep a minimum distance of ${vessel.minDist}${(vessel.minDistType!'m')}
        </#if>.
    </#if>
</#macro>
