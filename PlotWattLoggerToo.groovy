/**
 *  PlotWatt Logger 
 *
 *  Copyright 2015 Brian Wilson
 *  Extended by Barry Burke to send data max once per minute, saving up values for bulk updates when necessary (ie. HEM reports total
 *  power multiple times per minute). This is necessary to avoid PlotWatt API throttling.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 * 
 *  Please go here for help and setup instructions: https://github.com/bdwilson/PlotWatt-SmartThings-Logger 
 *
 */
definition(
    name: "PlottWatt Logger Too",
    namespace: "sandood",
    author: "Brian Wilson & Barry Burke",
    description: "PlotWatter Logger Too",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Log devices...") {
        input "power", "capability.powerMeter", title: "Power", required: false, multiple: true
    }

    section ("PlotWatt API ID...") {
        input "channelId", "text", title: "PlotWatt API ID"
    }

    section ("PlotWatt panel id...") {
        input "channelKey", "text", title: "Panel id"
    }
}

def installed() {
    state.count = null
    state.reports = ""
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(power, "power", handlePowerEvent)
    state.lastReport = 0 as Integer
}

def handlePowerEvent(evt) {
    logField(evt,"power") { it.toString() }
}


private logField(evt, field, Closure c) {
	def MIN_ELAPSED = 60 as Integer
    def MAX_REPORTS = 45 as Integer
    
    def value = c(evt.value)
    float watts = value.toFloat()
    def kwatts = watts/1000
    def now = Calendar.instance
    def date = now.time
    def millis = date.time
    def secs = millis/1000
    secs = Math.round(secs)
    
    if ( state.lastReport > secs) {
    	state.lastReport = 0 as Integer
    }
    Integer elapsed = secs - state.lastReport
    
    if (state.count) {
    	state.count = state.count +1
        state.reports = "${channelKey},${kwatts},${secs}," + state.reports	// newest first
    } else {
        state.count = 1
        state.reports = "${channelKey},${kwatts},${secs}"
    }

//	log.debug "${state.reports}"
    

	if ((elapsed < MIN_ELAPSED) && (state.count < MAX_REPORTS)) { return }
    
    def body = state.reports

 	state.count = null
    state.reports = null
    state.lastReport = secs as Integer
    
	def uri = "http://${channelId}:@plotwatt.com/api/v2/push_readings"
    def params = [
        uri: uri,
        body: body
    ] 
    log.debug "Posting Body: ${body} to ${uri}"

    httpPost(params) {response -> parseHttpResponse(response)}
}

def parseHttpResponse(response) {
	log.debug "Request was successful, $response.status"
}
