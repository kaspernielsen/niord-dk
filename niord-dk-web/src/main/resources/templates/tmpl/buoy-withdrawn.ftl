<#include "aton-common.ftl"/>

<#assign durationDa=(params.duration??)?then(getListValue(params.duration, '', 'normal', 'da'), '')/>
<#assign durationEn=(params.duration??)?then(getListValue(params.duration, '', 'normal', 'en'), '')/>

<@aton
    daDefaultName="Dagsømærket"
    daDetails="er ${durationDa} inddraget"
    enDefaultName="The buoy"
    enDetails="has been ${durationEn} withdrawn"
    enNavtex="${durationEn} WITHDRAWN"
    />
