{
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
						{
							"textParagraph": {
								"text": "<b>Reason:</b> ${reason!}"
							}
						},
						{
							"textParagraph": {
								"text": "<b>Tests:</b> ${tests!}"
							}
						},
<#if changes??>
						{
							"keyValue": {
								"topLabel": "Changes",
								"content": "${changes}"
							}
						},
</#if>
						{
							"textParagraph": {
								"text": "<b>Build duration:</b> ${buildDuration}"
							}
						},
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
