<#include "common.ftl"/>
<#include "markings.ftl"/>

<@defaultSubjectFieldTemplates/>

<field-template field="part.getDesc('da').details" format="html">
    En ikke kortlagt grund er rapporteret beliggende <@renderPositionList geomParam=part lang="da"/>.
    Grunden er <@renderMarkings markings=params.markings! lang="da" format="details" unmarkedText="ikke afmærket"/><br>
    Det tilrådes skibsfarten at holde godt klar af positionen.
</field-template>

<field-template field="part.getDesc('en').details" format="html">
    An uncharted obstruction is reported <@renderPositionList geomParam=part lang="en"/>.
    The obstruction is <@renderMarkings markings=params.markings! lang="en" format="details" unmarkedText="unmarked"/><br>
    Mariners are advised to keep well clear.
</field-template>

<field-template field="message.promulgation('audio').text" update="append">
    <@line>
        En ikke kortlagt grund er rapporteret beliggende <@renderPositionList geomParam=part format="audio" lang="da"/>.
        Grunden er <@renderMarkings markings=params.markings! lang="da" format="audio"  unmarkedText="ikke afmærket"/>
    </@line>
    <@line>
        Det tilrådes skibsfarten at holde godt klar af positionen.
    </@line>
</field-template>

<field-template field="message.promulgation('navtex').text" update="append">
    <@line format="navtex">
        UNCHARTED OBSTRUCTION REPORTED <@renderPositionList geomParam=part format="navtex" lang="en"/>.
        OBSTRUCTION <@renderMarkings markings=params.markings! lang="en" format="navtex"  unmarkedText="UNMARKED"/>
    </@line>
    <@line format="navtex">
        MARINERS ADVISED TO KEEP CLEAR.
    </@line>
</field-template>
