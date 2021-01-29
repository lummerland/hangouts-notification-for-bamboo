{
<#if mentionAllUsers??>
	 "text": "<users/all>: Please have a look at this build result.",
</#if>
	"cards": [
		{
			"sections": [
				{
					"widgets": [
						{
							"textParagraph": {
								"text": "<!--${projectName} &gt; -->${planName} &gt; #${buildNumber}: <b>${buildState}</b>"
							}
						},
<#if reason??>
						{
							"textParagraph": {
								"text": "<b>Reason:</b> ${reason}"
							}
						},
</#if>
<#if tests??>
						{
							"textParagraph": {
								"text": "<b>Tests:</b> ${tests}"
							}
						},
</#if>
<#if changes??>
						{
							"textParagraph": {
								"text": "<b>Changes:</b><br>${changes}"
							}
						},
</#if>
<#if buildDuration??>
						{
							"textParagraph": {
								"text": "<b>Build duration:</b> ${buildDuration}"
							}
						},
</#if>
<#if artifacts??>
						{
							"textParagraph": {
								"text": "<b>Artifacts:</b><br><#list artifacts as link>${link}<br></#list>"
							}
						},
</#if>
						{
							"buttons": [
								{
									"textButton": {
										"text": "&#x21E8; Build",
										"onClick": {
											"openLink": {
												"url": "${baseUrl}/browse/${planKey}-${buildNumber}"
											}
										}
									}
								},
								{
									"textButton": {
										"text": "&#x21E8; Plan",
										"onClick": {
											"openLink": {
												"url": "${baseUrl}/browse/${planKey}"
											}
										}
									}
								}
							]
						}
					]
				}
			]
		}
	]
}
