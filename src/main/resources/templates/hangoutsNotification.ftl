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
								"text": "${reason!}"
							}
						},
						{
							"textParagraph": {
								"text": "${tests!}"
							}
						},
<#if changes??>
						{
							"textParagraph": {
								"text": "${changes}"
							}
						},
</#if>
						{
							"textParagraph": {
								"text": "Build duration: ${buildDuration}"
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
