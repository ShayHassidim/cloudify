service {

	name "redis"
	type "MESSAGE_BUS"
	icon "http://www.apache.org/images/feather-small.gif"

	lifecycle{
		init "redis_install.groovy"
	    start "redis_start.groovy"
	}
	plugins([
		plugin {
			name "portLiveness"
			className "com.gigaspaces.cloudify.usm.liveness.PortLivenessDetector"
			config ([
						"Port" : [6379],
						"TimeoutInSeconds" : 60,
						"Host" : "127.0.0.1"
					])
		},
		/*plugin {
			name "jmx"
			className "com.gigaspaces.cloudify.usm.jmx.JmxMonitor"
			config([
						"Store Percent Usage": [
							"org.apache.activemq:BrokerName=localhost,Type=Broker",
							"StorePercentUsage"
						],
						port: 11099
					])
		}*/
	])


/*	userInterface {
		metricGroups = ([
			metricGroup {

				name "broker"

				metrics([
					"Store Percent Usage",
				])
			},
		]
		)

		widgetGroups = ([
			widgetGroup {
				name "Store Percent Usage"
				widgets ([
					barLineChart{
						metric "Store Percent Usage"
						axisYUnit Unit.PERCENTAGE
					}
				])
			},
		]
		)
	}*/
}

