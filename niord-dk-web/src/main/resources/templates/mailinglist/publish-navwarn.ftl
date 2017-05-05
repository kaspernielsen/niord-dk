
<#include "publish-navwarn-common.ftl"/>

<div>
    Danish Navigational Warning no. <#if message.shortId?has_content>${message.shortId}</#if>
</div>
<@renderNavWarnDetails msg=message lang='en'></@renderNavWarnDetails>
<p>&nbsp;</p>
<div>
    Dansk navigationsadvarsel nr. <#if message.shortId?has_content>${message.shortId}</#if>
</div>
<@renderNavWarnDetails msg=message lang='da'></@renderNavWarnDetails>
